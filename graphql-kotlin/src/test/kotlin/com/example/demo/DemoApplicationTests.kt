package com.example.demo

import com.expediagroup.graphql.server.spring.subscriptions.SubscriptionOperationMessage
import com.expediagroup.graphql.server.types.GraphQLRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.publisher.TestPublisher
import java.net.URI
import java.time.Duration
import java.util.*
import kotlin.random.Random


@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var webClientBuilder: WebClient.Builder

    @LocalServerPort
    var port: Int = 0

    lateinit var client: WebClient

    @BeforeEach
    fun setup() {
        this.client = webClientBuilder
            .baseUrl("http://localhost:$port/graphql")
            .build()
    }

    @Test
    fun `fetching all posts`() = runTest {
        val body = mapOf(
            "query" to """
				query {
				  allPosts{ 
				  	id
				    title
					content
					comments { content}
					author {email}
				  }
				}
			""".trimIndent(),
            "variables" to emptyMap<String, String>()
        )
        val response = client.post()
            .bodyValue(body)
            .awaitExchange<Map<String, Any>> {
                it.statusCode() shouldBe HttpStatus.OK
                it.awaitBody()
            }

        val data = response["data"]
        data shouldNotBe null
        response["errors"] shouldBe null

        val allPosts = (data as Map<*, *>)["allPosts"] as List<*>
        (allPosts[0] as Map<*, *>)["title"] as String shouldBe "LEARN SPRING"
    }

    @Test
    fun `test comments on post`() = runTest {
        val socketClient = ReactorNettyWebSocketClient()
        val uri: URI = URI.create("ws://localhost:$port/subscriptions")

        // create a post
        val createPostRequest = mapOf(
            "query" to """
            mutation createPost(${'$'}input: CreatePostInput!){
                createPost(input:${'$'}input){ 
                    id  
                    title 
                }
            }
			""".trimIndent(),
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "my title",
                    "content" to "my content of my title"
                )
            )
        )
        val createPostResponse = client.post()
            .bodyValue(createPostRequest)
            .awaitExchange<Map<String, Map<String, Map<String, Any>>>> {
                it.statusCode() shouldBe HttpStatus.OK
                it.awaitBody()
            }

        val createPostData = createPostResponse["data"]
        val postId = UUID.fromString(createPostData!!["createPost"]!!["id"] as String)

        // get post by id
        val postByIdRequest = mapOf(
            "query" to """
			query postById(${'$'}id: UUID!){
                getPostById(postId:${'$'}id){
                    id
                    title
                }
            }
			""".trimIndent(),
            "variables" to mapOf(
                "id" to postId
            )
        )
        val postByIdResponse = client.post()
            .bodyValue(postByIdRequest)
            .awaitExchange<Map<String, Map<String, Map<String, Any>>>> {
                it.statusCode() shouldBe HttpStatus.OK
                it.awaitBody()
            }

        val postByIdData = postByIdResponse["data"]
        (postByIdData!!["getPostById"]!!["title"] as String) shouldBe "MY TITLE"


        // add comments  to post
        val commentQuery =
            "mutation addComment(\$input: CommentInput!) { addComment(input:\$input) { id postId content}}"
        val comment1Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to "comment1"
            )
        )
        val comment2Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to "comment2"
            )
        )

        val addComment1Response = client.post()
            .bodyValue(mapOf("query" to commentQuery, "variables" to comment1Variables))
            .awaitExchange<Map<String, Map<String, Map<String, Any>>>> {
                it.statusCode() shouldBe HttpStatus.OK
                it.awaitBody()
            }

        val addComment1Data = addComment1Response["data"]
        (addComment1Data!!["addComment"]!!["content"] as String) shouldBe "comment1"

        val addComment2Response = client.post()
            .bodyValue(mapOf("query" to commentQuery, "variables" to comment2Variables))
            .awaitExchange<Map<String, Map<String, Map<String, Any>>>> {
                it.statusCode() shouldBe HttpStatus.OK
                it.awaitBody()
            }

        val addComment2Data = addComment2Response["data"]
        (addComment2Data!!["addComment"]!!["content"] as String) shouldBe "comment2"

        // subscribe to event `commentAdded`
        val query = "subscription onCommentAdded { commentAdded { id content } }"
        val output = TestPublisher.create<String>()

       val subscription = socketClient.execute(uri) { session -> executeSubscription(session, query, output) }
            .subscribe()

        StepVerifier.create(output)
            .consumeNextWith { it shouldContain "\"content\":\"comment2\"" }
            .expectComplete()
            .verify()

        subscription.dispose()
    }


    // see: https://github.com/ExpediaGroup/graphql-kotlin/blob/master/examples/server/spring-server/src/test/kotlin/com/expediagroup/graphql/examples/server/spring/subscriptions/SimpleSubscriptionIT.kt
    private fun executeSubscription(
        session: WebSocketSession,
        query: String,
        output: TestPublisher<String>,
        initPayload: Any? = null
    ): Mono<Void> {
        val id = Random.nextInt().toString()
        val initMessage = getInitMessage(id, initPayload)
        val startMessage = getStartMessage(query, id)

        return session.send(Flux.just(session.textMessage(initMessage)))
            .then(session.send(Flux.just(session.textMessage(startMessage))))
            .thenMany(
                session.receive()
                    .map { objectMapper.readValue<SubscriptionOperationMessage>(it.payloadAsText) }
                    .doOnNext {
                        if (it.type == SubscriptionOperationMessage.ServerMessages.GQL_DATA.type) {
                            val data = objectMapper.writeValueAsString(it.payload)
                            output.next(data)
                        } else if (it.type == SubscriptionOperationMessage.ServerMessages.GQL_COMPLETE.type) {
                            output.complete()
                        }
                    }
            )
            .then()
    }

    private fun SubscriptionOperationMessage.toJson() = objectMapper.writeValueAsString(this)
    private fun getInitMessage(id: String, payload: Any?) = SubscriptionOperationMessage(
        SubscriptionOperationMessage.ClientMessages.GQL_CONNECTION_INIT.type,
        id = id,
        payload = payload
    ).toJson()

    private fun getStartMessage(query: String, id: String): String {
        val request = GraphQLRequest(query)
        return SubscriptionOperationMessage(
            SubscriptionOperationMessage.ClientMessages.GQL_START.type,
            id = id,
            payload = request
        ).toJson()
    }

}
