package com.example.demo

import com.example.demo.gql.types.Comment
import com.netflix.graphql.dgs.client.DgsGraphqlSSESubscriptionGraphQLClient
import com.netflix.graphql.dgs.client.DgsWebClientGraphQLClient
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.graphql.client.WebSocketGraphQlClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import java.util.UUID

/**
 * Transport level behaviour of the GraphQL endpoint, exercised through the GraphQL client
 * libraries: the DGS WebClient, the DGS SSE subscription client and Spring GraphQL's
 * built-in WebSocket client.
 * See `WebClient_IntegrationTests` for the same behaviour seen without any client library.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestcontainersConfiguration::class)
class IntegrationTests {

    companion object {
        private val log = LoggerFactory.getLogger(IntegrationTests::class.java)

        private const val CREATE_POST_MUTATION = """
            mutation createPost(${'$'}input: CreatePostInput!){
                createPost(createPostInput:${'$'}input){
                    id
                    title
                }
            }
        """

        private const val ADD_COMMENT_MUTATION = """
            mutation addComment(${'$'}input: CommentInput!){
                addComment(commentInput:${'$'}input){
                    id
                    postId
                    content
                }
            }
        """

        private const val COMMENT_ADDED_SUBSCRIPTION = """
            subscription onCommentAdded {
                commentAdded {
                    id
                    postId
                    content
                }
            }
        """

        private val CREATE_POST_VARIABLES = mapOf(
            "input" to mapOf(
                "title" to "test title",
                "content" to "test content"
            )
        )

        // The service publishes the comments through a `SharedFlow` with a replay buffer, so a
        // subscriber that connects later on also receives the comment of a previously executed
        // test. The contents are therefore unique per test and the foreign ones are filtered out.
        private const val COMMENT_1 = "comment1"
        private const val COMMENT_2 = "comment2"
        private const val SSE_COMMENT = "sse comment"
    }

    @LocalServerPort
    var port: Int = 8080

    lateinit var client: DgsWebClientGraphQLClient

    lateinit var sseClient: DgsGraphqlSSESubscriptionGraphQLClient

    lateinit var socketClient: WebSocketGraphQlClient

    @BeforeEach
    fun setup() {
        val graphqlUrl = "http://localhost:$port/graphql"

        client = DgsWebClientGraphQLClient(
            WebClient.builder()
                .defaultHeaders { it.setBasicAuth("user", "password") }
                .baseUrl(graphqlUrl)
                .build()
        )

        sseClient = DgsGraphqlSSESubscriptionGraphQLClient(graphqlUrl, WebClient.create(graphqlUrl))

        socketClient = WebSocketGraphQlClient.create(
            URI.create("ws://localhost:$port/graphql"),
            ReactorNettyWebSocketClient()
        )
        socketClient.start().block(Duration.ofSeconds(30))
    }

    @AfterEach
    fun teardown() {
        socketClient.stop().block(Duration.ofSeconds(30))
    }

    @Test
    fun `create a post and comment over the websocket subscription`() = runTest {
        // create a new post
        val postId = createPost()

        // subscribe to `commentAdded`
        val verifier = socketClient
            .document(COMMENT_ADDED_SUBSCRIPTION)
            .executeSubscription()
            .mapNotNull {
                log.debug("WebSocket client response: {}", it)
                it.getData<Map<String, Map<String, Any>>>()!!["commentAdded"]
            }
            // ignore the comments that were published by the other tests
            .filter { it["content"] == COMMENT_1 || it["content"] == COMMENT_2 }
            .`as` { StepVerifier.create(it) }
            .thenAwait(Duration.ofMillis(1000))
            .consumeNextWith { it!!["content"] shouldBe COMMENT_1 }
            .consumeNextWith { it!!["content"] shouldBe COMMENT_2 }
            .thenCancel()
            .verifyLater()

        // The subscription has to be established on the server before the comments are added,
        // otherwise the first comment would be replaced in the replay buffer of the `SharedFlow`.
        Thread.sleep(1000)

        // add comments to the post
        addComment(postId, COMMENT_1)
        // a short delay, so that the first comment is delivered before the second one is published
        Thread.sleep(500)
        addComment(postId, COMMENT_2)

        // verify the result now.
        verifier.verify(Duration.ofSeconds(30))
    }

    @Test
    fun `subscribe to the comments over SSE`() = runTest {
        val postId = createPost()

        val comments = sseClient
            .reactiveExecuteQuery(COMMENT_ADDED_SUBSCRIPTION, emptyMap<String, Any>())
            .map { response ->
                response.extractValueAsObject("data.commentAdded", Map::class.java)
            }
            // ignore the comments that were published by the other tests
            .filter { it["content"] == SSE_COMMENT }

        val verifier = StepVerifier.create(comments)
            .consumeNextWith { comment ->
                comment["content"] shouldBe SSE_COMMENT
            }
            .thenCancel()
            .verifyLater()

        // The subscription has to be established on the server before the comment is added,
        // otherwise it would be lost - the replay buffer only keeps the last published comment.
        Thread.sleep(1000)

        addComment(postId, SSE_COMMENT)

        verifier.verify(Duration.ofSeconds(30))
    }

    private suspend fun createPost(): UUID {
        val createPostResult = client
            .reactiveExecuteQuery(CREATE_POST_MUTATION, CREATE_POST_VARIABLES)
            .map { it.extractValueAsObject("createPost", Map::class.java) }
            .awaitSingle()
        log.debug("createPostResult: {}", createPostResult)

        val postId = createPostResult["id"] as String
        postId.isNotBlank() shouldBe true
        createPostResult["title"] shouldBe "test title"

        return UUID.fromString(postId)
    }

    private suspend fun addComment(postId: UUID, comment: String) {
        val addedComment = client
            .reactiveExecuteQuery(
                ADD_COMMENT_MUTATION,
                mapOf(
                    "input" to mapOf(
                        "postId" to postId,
                        "content" to comment
                    )
                )
            )
            .map { it.extractValueAsObject("addComment", Comment::class.java) }
            .awaitSingle()

        addedComment.content shouldBe comment
    }
}
