package com.example.demo

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
            "payload" to mapOf("query" to "subscription onCommentAdded { commentAdded { id postId content } }"),
            "type" to "start",
            "id" to 1
        )

        socketClient.execute(
            URI.create("ws://localhost:$port/subscriptions")
        ) { session: WebSocketSession ->

            // payload data format: { 'data': {'commentAdded': {'id': '...', 'title': '...'}}}
            val receiveMono = session.receive()
                .map {
                    objectMapper.convertValue(
                        JsonPath.read(it.payloadAsText, "$.payload.data.commentAdded"),
                        Comment::class.java
                    )
                }
                .log("receiving message:")

            session
                .send(
                    Mono.delay(Duration.ofMillis(500))
                        .then(Mono.just(objectMapper.writeValueAsString(subscriptionQuery))
                            .map { session.textMessage(it) }
                        )
                )
                .log("sending message:")
                .thenMany(receiveMono)
                .doOnNext {
                    log.debug("added comment: {}", it)
                    commentsReplay.add(it!!.content)
                }
                .then()
                .then(
                    session.send(
                        Mono.just(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "id" to 1,
                                    "payload" to emptyMap<String, Any>(),
                                    "type" to "complete"
                                )
                            )
                        ).map { session.textMessage(it) }
                    )
                )
        }.block(Duration.ofSeconds(5L))

        // limit to the `latest` item in the `Sinks.replay`
        assertThat(commentsReplay).isEqualTo(arrayListOf("comment2"))
    }
}