package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.GraphqlSSESubscriptionGraphQLClient
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    @LocalServerPort
    var port = 0;

    lateinit var sseClient: GraphqlSSESubscriptionGraphQLClient
    lateinit var webClient: WebClientGraphQLClient

    @BeforeEach
    fun setup() {
        val baseUrl = "http://localhost:$port/graphql"
        sseClient = GraphqlSSESubscriptionGraphQLClient(
            baseUrl,
            WebClient.create(baseUrl)
        )
        webClient = WebClientGraphQLClient(WebClient.create(baseUrl))
    }

    @Test
    fun testMessages() {
        //Hooks.onOperatorDebug();
        val subscriptionQuery = "subscription { messageSent { body } }"
        val variables = emptyMap<String, Any>()
        val result = sseClient.reactiveExecuteQuery(subscriptionQuery, variables).map {
            it.extractValueAsObject("data.messageSent", object : TypeRef<Map<String, Any>>() {})["body"] as String
        }

        val verifier = StepVerifier.create(result)
            .consumeNextWith { assertThat(it).isEqualTo("text1 message") }
            .consumeNextWith { assertThat(it).isEqualTo("text2 message") }
            .thenCancel()
            .verifyLater()

        val sendMessageQuery = "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}"
        webClient
            .reactiveExecuteQuery(
                sendMessageQuery,
                mapOf("msg" to (mapOf("body" to "text1 message")))
            )
            .`as` { StepVerifier.create(it) }
            .consumeNextWith {
                assertThat(it.extractValueAsObject("data.send.body", String::class.java))
                    .isEqualTo("text1 message")
            }
            .verifyComplete()

        webClient
            .reactiveExecuteQuery(
                sendMessageQuery,
                mapOf("msg" to (mapOf("body" to "text2 message")))
            )
            .`as` { StepVerifier.create(it) }
            .consumeNextWith {
                assertThat(it.extractValueAsObject("data.send.body", String::class.java))
                    .isEqualTo("text2 message")
            }
            .verifyComplete()

        //verify it now.
        verifier.verify();

        val allMessagesQuery = " { messages { body }}"

        webClient
            .reactiveExecuteQuery(
                allMessagesQuery,
                emptyMap<String, Any>()
            )
            .`as` { StepVerifier.create(it) }
            .consumeNextWith {
                assertThat(it.extractValueAsObject("data.messages[*].body", object : TypeRef<List<String>>() {}))
                    .allMatch { s: String ->
                        s.contains("message")
                    }
            }
            .verifyComplete()
    }
}
