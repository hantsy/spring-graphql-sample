package com.example.demo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Transport level behaviour of the GraphQL endpoint, exercised with the plain reactive
 * `WebClient` and the plain reactive WebSocket client - no GraphQL client library in between.
 *
 * See `IntegrationTests` for the same behaviour seen through the GraphQL client libraries
 * and `RestClient_IntegrationTests` for the blocking counterpart.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebClient_IntegrationTests {

    companion object {
        /**
         * The path prefix of the GraphQL endpoint. The HTTP, the Server-Sent-Events and the
         * WebSocket endpoint are all served under it.
         */
        private const val GRAPHQL_PATH = "/graphql"

        private const val MESSAGE_SENT_SUBSCRIPTION = """
            subscription {
                messageSent {
                    body
                }
            }
        """

        private const val SEND_MESSAGE_MUTATION = """
            mutation sendMessage(${'$'}msg: TextMessageInput!) {
                send(message:${'$'}msg) {
                    body
                }
            }
        """

        private const val ALL_MESSAGES_QUERY = """
            query allMessages {
                messages {
                    body
                }
            }
        """
    }

    @LocalServerPort
    var port: Int = 0

    private lateinit var webClient: WebClient

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    @BeforeEach
    fun setup() {
        webClient = WebClient.create("http://localhost:$port")
    }

    @Test
    fun `send a message over HTTP and query it back`() {
        val body = "web client http message"

        val sent = post(SEND_MESSAGE_MUTATION, mapOf("msg" to mapOf("body" to body)))
        assertThat(sent["errors"]).isNull()

        val allMessages = post(ALL_MESSAGES_QUERY, emptyMap<String, Any>())
        assertThat(allMessages["errors"]).isNull()

        val messages = allMessages["data"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val bodies = messages["messages"] as List<Map<String, Any>>
        assertThat(bodies.map { it["body"] }).contains(body)
    }

    /**
     * The subscription is delivered over the Server-Sent-Events transport, the stream of the
     * response body is decoded into `ServerSentEvent` instances.
     */
    @Test
    fun `subscribe to the messages over SSE with the raw WebClient`() {
        val body = "web client sse message"

        // the sink replays the published messages to a new subscriber, so the message is sent
        // before the subscription is opened.
        send(body)

        val received = webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "query" to MESSAGE_SENT_SUBSCRIPTION,
                    "variables" to emptyMap<String, Any>()
                )
            )
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .mapNotNull { it.data() }
            .filter { it.contains(body) }
            .next()
            .block(Duration.ofSeconds(30))

        assertThat(received).isNotNull()
        assertThat(received).contains(body)
    }

    /**
     * The subscription is delivered over the WebSocket transport, the `graphql-transport-ws`
     * protocol is spoken by the plain reactive `spring-webflux` client.
     *
     * see: https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
     */
    @Test
    fun `subscribe to the messages over websocket with the raw reactive WebSocket client`() {
        val body = "web client websocket message"

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
                val payload = json["payload"] as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val data = payload["data"] as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val messageSent = data["messageSent"] as Map<String, Any>
                contents.add(messageSent["body"] as String)
            }
        }

        ReactorNettyWebSocketClient()
            .execute(URI("ws://localhost:$port$GRAPHQL_PATH"), socketHandler)
            .subscribe({}, {}, {})

        try {
            // give the server the time to register the subscription
            Thread.sleep(1000)

            send(body)

            val content = awaitContent(contents, body)
            assertThat(content).isEqualTo(body)
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
                "query" to MESSAGE_SENT_SUBSCRIPTION,
                "variables" to emptyMap<String, Any>()
            )
        )
    )

    private fun send(body: String) {
        val response = post(SEND_MESSAGE_MUTATION, mapOf("msg" to mapOf("body" to body)))
        assertThat(response["errors"]).isNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun post(query: String, variables: Map<String, Any>): Map<String, Any> {
        val response = webClient.post()
            .uri(GRAPHQL_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("query" to query, "variables" to variables))
            .retrieve()
            .bodyToMono(String::class.java)
            .block(Duration.ofSeconds(30))!!

        return jsonMapper.readValue(response, Map::class.java) as Map<String, Any>
    }

    private fun awaitContent(contents: List<String>, expected: String, timeoutMs: Long = 30_000): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            contents.firstOrNull { it == expected }?.let { return it }
            Thread.sleep(100)
        }
        error("No message '$expected' received within $timeoutMs ms, got: $contents")
    }
}
