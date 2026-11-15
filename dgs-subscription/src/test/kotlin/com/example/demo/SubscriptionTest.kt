package com.example.demo

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@SpringBootTest
@EnableDgsTest
class SubscriptionTest {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun testMessages() {
        val executionResult = dgsQueryExecutor.execute("subscription { messageSent { body } }")
        val publisher = executionResult.getData<Publisher<ExecutionResult>>()

        // the sink replays the last 100 messages to every new subscriber, so the stream
        // is filtered down to the messages this test sent itself
        val verifier = StepVerifier.create(Flux.from(publisher).filter { body(it).startsWith("sub-") })
            .consumeNextWith { assertThat(body(it)).isEqualTo("sub-text1 message") }
            .consumeNextWith { assertThat(body(it)).isEqualTo("sub-text2 message") }
            .thenCancel()
            .verifyLater()

        val sendText1 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "sub-text1 message")))
        )
        assertThat(sendText1).contains("sub-text1")

        val sendText2 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "sub-text2 message")))
        )
        assertThat(sendText2).contains("sub-text2")

        //verify it now.
        verifier.verify();

        val msgs = dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(
            " { messages { body }}",
            "data.messages[*].body"
        )
        // the in-memory store is shared with the other tests in this JVM, so only assert
        // that the messages sent by this test are persisted
        assertThat(msgs).contains("sub-text1 message", "sub-text2 message")
    }

    private fun body(result: ExecutionResult): String =
        result.getData<Map<String, Map<String, Any>>>()["messageSent"]
            ?.get("body") as String
}
