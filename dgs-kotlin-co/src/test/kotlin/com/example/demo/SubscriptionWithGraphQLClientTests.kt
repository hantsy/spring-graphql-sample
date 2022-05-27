package com.example.demo

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.netflix.graphql.dgs.client.WebSocketGraphQLClient
import io.kotest.assertions.timing.continually
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
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds

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
    fun `sign in and create a post and comment`() = runTest {

        //create a new post
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
        postId shouldNotBe null
        createPostResult?.title shouldBe "test title"

        // get post by id.
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

        // subscribe to commentadded subscription
        val commentAddedSubscriptionQuery = "subscription onCommentAdded{ commentAdded{ id postId  content}}"
        val commentAddedResultFlux = websocketClient
            .reactiveExecuteQuery(
                commentAddedSubscriptionQuery,
                emptyMap()
            )

        val comments = CopyOnWriteArrayList<Comment>()
        commentAddedResultFlux.subscribe {
            val addedComment = it.extractValueAsObject("commentAdded", Comment::class.java)
            log.debug("The payload of commentAdded: $addedComment")
            comments.add(addedComment)
        }

        // add comments to post
        val commentQuery =
            "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { id postId content}}"
        val comment1Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to "comment1"
            )
        )
        val comment2Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to "comment2"
            )
        )

        client.reactiveExecuteQuery(commentQuery, comment1Variables).awaitSingle()
        client.reactiveExecuteQuery(commentQuery, comment2Variables).awaitSingle()

        //verify the commentAdded event is tracked.
        continually(5.seconds) {
            comments.size shouldBe 2
        }

    }
}