package com.example.demo

import com.example.demo.gql.types.TextMessage
import com.example.demo.gql.types.TextMessageInput
import com.netflix.graphql.dgs.*
import org.reactivestreams.Publisher
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import java.time.LocalDateTime
import java.util.*

@DgsComponent
class MessagesDataFetcher {

    @DgsQuery
    fun messages() = STORE.values

    @DgsMutation
    fun send(@InputArgument("message") message: TextMessageInput): TextMessage {
        val msg = TextMessage(
            id = UUID.randomUUID().toString(),
            body = message.body,
            sentAt = LocalDateTime.now()
        )
        STORE[msg.id] = msg
        sink.emitNext(msg, Sinks.EmitFailureHandler.FAIL_FAST)
        return msg;
    }

    @DgsSubscription
    fun messageSent(): Publisher<TextMessage> = sink.asFlux()

    private val sink: Many<TextMessage> = Sinks.many().replay().limit(100)

    companion object {
        val STORE = mutableMapOf<String, TextMessage>()
    }
}