package com.example.demo

import com.netflix.graphql.dgs.DgsQueryExecutor
import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun testMessages() {
        //Hooks.onOperatorDebug();
        val executionResult = dgsQueryExecutor.execute("subscription { messageSent { body } }")
        val publisher = executionResult.getData<Publisher<ExecutionResult>>()

        val verifier = StepVerifier.create(publisher)
            .consumeNextWith {
                assertThat(
                    (it.getData<Map<String, Map<String, Any>>>()["messageSent"]
                        ?.get("body") as String)
                ).contains("text1")
            }
            .consumeNextWith {
                assertThat(
                    (it.getData<Map<String, Map<String, Any>>>()["messageSent"]
                        ?.get("body") as String)
                ).contains("text2")
            }
            .thenCancel()
            .verifyLater()

        val sendText1 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "text1 message")))
        )
        assertThat(sendText1).contains("text1");

        val sendText2 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "text2 message")))
        )
        assertThat(sendText2).contains("text2");

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
