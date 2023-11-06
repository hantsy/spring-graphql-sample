package com.example.demo

import com.expediagroup.graphql.server.spring.subscriptions.ApolloSubscriptionOperationMessage
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
import org.slf4j.LoggerFactory
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
import java.util.*
import kotlin.random.Random


@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

    companion object {
        private val log = LoggerFactory.getLogger(DemoApplicationTests::class.java)
    }


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
        val postId = createPost()

        // get post by id
        getPostById(postId)

        // subscribe to event `commentAdded`
        val query = """
            subscription onCommentAdded {
                commentAdded {
                    id
                    content
                }
            }
        """.trimIndent()
        val output = TestPublisher.create<String>()
        val subscription = socketClient.execute(uri) { session -> executeSubscription(session, query, output) }
            .subscribe()

        // add comments  to post
        addComment(postId, "comment1")
        addComment(postId, "comment2")

        StepVerifier.create(output)
            .consumeNextWith { it shouldContain "\"content\":\"comment1\"" }
            .consumeNextWith { it shouldContain "\"content\":\"comment2\"" }
            .expectComplete()
            .verify()

        subscription.dispose()
    }

    private suspend fun getPostById(postId: UUID) {
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
    }

    private suspend fun createPost(): UUID {
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
        return UUID.fromString(createPostData!!["createPost"]!!["id"] as String)
    }

    private suspend fun addComment(postId: UUID, comment: String) {
        val commentQuery =
            "mutation addComment(\$input: CommentInput!) { addComment(input:\$input) { id postId content}}"

        val comment1Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to comment
            )
        )

        val response = client.post()
            .bodyValue(mapOf("query" to commentQuery, "variables" to comment1Variables))
            .awaitExchange<Map<String, Map<String, Map<String, Any>>>> {
                it.statusCode() shouldBe HttpStatus.OK
                it.awaitBody()
            }

        val data = response["data"]
        (data!!["addComment"]!!["content"] as String) shouldBe comment
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
                    .map { objectMapper.readValue<ApolloSubscriptionOperationMessage>(it.payloadAsText) }
                    .doOnNext {
                        log.debug("received message:$it")
                        if (it.type == ApolloSubscriptionOperationMessage.ServerMessages.GQL_DATA.type) {
                            val data = objectMapper.writeValueAsString(it.payload)
                            output.next(data)
                        } else if (it.type == ApolloSubscriptionOperationMessage.ServerMessages.GQL_COMPLETE.type) {
                            output.complete()
                        }
                    }
            )
            .then()
    }

    private fun ApolloSubscriptionOperationMessage.toJson() = objectMapper.writeValueAsString(this)
    private fun getInitMessage(id: String, payload: Any?) = ApolloSubscriptionOperationMessage(
        ApolloSubscriptionOperationMessage.ClientMessages.GQL_CONNECTION_INIT.type,
        id = id,
        payload = payload
    ).toJson()

    private fun getStartMessage(query: String, id: String): String {
        val request = GraphQLRequest(query)
        return ApolloSubscriptionOperationMessage(
            ApolloSubscriptionOperationMessage.ClientMessages.GQL_START.type,
            id = id,
            payload = request
        ).toJson()
    }

}
