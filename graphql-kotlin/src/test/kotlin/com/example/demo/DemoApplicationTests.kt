package com.example.demo

import com.expediagroup.graphql.server.types.GraphQLRequest
import com.expediagroup.graphql.server.types.SubscriptionMessageConnectionInit
import com.expediagroup.graphql.server.types.SubscriptionMessageSubscribe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier
import reactor.test.publisher.TestPublisher
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.time.Duration
import java.util.*
import kotlin.random.Random


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration::class)
class DemoApplicationTests {

    companion object {
        private val log = LoggerFactory.getLogger(DemoApplicationTests::class.java)
    }

    @Autowired
    lateinit var jsonMapper: JsonMapper

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
        // allPosts() is findAll() with no ORDER BY and other tests insert posts, so match on
        // content rather than position
        allPosts.map { (it as Map<*, *>)["title"] } shouldContain "LEARN SPRING"
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

        val output = TestPublisher.create<Map<*, *>>()
        val connected = Sinks.one<Unit>()
        val subscription = socketClient
            .execute(uri) { session -> executeSubscription(session, query, output, connected) }
            .subscribe()

        try {
            // the server ignores `subscribe` until it has acknowledged the connection
            connected.asMono().block(Duration.ofSeconds(30))

            // attach the verifier before publishing so no event is missed
            val verifier = StepVerifier.create(output)
                // commentAdded is a hot SharedFlow: let the server register the subscription
                // before the mutations below publish to it
                .thenAwait(Duration.ofMillis(1000))
                .consumeNextWith { it["content"] shouldBe "comment1" }
                .consumeNextWith { it["content"] shouldBe "comment2" }
                // the SharedFlow never terminates, so cancel instead of expecting onComplete
                .thenCancel()
                .verifyLater()

            // add comments  to post
            addComment(postId, "comment1")
            addComment(postId, "comment2")

            verifier.verify(Duration.ofSeconds(30))
        } finally {
            subscription.dispose()
        }
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

    // graphql-ws frames: the app sets graphql.subscriptions.protocol=GRAPHQL_WS, so the legacy
    // subscriptions-transport-ws `start`/`data` messages are rejected as invalid
    private fun executeSubscription(
        session: WebSocketSession,
        query: String,
        output: TestPublisher<Map<*, *>>,
        connected: Sinks.One<Unit>
    ): Mono<Void> {
        val id = Random.nextInt().toString()
        val initMessage = jsonMapper.writeValueAsString(SubscriptionMessageConnectionInit())
        val subscribeMessage =
            jsonMapper.writeValueAsString(SubscriptionMessageSubscribe(id, GraphQLRequest(query)))

        val receive = session.receive()
            .map { jsonMapper.readValue<Map<String, Any?>>(it.payloadAsText) }
            .doOnNext { message ->
                log.debug("received message: {}", message)
                when (message["type"]) {
                    "connection_ack" -> connected.tryEmitValue(Unit)
                    "next" -> ((message["payload"] as? Map<*, *>)?.get("data") as? Map<*, *>)
                        ?.get("commentAdded")
                        ?.let { output.next(it as Map<*, *>) }

                    "complete" -> output.complete()
                }
            }
            .then()

        // a session outbound takes a single subscriber, so both frames share one send(); `subscribe`
        // waits on connection_ack, which only the concurrent receive leg can deliver
        val send = session.send(
            Flux.concat(
                Flux.just(session.textMessage(initMessage)),
                connected.asMono().thenMany(Flux.just(session.textMessage(subscribeMessage)))
            )
        )

        return send.and(receive)
    }

}
