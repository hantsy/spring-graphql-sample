package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.client.WebSocketGraphQlClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {
    companion object {
        private val log = LoggerFactory.getLogger(IntegrationTests::class.java)
    }
    lateinit var webClient: WebClientGraphQLClient
    lateinit var socketClient: WebSocketGraphQlClient

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setup() {
        val baseUrl = "http://localhost:$port/graphql"
        this.webClient = WebClientGraphQLClient(WebClient.create(baseUrl))

        val subscriptionUrl = "ws://localhost:$port/graphql"
        this.socketClient = WebSocketGraphQlClient.create(URI.create(subscriptionUrl), ReactorNettyWebSocketClient())
        this.socketClient.start().subscribe()
    //        val latch = CountDownLatch(1)
//        this.socketClient.start().doOnTerminate { latch.countDown() }.subscribe()
//        latch.await(500, TimeUnit.MILLISECONDS)
    }

    @AfterEach
    fun teardown() {
        this.socketClient.stop().subscribe()
//        val latch = CountDownLatch(1)
//        this.socketClient.stop().doOnTerminate { latch.countDown() }.subscribe()
//        latch.await(500, TimeUnit.MILLISECONDS)
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

        val executionResult = socketClient.document(messageSentSubscriptionQuery).executeSubscription()
            .map {
                val messageSent = it.getData<Map<String, Any>>()!!["messageSent"]!! as Map<String, Any>
                messageSent["body"] as String
            }

        val message1 = "text1"
        val message2 = "text2"
        val verifier = StepVerifier.create(executionResult)
            .thenAwait(Duration.ofMillis(1500)) // see: https://github.com/Netflix/dgs-framework/issues/657
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

        @Language("graphql") val allMessagesQuery = """
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
