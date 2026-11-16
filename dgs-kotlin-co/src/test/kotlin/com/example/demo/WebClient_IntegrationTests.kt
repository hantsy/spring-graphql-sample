package com.example.demo

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Transport level behaviour of the GraphQL endpoint, exercised with the plain
 * `WebTestClient` - no GraphQL client library in between.
 * See `IntegrationTests` for the same behaviour seen through the GraphQL client libraries.
 */
@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestcontainersConfiguration::class)
class WebClient_IntegrationTests {

    companion object {
        private const val CREATE_POST_MUTATION = """
            mutation createPost(${'$'}input: CreatePostInput!){
                createPost(createPostInput:${'$'}input){
                    id
                    title
                }
            }
        """

        private const val POST_BY_ID_QUERY = """
            query postById(${'$'}postId: UUID!) {
                postById(postId: ${'$'}postId) {
                    id
                    title
                }
            }
        """

        private const val ADD_COMMENT_MUTATION = """
            mutation addComment(${'$'}input: CommentInput!){
                addComment(commentInput:${'$'}input){
                    id
                    postId
                    content
                }
            }
        """

        private const val COMMENT_ADDED_SUBSCRIPTION = """
            subscription onCommentAdded {
                commentAdded {
                    id
                    postId
                    content
                }
            }
        """

        // The contents are unique per test, the subscription replays the last published comment.
        private const val RAW_SSE_COMMENT = "raw sse comment"
        private const val RAW_WEBSOCKET_COMMENT = "raw websocket comment"
    }

    @LocalServerPort
    var port: Int = 8080

