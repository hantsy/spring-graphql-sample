package com.example.demo

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.netflix.graphql.dgs.client.WebSocketGraphQLClient
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.test.StepVerifier

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubscriptionWithGraphQLClientTests {
    companion object {
        private val log = LoggerFactory.getLogger(SubscriptionWithGraphQLClientTests::class.java)
    }

    @LocalServerPort
    var port: Int = 8080

    lateinit var client: WebClientGraphQLClient

    lateinit var websocketClient: WebSocketGraphQLClient

    @BeforeEach
    fun setup() {
        val webClient = WebClient.builder()
            .defaultHeaders { it.setBasicAuth("user", "password") }
            .baseUrl("http://localhost:$port/graphql")
            .build()

        client = WebClientGraphQLClient(webClient)
        websocketClient = WebSocketGraphQLClient("ws://localhost:$port/subscriptions", ReactorNettyWebSocketClient())
    }

    @Test
    fun `create a post and comment`() = runTest {

        //create a new post
        val postId = createPost()

        // get post by id.
        getPostById(postId)

        // subscribe to commentadded subscription
        val comments = mutableListOf<Comment>()
        val commentAddedSubscriptionQuery = "subscription onCommentAdded{ commentAdded{ id postId  content}}"
        val verifier = websocketClient
            .reactiveExecuteQuery(
                commentAddedSubscriptionQuery,
                emptyMap()
            )
            .map {
                it.extractValueAsObject("commentAdded", Comment::class.java)
            }
            .doOnNext {
                log.debug("doOnNext: $it")
            }
            .`as` { StepVerifier.create(it) }
            .expectNextCount(1) // only one comment in the sinks.
            .thenCancel()
            .verifyLater()

        // add comments to post
        addComment(postId, "comment1")
        addComment(postId, "comment2")
        addComment(postId, "comment3 ")

        //verify the commentAdded event is tracked.
        verifier.verify()
    }

    private suspend fun addComment(postId: String, comment: String) {
        val commentQuery =
            "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { id postId content}}"
        val comment1Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to comment
            )
        )

        val addedComment = client.reactiveExecuteQuery(commentQuery, comment1Variables)
            .map { it.extractValueAsObject("addComment", Comment::class.java) }
            .awaitSingle()
        addedComment.content shouldBe comment
    }

    private suspend fun getPostById(postId: String) {
        val postByIdQuery = "query postById(\$id: String!){postById(postId:\$id){ id title }}"
        val postByIdVariables = mapOf(
            "id" to postId as Any
        )

        val postByIdResult = client.reactiveExecuteQuery(postByIdQuery, postByIdVariables)
            .map { it.extractValueAsObject("postById", Post::class.java) }
            .awaitSingleOrNull()
        log.debug("postByIdResult: $postByIdResult")
        postByIdResult shouldNotBe null
        postByIdResult?.title shouldBe "test title"
    }

    private suspend fun createPost(): String {
        val createPostQuery =
            "mutation createPost(\$input: CreatePostInput!){ createPost(createPostInput:\$input) {id, title} }"
        val createPostQueryVariables = mapOf(
            "input" to mapOf(
                "title" to "test title",
                "content" to "test content"
            )
        )
        val createPostResult =
            client.reactiveExecuteQuery(createPostQuery, createPostQueryVariables)
                .map { it.extractValueAsObject("createPost", Post::class.java) }
                .awaitSingle()
        log.debug("createPostResult: $createPostResult")
        createPostResult shouldNotBe null
        val postId = createPostResult?.id
        postId!! shouldNotBe null
        createPostResult?.title shouldBe "test title"
        return postId
    }
}