package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.client.WebSocketGraphQLClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTestsWithGraphQLClient {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    lateinit var socketGraphQLClient: WebSocketGraphQLClient

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setup() {
        this.socketGraphQLClient = WebSocketGraphQLClient("ws://localhost:$port/subscriptions", ReactorNettyWebSocketClient())
    }

    @Test
    fun testMessages() {
        //Hooks.onOperatorDebug();
        val query = "subscription { messageSent { body } }"
        val variables = emptyMap<String, Any>()
        val executionResult = socketGraphQLClient.reactiveExecuteQuery(query, variables)
            .map {
                it.extractValueAsObject(
                    "data.messageSent",
                    object : TypeRef<Map<String, Any>>() {}
                )["body"] as String
            }
        val verifier = StepVerifier.create(executionResult)
            .consumeNextWith { assertThat(it).isEqualTo("text1 message") }
 //           .consumeNextWith { assertThat(it).isEqualTo("text2 message") }
            .thenCancel()
            .verifyLater()

        val sendText1 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "text1 message")))
        )
        assertThat(sendText1).contains("text1");

//        val sendText2 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
//            "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}",
//            "data.send.body",
//            mapOf("msg" to (mapOf("body" to "text2 message")))
//        )
//        assertThat(sendText2).contains("text2");

        //verify it now.
        verifier.verify();

        val msgs = dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(
            " { messages { body }}",
            "data.messages[*].body"
        )
        assertThat(msgs).allMatch { s: String ->
            s.contains(
                "message"
            )
        }
    }
}
