package com.example.demo

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.client.WebSocketGraphQlClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalKotest::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {
    companion object {
        private val log = LoggerFactory.getLogger(IntegrationTests::class.java)
    }

    @LocalServerPort
    var port: Int = 8080

    lateinit var client: WebClientGraphQLClient

    lateinit var socketClient: WebSocketGraphQlClient

    @BeforeEach
    fun setup() {
        val webClient = WebClient.builder()
            .defaultHeaders { it.setBasicAuth("user", "password") }
            .baseUrl("http://localhost:$port/graphql")
            .build()

        client = WebClientGraphQLClient(webClient)
        socketClient = WebSocketGraphQlClient.create(
            URI.create("ws://localhost:$port/graphql"),
            ReactorNettyWebSocketClient()
        )
        this.socketClient.start().subscribe()
    }

    @AfterEach
    fun teardown() {
        this.socketClient.stop().subscribe()
    }

    @Test
    fun `create a post and comment`() = runTest {

        //create a new post
        val postId = createPost()

        // get post by id.
        getPostById(postId)

        // subscribe to `commentAdded`
        val commentAddedSubscriptionQuery =
            """
            subscription onCommentAdded
            { 
                commentAdded
                { 
                    id 
                    postId  
                    content
                }
            }
            """.trimIndent()
        val verifier = socketClient
            .document(commentAddedSubscriptionQuery).executeSubscription()
            .mapNotNull {
                log.debug("WebSocket client response: $it")
                it.getData<Map<String, Map<String, Any>>>()!!["commentAdded"]
            }
            .doOnNext {
                log.debug("doOnNext: $it")
            }
            //.subscribe { it -> comments.add(it) }

            .`as` { StepVerifier.create(it) }
            .thenAwait(Duration.ofMillis(1000))
            .consumeNextWith {                it!!["content"]!! shouldBe "comment1"            }
            .consumeNextWith {                it!!["content"]!! shouldBe "comment2"            }
            .thenCancel()
            .verifyLater() // delay to verify later

        // add comments to post
        addComment(postId, "comment1")
        addComment(postId, "comment2")
        //addComment(postId, "comment3 ")

        // verify the result now.
        verifier.verify()
    }

    private suspend fun addComment(postId: UUID, comment: String) {
        val commentQuery =
            """
                mutation addComment(${'$'}input: CommentInput!) 
                { 
                    addComment(commentInput:${'$'}input) 
                    { 
                        id 
                        postId 
                        content
                    }
                }
            """.trimIndent()
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

    private suspend fun getPostById(postId: UUID) {
        val postByIdQuery = """
            query postById(${'$'}id: UUID!)
            {
                postById(postId:${'$'}id)
                { 
                    id 
                    title 
                }
            }
        """.trimIndent()
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

    private suspend fun createPost(): UUID {
        val createPostQuery =
            """
                mutation createPost(${'$'}input: CreatePostInput!)
                { 
                    createPost(createPostInput:${'$'}input) 
                    {
                        id 
                        title
                    } 
                }
            """.trimIndent()
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
        createPostResult.title shouldBe "test title"
        return postId
    }
}