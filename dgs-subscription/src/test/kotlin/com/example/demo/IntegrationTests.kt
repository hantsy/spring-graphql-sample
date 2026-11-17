package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.DgsGraphqlSSESubscriptionGraphQLClient
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.client.WebSocketGraphQlClient
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.net.URI
import java.time.Duration

/**
 * The merged module serves the same subscription over the Server-Sent-Events and over the
 * WebSocket transport, both of them under the same path prefix as the HTTP endpoint.
 *
 * The GraphQL client libraries are used here, see `RestClient_IntegrationTests` and
 * `WebClient_IntegrationTests` for the same behaviour seen at the transport level.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

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

    // The Dgs-prefixed clients are the Jackson 3 variants; GraphqlSSESubscriptionGraphQLClient and
    // RestClientGraphQLClient are their Jackson 2 predecessors.
    private lateinit var sseClient: DgsGraphqlSSESubscriptionGraphQLClient
    private lateinit var restClient: DgsRestClientGraphQLClient
    private lateinit var socketClient: WebSocketGraphQlClient

    @BeforeEach
    fun setup() {
        val baseUrl = "http://localhost:$port$GRAPHQL_PATH"
        sseClient = DgsGraphqlSSESubscriptionGraphQLClient(
            baseUrl,
            WebClient.create(baseUrl)
        )
        restClient = DgsRestClientGraphQLClient(RestClient.builder().baseUrl(baseUrl).build())

        socketClient = WebSocketGraphQlClient.create(
            URI.create("ws://localhost:$port$GRAPHQL_PATH"),
            ReactorNettyWebSocketClient()
        )
        socketClient.start().subscribe()
    }

    @AfterEach
    fun teardown() {
        socketClient.stop().subscribe()
    }

    @Test
    fun `send a message over HTTP and query it back`() {
        val body = "http message"

        val sent = restClient.executeQuery(
            SEND_MESSAGE_MUTATION,
            mapOf("msg" to mapOf("body" to body))
        ).extractValueAsObject("data.send.body", String::class.java)
        assertThat(sent).isEqualTo(body)

        val allMessages = restClient.executeQuery(ALL_MESSAGES_QUERY, emptyMap<String, Any>())
            .extractValueAsObject("data.messages[*].body", object : TypeRef<List<String>>() {})
        assertThat(allMessages).contains(body)
    }

    @Test
    fun `subscribe to the messages over SSE`() {
        val body = "sse message"
        // the sink replays the published messages, so the message is sent before the
        // subscription is opened.
        send(body)

        val received = sseClient.reactiveExecuteQuery(MESSAGE_SENT_SUBSCRIPTION, emptyMap<String, Any>())
            .map {
                it.extractValueAsObject("data.messageSent", object : TypeRef<Map<String, Any>>() {})["body"] as String
            }
            .filter { it == body }
            .next()
            .block(Duration.ofSeconds(30))

        assertThat(received).isEqualTo(body)
    }

    @Test
    fun `subscribe to the messages over WebSocket`() {
        val body = "websocket message"
        send(body)

        val received = socketClient.document(MESSAGE_SENT_SUBSCRIPTION)
            .executeSubscription()
            .map { response ->
                val messageSent = response.getData<Map<String, Any>>()!!["messageSent"] as Map<String, Any>
                messageSent["body"] as String
            }
            .filter { it == body }
            .next()
            .block(Duration.ofSeconds(30))

        assertThat(received).isEqualTo(body)
    }

    private fun send(body: String) {
        val sent = restClient.executeQuery(
            SEND_MESSAGE_MUTATION,
            mapOf("msg" to mapOf("body" to body))
        ).extractValueAsObject("data.send.body", String::class.java)
        assertThat(sent).isEqualTo(body)
    }
}
