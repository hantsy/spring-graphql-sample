package com.example.demo

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.web.client.RestClient
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Transport level behaviour of the GraphQL endpoint, exercised with the plain
 * `RestTestClient` and `RestClient` - no GraphQL client library in between.
 * See `IntegrationTests` for the same behaviour seen through the GraphQL client libraries.
 */
@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestcontainersConfiguration::class)
class RestClient_IntegrationTests {

    companion object {
        private const val SIGN_IN_MUTATION = """
            mutation signIn(${'$'}input: Credentials!){
                signIn(credentials:${'$'}input) {
                    name
                    roles
                    token
                }
            }
        """

        private const val CREATE_POST_MUTATION = """
            mutation createPost(${'$'}input: CreatePostInput!){
                createPost(createPostInput:${'$'}input){
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

        private val CREATE_POST_VARIABLES = mapOf(
            "input" to mapOf(
                "title" to "test title",
                "content" to "test content"
            )
        )

        // The contents are unique per test, the subscription replays the last published comment.
        private const val RAW_SSE_COMMENT = "raw sse comment"
        private const val RAW_WEBSOCKET_COMMENT = "raw websocket comment"
    }

    @LocalServerPort
    var port: Int = 8080

    lateinit var restTestClient: RestTestClient

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    @BeforeEach
    fun setup() {
        restTestClient = RestTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `create a post without auth should return errors PERMISSION_DENIED`() {
        val body = post(CREATE_POST_MUTATION, CREATE_POST_VARIABLES)

        val errors = body["errors"] as List<*>?
        errors shouldNotBe null
        errors!!.size shouldBe 1

        // Over HTTP the request goes through the Spring Security filter chain, which puts an
        // anonymous `Authentication` into the `SecurityContext`. Method security (`@Secured`) then
        // rejects the call with an `AccessDeniedException`, which DGS maps to PERMISSION_DENIED.
        @Suppress("UNCHECKED_CAST")
        val extensions = (errors[0] as Map<String, Any>)["extensions"] as Map<String, Any>
        extensions["errorType"] shouldBe "PERMISSION_DENIED"
    }

    @Test
    fun `sign in and create a post`() {
        val signIn = signIn()
        signIn["name"] shouldBe "user"
        @Suppress("UNCHECKED_CAST")
        (signIn["roles"] as List<String>) shouldContainExactly listOf("ROLE_USER")

        val token = signIn["token"] as String

        // the session id is passed back in the `X-Auth-Token` header.
        val body = post(CREATE_POST_MUTATION, CREATE_POST_VARIABLES, token)

        body["errors"] shouldBe null
        @Suppress("UNCHECKED_CAST")
        val postData = body["data"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val created = postData["createPost"] as Map<String, Any>
        created["id"] shouldNotBe null
        created["title"] shouldBe "test title"
    }

    /**
     * The subscription is delivered over the Server-Sent-Events transport. The response of the
     * SSE endpoint stays open, so the events are read from the raw input stream of the response
     * body.
     *
     * The `RestTestClient` waits for the complete response, therefore the plain `RestClient` is
     * used here, its exchange function runs while the response is still open.
     */
    @Test
    fun `subscribe to the comments over SSE with the raw RestClient`() {
        val token = signIn()["token"] as String
        val postId = createPost(token)

        val events = LinkedBlockingQueue<String>()
        val failure = AtomicReference<Throwable>()

        // the service replays the last published comment to a new subscriber, so the comment is
        // added before the subscription is opened - the response of the SSE endpoint is flushed
        // when the first event is written.
        addComment(postId, RAW_SSE_COMMENT, token)

        val restClient = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()

        // the SSE response never ends, so the stream is consumed in the background
        val subscriber = thread(start = true, isDaemon = true) {
            try {
                restClient.post()
                    .uri("/graphql")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        mapOf(
                            "query" to COMMENT_ADDED_SUBSCRIPTION,
                            "variables" to emptyMap<String, Any>()
                        )
                    )
                    .exchange { _, response ->
                        // a server sent event is a `data:` prefixed line
                        response.body.bufferedReader().forEachLine { line ->
                            if (line.startsWith("data:")) {
                                events.put(line.removePrefix("data:").trim())
                            }
                        }
                    }
            } catch (ex: Throwable) {
                failure.set(ex)
            }
        }

        try {
            val event = events.poll(30, TimeUnit.SECONDS)
            failure.get()?.let { throw AssertionError("The SSE request failed", it) }
            event shouldNotBe null
            event!! shouldContain RAW_SSE_COMMENT
        } finally {
            subscriber.interrupt()
        }
    }

    /**
     * The subscription is delivered over the WebSocket transport, the `graphql-transport-ws`
     * protocol is spoken by the plain blocking `spring-websocket` client.
     *
     * see: https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
     */
    @Test
    fun `subscribe to the comments over websocket with the raw WebSocket client`() {
        val token = signIn()["token"] as String
        val postId = createPost(token)

        val contents = CopyOnWriteArrayList<String>()

        val socketHandler = object : TextWebSocketHandler() {
            override fun afterConnectionEstablished(session: WebSocketSession) {
                // the graphql-transport-ws protocol starts with a `connection_init` message,
                // the `subscribe` message follows directly
                session.sendMessage(TextMessage(connectionInitMessage()))
                session.sendMessage(TextMessage(subscribeMessage()))
            }

            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                @Suppress("UNCHECKED_CAST")
                val json = jsonMapper.readValue(message.payload, Map::class.java) as Map<String, Any>

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

        // the sub protocol has to be requested, otherwise the server rejects the handshake
        val headers = WebSocketHttpHeaders().apply {
            secWebSocketProtocol = listOf("graphql-transport-ws")
        }

        val clientSession = StandardWebSocketClient()
            .execute(socketHandler, headers, URI("ws://localhost:$port/subscriptions"))
            .get(30, TimeUnit.SECONDS)

        try {
            // give the server the time to register the subscription
            Thread.sleep(1000)

            addComment(postId, RAW_WEBSOCKET_COMMENT, token)

            val content = awaitContent(contents, RAW_WEBSOCKET_COMMENT)
            content shouldBe RAW_WEBSOCKET_COMMENT
        } finally {
            clientSession.close()
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

    private fun signIn(): Map<String, Any> {
        val body = post(
            SIGN_IN_MUTATION,
            mapOf(
                "input" to mapOf(
                    "username" to "user",
                    "password" to "password"
                )
            )
        )

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        return data["signIn"] as Map<String, Any>
    }

    private fun createPost(token: String): String {
        val body = post(CREATE_POST_MUTATION, CREATE_POST_VARIABLES, token)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val created = data["createPost"] as Map<String, Any>

        return created["id"] as String
    }

    private fun addComment(postId: String, content: String, token: String) {
        val body = post(
            ADD_COMMENT_MUTATION,
            mapOf(
                "input" to mapOf(
                    "postId" to postId,
                    "content" to content
                )
            ),
            token
        )

        body["errors"] shouldBe null
    }

    private fun post(query: String, variables: Map<String, Any>, token: String? = null): HashMap<String, Any> {
        val spec = restTestClient.post()
            .uri("graphql")
            .accept(MediaType.APPLICATION_JSON)

        val body = mapOf("query" to query, "variables" to variables)

        val response = if (token == null) {
            spec.body(body).exchange()
        } else {
            spec.header("X-Auth-Token", token).body(body).exchange()
        }

        return response
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, Any>>() {})
            .returnResult().getResponseBody()!!
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
