package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.netflix.graphql.dgs.client.WebSocketGraphQLClient
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.test.StepVerifier
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTestsWithGraphQLClient {
    lateinit var webClientGraphQLClient: WebClientGraphQLClient
    lateinit var socketGraphQLClient: WebSocketGraphQLClient

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setup() {
        this.webClientGraphQLClient = WebClientGraphQLClient(WebClient.create("http://localhost:$port/graphql"))
        this.socketGraphQLClient =
            WebSocketGraphQLClient("ws://localhost:$port/subscriptions", ReactorNettyWebSocketClient())
    }

    @Test
    fun testMessages() {
        @Language("graphql") val messageSentSubscriptionQuery = """
            subscription { 
                messageSent { 
                    body 
                } 
            }
            """.trimIndent()
        val variables = emptyMap<String, Any>()
        val executionResult = socketGraphQLClient.reactiveExecuteQuery(messageSentSubscriptionQuery, variables)
            .map {
                it.extractValueAsObject(
                    "data.messageSent",
                    object : TypeRef<Map<String, Any>>() {}
                )["body"] as String
            }

        val message1 = "text1"
        val message2 = "text2"
        val verifier = StepVerifier.create(executionResult)
            .thenAwait(Duration.ofMillis(1000)) // see: https://github.com/Netflix/dgs-framework/issues/657
            .consumeNextWith { assertThat(it).isEqualTo(message1) }
            .consumeNextWith { assertThat(it).isEqualTo(message2) }
            .thenCancel()
            .verifyLater()

        @Language("graphql") val sendMessageQuery = """
            mutation sendMessage(${'$'}msg: TextMessageInput!) {
                 send(message:${'$'}msg) { 
                    body
                 }
             }
             """.trimIndent()
        webClientGraphQLClient.reactiveExecuteQuery(sendMessageQuery, mapOf("msg" to (mapOf("body" to message1))))
            .map { it.extractValueAsObject("data.send.body", String::class.java) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it).isEqualTo(message1) }
            .verifyComplete()

        webClientGraphQLClient.reactiveExecuteQuery(sendMessageQuery, mapOf("msg" to (mapOf("body" to message2))))
            .map { it.extractValueAsObject("data.send.body", String::class.java) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it).isEqualTo(message2) }
            .verifyComplete()

        //verify it now.
        verifier.verify()

        @Language("graphql") val allMessagesQuery = """
            query allMessages { 
                messages { 
                    body
                }
            }
            """.trimIndent()
        webClientGraphQLClient.reactiveExecuteQuery(allMessagesQuery)
            .map { it.extractValueAsObject("data.messages[*].body", object : TypeRef<List<String>>() {}) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it).isEqualTo(message1) }
            .consumeNextWith { assertThat(it).isEqualTo(message2) }
            .verifyComplete()
    }
}
