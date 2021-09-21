package com.example.demo

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled//see: https://github.com/Netflix/dgs-framework/discussions/566
class SubscriptionTests {
    private val log = LoggerFactory.getLogger(SubscriptionTests::class.java)

    @LocalServerPort
    var port: Int = 8080

    lateinit var client: WebTestClient

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        this.client = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `sign in and create a post and comment`() {
        val requestData = mapOf(
            "query" to "mutation createPost(\$input: CreatePostInput!){ createPost(createPostInput:\$input) {id, title} }",
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
        )

        val result = this.client
            .post().uri("/graphql").headers { it.setBasicAuth("user", "password") }
            .bodyValue(requestData)
            .exchange()
            .returnResult<HashMap<String, HashMap<String, Post>>>()


        val createPostResult =
            result.responseBody.map { it!!["data"] }.map { it!!["createPost"] }
                .blockLast(Duration.ofSeconds(5L))

        val postId = createPostResult?.id

        assertThat(postId).isNotNull
        assertThat(createPostResult?.title).isEqualTo("test title")


        val postByIdRequestData = mapOf(
            "query" to "query postById(\$id: String!){postById(postId:\$id){ id title }}",
            "variables" to mapOf(
                "id" to postId
            )
        )

        this.client
            .post().uri("/graphql")
            .bodyValue(postByIdRequestData)
            .exchange()
            .expectBody()
            .jsonPath("data.postById.title").isEqualTo("test title")


        val comment1RequestData = mapOf(
            "query" to "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { id postId content}}",
            "variables" to mapOf(
                "input" to mapOf(
                    "postId" to postId,
                    "content" to "comment1"
                )
            )
        )

        val comment2RequestData = mapOf(
            "query" to "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { id postId content}}",
            "variables" to mapOf(
                "input" to mapOf(
                    "postId" to postId,
                    "content" to "comment2"
                )
            )
        )

        this.client
            .post().uri("/graphql")
            .bodyValue(comment1RequestData)
            .exchange()
            .expectBody()
            .jsonPath("data.addComment.content").isEqualTo("comment1")

        this.client
            .post().uri("/graphql")
            .bodyValue(comment2RequestData)
            .exchange()
            .expectBody()
            .jsonPath("data.addComment.content").isEqualTo("comment2")

        val socketClient = ReactorNettyWebSocketClient()
        val commentsReplay = ArrayList<String>(2)

        // The Dgs webflux subscription is handled by `DgsReactiveWebsocketHandler` which implements ApolloGraphQL
        // subscription websocket protocol.
        // see: https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
        //
        // The Vertx Web GraphQL also uses this protocol to handle subscription.
        //
        val subscriptionQuery = mapOf(
            "payload" to mapOf(
                "query" to "subscription onCommentAdded { commentAdded { id postId content } }",
                "extensions" to emptyMap<String, Any>(),
                "variables" to emptyMap<String, Any>()// have to add this to avoid a NPE exception.
            ),
            "type" to "start",
            "id" to 1
        )

        val socketHandler: WebSocketHandler = object : WebSocketHandler {
//            override fun getSubProtocols(): MutableList<String> {
//                return mutableListOf("graphql-ws")
//            }

            override fun handle(session: WebSocketSession): Mono<Void> {
                // payload data format: { 'data': {'commentAdded': {'id': '...', 'title': '...'}}}
                val receiveMono = session.receive()
                    .doOnNext {
                        val text = it.payloadAsText
                        log.debug("receiving text: {}", text)
                        if ("data" == JsonPath.read(text, "$.type")) {
                            val data = objectMapper.convertValue(
                                JsonPath.read(text, "$.payload.data.commentAdded"),
                                Comment::class.java
                            )
                            log.debug("added comment: {}", data)
                            commentsReplay.add(data.content)
                        }

                    }
                    .doOnError { log.error("receiving err:$it") }
                    .log("receiving message:")
                    .then()


                val sendMono = session
                    .send(
                        Mono.delay(Duration.ofMillis(500))
                            .then(Mono.just(objectMapper.writeValueAsString(subscriptionQuery))
                                .map { session.textMessage(it) }
                            )
                    )
                    .log("sending message:")
                    .doOnError { log.error("sending err:$it") }

                return Mono.zip(sendMono, receiveMono).then()
//                    .then(
//                        session.send(
//                            Mono.just(
//                                objectMapper.writeValueAsString(
//                                    mapOf(
//                                        "id" to 1,
//                                        "payload" to emptyMap<String, Any>(),
//                                        "type" to "complete"
//                                    )
//                                )
//                            ).map { session.textMessage(it) }
//                        )
//                    )
//                    .then()

            }
        }

        socketClient
            .execute(
                URI.create("ws://localhost:$port/subscriptions"),
                socketHandler
            )
            .doOnError { log.error("execute err:$it") }
            .block(Duration.ofMillis(500L))

        // limit to the `latest` item in the `Sinks.replay`
        assertThat(commentsReplay).isEqualTo(arrayListOf("comment2"))
    }
}