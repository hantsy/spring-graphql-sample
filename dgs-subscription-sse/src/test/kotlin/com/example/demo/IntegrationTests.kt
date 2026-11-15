package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.DgsGraphqlSSESubscriptionGraphQLClient
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    @LocalServerPort
    var port = 0;

    // The Dgs-prefixed clients are the Jackson 3 variants; GraphqlSSESubscriptionGraphQLClient and
    // RestClientGraphQLClient are their Jackson 2 predecessors.
    lateinit var sseClient: DgsGraphqlSSESubscriptionGraphQLClient
    lateinit var restClient: DgsRestClientGraphQLClient

    @BeforeEach
    fun setup() {
        val baseUrl = "http://localhost:$port/graphql"
        sseClient = DgsGraphqlSSESubscriptionGraphQLClient(
            baseUrl,
            WebClient.create(baseUrl)
        )
        restClient = DgsRestClientGraphQLClient(RestClient.builder().baseUrl(baseUrl).build())
    }

    @Test
    fun testMessages() {
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

        val sendText1 = restClient.executeQuery(
            sendMessageQuery,
            mapOf("msg" to (mapOf("body" to "text1 message")))
        ).extractValueAsObject("data.send.body", String::class.java)
        assertThat(sendText1).isEqualTo("text1 message")

        val sendText2 = restClient.executeQuery(
            sendMessageQuery,
            mapOf("msg" to (mapOf("body" to "text2 message")))
        ).extractValueAsObject("data.send.body", String::class.java)
        assertThat(sendText2).isEqualTo("text2 message")

        //verify it now.
        verifier.verify();

        val allMessagesQuery = " { messages { body }}"

        val allMessages = restClient.executeQuery(allMessagesQuery, emptyMap<String, Any>())
            .extractValueAsObject("data.messages[*].body", object : TypeRef<List<String>>() {})
        assertThat(allMessages).allMatch { s: String -> s.contains("message") }
    }
}
