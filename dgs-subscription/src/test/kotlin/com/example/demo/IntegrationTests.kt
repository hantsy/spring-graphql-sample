package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.DgsWebClientGraphQLClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.client.WebSocketGraphQlClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    // DgsWebClientGraphQLClient is the Jackson 3 variant of the Jackson 2 based WebClientGraphQLClient,
    // and the subscription runs over Spring GraphQL's own WebSocketGraphQlClient.
    lateinit var webClient: DgsWebClientGraphQLClient
    lateinit var socketClient: WebSocketGraphQlClient

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setup() {
        val baseUrl = "http://localhost:$port/graphql"
        this.webClient = DgsWebClientGraphQLClient(WebClient.create(baseUrl))

        val subscriptionUrl = "ws://localhost:$port/graphql"
        this.socketClient = WebSocketGraphQlClient.create(URI.create(subscriptionUrl), ReactorNettyWebSocketClient())
        this.socketClient.start().subscribe()
    }

    @AfterEach
    fun teardown() {
        this.socketClient.stop().subscribe()
    }

    @Test
    fun testMessages() {
        val messageSentSubscriptionQuery = """
            subscription {
                messageSent {
                    body
                }
            }
            """.trimIndent()

        val executionResult = socketClient.document(messageSentSubscriptionQuery).executeSubscription()
            .map {
                val messageSent = it.getData<Map<String, Any>>()!!["messageSent"]!! as Map<String, Any>
                messageSent["body"] as String
            }
            // the sink replays the last 100 messages to every new subscriber, so the stream
            // is filtered down to the messages this test sent itself
            .filter { it.startsWith("ws-") }

        val message1 = "ws-text1 message"
        val message2 = "ws-text2 message"
        val verifier = StepVerifier.create(executionResult)
            .thenAwait(Duration.ofMillis(1500)) // see: https://github.com/Netflix/dgs-framework/issues/657
            .consumeNextWith { assertThat(it).isEqualTo(message1) }
            .consumeNextWith { assertThat(it).isEqualTo(message2) }
            .thenCancel()
            .verifyLater()

        // give the websocket subscription time to register on the server before sending,
        // otherwise the first message may be emitted before the subscription is active
        Thread.sleep(1000)

        val sendMessageQuery = """
            mutation sendMessage(${'$'}msg: TextMessageInput!) {
                 send(message:${'$'}msg) {
                    body
                 }
             }
             """.trimIndent()
        webClient.reactiveExecuteQuery(sendMessageQuery, mapOf("msg" to (mapOf("body" to message1))))
            .map { it.extractValueAsObject("data.send.body", String::class.java) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it).isEqualTo(message1) }
            .verifyComplete()

        webClient.reactiveExecuteQuery(sendMessageQuery, mapOf("msg" to (mapOf("body" to message2))))
            .map { it.extractValueAsObject("data.send.body", String::class.java) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it).isEqualTo(message2) }
            .verifyComplete()

        //verify it now.
        verifier.verify()

        val allMessagesQuery = """
            query allMessages {
                messages {
                    body
                }
            }
            """.trimIndent()
        webClient.reactiveExecuteQuery(allMessagesQuery)
            .map { it.extractValueAsObject("data.messages[*].body", object : TypeRef<List<String>>() {}) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it).containsAll(listOf(message1, message2)) }
            .verifyComplete()
    }
}