    lateinit var webTestClient: WebTestClient

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    @BeforeEach
    fun setup() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port/graphql")
            .defaultHeaders { it.setBasicAuth("user", "password") }
            .build()
    }

    @Test
    fun `create a post and query it back`() {
        val postId = createPost()

        // query the created post back by its id - `allPosts` is not ordered, so the new post
        // cannot be addressed by its position in the list.
        val queryBody = webTestClient.post()
            .bodyValue(mapOf("query" to POST_BY_ID_QUERY, "variables" to mapOf("postId" to postId)))
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, Any>>() {})
            .returnResult().responseBody!!

        @Suppress("UNCHECKED_CAST")
        val post = (queryBody["data"] as Map<String, Any>)["postById"] as Map<String, Any>
        post["id"] shouldBe postId
        post["title"] shouldBe "test title"
    }

    /**
     * The subscription is delivered over the Server-Sent-Events transport. The `WebTestClient`
     * decodes the stream of the response body into `ServerSentEvent` instances.
     */
    @Test
    fun `subscribe to the comments over SSE with the raw WebTestClient`() {
        val postId = createPost()

        // the service replays the last published comment to a new subscriber, so the comment is
        // added before the subscription is opened.
        addComment(postId, RAW_SSE_COMMENT)

        val events = webTestClient.post()
            .accept(MediaType.TEXT_EVENT_STREAM)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "query" to COMMENT_ADDED_SUBSCRIPTION,
                    "variables" to emptyMap<String, Any>()
                )
            )
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .returnResult(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .responseBody!!

        val received = CopyOnWriteArrayList<String>()
        val subscription = events.subscribe { received.add(it.data().orEmpty()) }

        try {
            val event = awaitEvent(received, RAW_SSE_COMMENT)
            event shouldContain RAW_SSE_COMMENT
        } finally {
            subscription.dispose()
        }
    }

    /**
     * The subscription is delivered over the WebSocket transport, the `graphql-transport-ws`
     * protocol is spoken by the plain reactive `spring-webflux` client.
     *
     * see: https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
     */
    @Test
    fun `subscribe to the comments over websocket with the raw WebSocket client`() {
        val postId = createPost()

        val contents = CopyOnWriteArrayList<String>()
        val sessionRef = AtomicReference<WebSocketSession>()

        val socketHandler = object : WebSocketHandler {
            // the sub protocol has to be requested, otherwise the server rejects the handshake
            override fun getSubProtocols(): List<String> = listOf("graphql-transport-ws")

            override fun handle(session: WebSocketSession): Mono<Void> {
                sessionRef.set(session)

                // the graphql-transport-ws protocol starts with a `connection_init` message,
                // the `subscribe` message follows directly
                session.send(
                    Mono.just(session.textMessage(connectionInitMessage()))
                        .concatWith(Mono.just(session.textMessage(subscribeMessage())))
                ).subscribe({}, {}, {})

                return session.receive()
                    .map { it.payloadAsText }
                    .doOnNext { onMessage(it) }
                    .then()
            }

            fun onMessage(message: String) {
                @Suppress("UNCHECKED_CAST")
                val json = jsonMapper.readValue(message, Map::class.java) as Map<String, Any>

                // the protocol also sends control frames such as the `connection_ack`,
                // only the `next` frames carry the subscription data
                if (json["type"] != "next") {
                    return
                }

                @Suppress("UNCHECKED_CAST")
                val data = (json["payload"] as Map<String, Any>)["data"] as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val comment = data["commentAdded"] as Map<String, Any>
                contents.add(comment["content"] as String)
            }
        }

        ReactorNettyWebSocketClient()
            .execute(URI("ws://localhost:$port/graphql"), socketHandler)
            .subscribe({}, {}, {})

        try {
            // give the server the time to register the subscription
            Thread.sleep(1000)

            addComment(postId, RAW_WEBSOCKET_COMMENT)

            val content = awaitContent(contents, RAW_WEBSOCKET_COMMENT)
            content shouldBe RAW_WEBSOCKET_COMMENT
        } finally {
            sessionRef.get()?.close()?.subscribe({}, {}, {})
        }
    }

    private fun connectionInitMessage(): String = jsonMapper.writeValueAsString(
        mapOf(
            "type" to "connection_init",
            "payload" to emptyMap<String, Any>()
        )
    )

    private fun subscribeMessage(): String = jsonMapper.writeValueAsString(
        mapOf(
            "id" to "1",
            "type" to "subscribe",
            "payload" to mapOf(
                "query" to COMMENT_ADDED_SUBSCRIPTION,
                "variables" to emptyMap<String, Any>()
            )
        )
    )

    private fun createPost(): String {
        val createBody = webTestClient.post()
            .bodyValue(
                mapOf(
                    "query" to CREATE_POST_MUTATION,
                    "variables" to mapOf(
                        "input" to mapOf(
                            "title" to "test title",
                            "content" to "test content"
                        )
                    )
                )
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, Any>>() {})
            .returnResult().responseBody!!

        @Suppress("UNCHECKED_CAST")
        val created = (createBody["data"] as Map<String, Any>)["createPost"] as Map<String, Any>
        created["title"] shouldBe "test title"

        return created["id"] as String
    }

    private fun addComment(postId: String, content: String) {
        val body = webTestClient.post()
            .bodyValue(
                mapOf(
                    "query" to ADD_COMMENT_MUTATION,
                    "variables" to mapOf(
                        "input" to mapOf(
                            "postId" to postId,
                            "content" to content
                        )
                    )
                )
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, Any>>() {})
            .returnResult().responseBody!!

        body["errors"] shouldBe null
    }

    private fun awaitEvent(events: List<String>, expected: String, timeoutMs: Long = 30_000): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            events.firstOrNull { it.contains(expected) }?.let { return it }
            Thread.sleep(100)
        }
        error("No event containing '$expected' received within $timeoutMs ms, got: $events")
    }

    private fun awaitContent(contents: List<String>, expected: String, timeoutMs: Long = 30_000): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            contents.firstOrNull { it == expected }?.let { return it }
            Thread.sleep(100)
        }
        error("No comment '$expected' received within $timeoutMs ms, got: $contents")
    }
}
