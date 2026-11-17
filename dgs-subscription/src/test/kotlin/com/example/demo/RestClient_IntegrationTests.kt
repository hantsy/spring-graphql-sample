package com.example.demo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
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
 * `RestClient` and the plain blocking WebSocket client - no GraphQL client library
 * in between.
 *
 * See `IntegrationTests` for the same behaviour seen through the GraphQL client libraries
 * and `WebClient_IntegrationTests` for the reactive counterpart.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestClient_IntegrationTests {

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

    private lateinit var restClient: RestClient

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    @BeforeEach
    fun setup() {
        restClient = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `send a message over HTTP and query it back`() {
        val body = "rest client http message"

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
     * The subscription is delivered over the Server-Sent-Events transport. The response of the
     * SSE endpoint stays open, so the events are read from the raw input stream of the
     * response body.
     */
    @Test
    fun `subscribe to the messages over SSE with the raw RestClient`() {
        val body = "rest client sse message"

        // the sink replays the published messages to a new subscriber, so the message is sent
        // before the subscription is opened - the response of the SSE endpoint is flushed when
        // the first event is written.
        send(body)

        val events = LinkedBlockingQueue<String>()
        val failure = AtomicReference<Throwable>()

        // the SSE response never ends, so the stream is consumed in the background
        val subscriber = thread(start = true, isDaemon = true) {
            try {
                restClient.post()
                    .uri(GRAPHQL_PATH)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        mapOf(
                            "query" to MESSAGE_SENT_SUBSCRIPTION,
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
            // the sink replays the published messages, so the stream starts with the
            // messages of the earlier tests - the expected one is picked from the stream.
            val event = awaitEvent(events, body)
            failure.get()?.let { throw AssertionError("The SSE request failed", it) }
            assertThat(event).contains(body)
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
    fun `subscribe to the messages over websocket with the raw WebSocket client`() {
        val body = "rest client websocket message"

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
                val payload = json["payload"] as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val data = payload["data"] as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val messageSent = data["messageSent"] as Map<String, Any>
                contents.add(messageSent["body"] as String)
            }
        }

        // the sub protocol has to be requested, otherwise the server rejects the handshake
        val headers = WebSocketHttpHeaders().apply {
            secWebSocketProtocol = listOf("graphql-transport-ws")
        }

        val clientSession = StandardWebSocketClient()
            .execute(socketHandler, headers, URI("ws://localhost:$port$GRAPHQL_PATH"))
            .get(30, TimeUnit.SECONDS)

        try {
            // give the server the time to register the subscription
            Thread.sleep(1000)

            send(body)

            val content = awaitContent(contents, body)
            assertThat(content).isEqualTo(body)
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
        val response = restClient.post()
            .uri(GRAPHQL_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(mapOf("query" to query, "variables" to variables))
            .retrieve()
            .body(String::class.java)!!

        return jsonMapper.readValue(response, Map::class.java) as Map<String, Any>
    }

    private fun awaitEvent(events: LinkedBlockingQueue<String>, expected: String, timeoutMs: Long = 30_000): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val event = events.poll(100, TimeUnit.MILLISECONDS)
            if (event != null && event.contains(expected)) {
                return event
            }
        }
        error("No event containing '$expected' received within $timeoutMs ms")
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
