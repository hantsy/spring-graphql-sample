package com.example.demo

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.nondeterministic.continually
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.rsocket.server.LocalRSocketServerPort
import org.springframework.graphql.client.ClientGraphQlResponse
import org.springframework.graphql.client.RSocketGraphQlClient
import org.springframework.http.MediaType
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class IntegrationTests {
    companion object {
        private val log = LoggerFactory.getLogger(IntegrationTests::class.java)
    }

    lateinit var client: RSocketGraphQlClient

    @LocalRSocketServerPort
    var port: Int = 0

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() = runTest {
        log.debug("connecting to port: $port")
        client = RSocketGraphQlClient.builder()
            .dataMimeType(MediaType.APPLICATION_JSON)
            .tcp("localhost", port)
            .route("graphql")
            .build()
        client.start().awaitSingleOrNull()
    }

    @AfterEach
    fun tearDown() = runTest {
        client.stop().awaitSingleOrNull()
    }

    @Test
    fun `add comment and subscription integration tests`() = runTest {
        //create post
        val inputPlaceHolder = "\$input"
        val createPostQuery: String = """
            mutation createPost($inputPlaceHolder: CreatePostInput!){
                createPost(createPostInput:$inputPlaceHolder){
                    id
                    title
                    content
                }
            }""".trim()
        val createPostVariables = java.util.Map.of<String, Any>(
            "input", java.util.Map.of(
                "title", "my post created by Spring GraphQL",
                "content", "content of my post"
            )
        )

        val postId = client.document(createPostQuery).variables(createPostVariables).execute()
            .map { response: ClientGraphQlResponse ->
                objectMapper.convertValue(
                    response.getData<Map<String, Any>>()!!["createPost"],
                    Post::class.java
                )
            }
            .awaitSingle()
            .id

        postId shouldNotBe null
        val postIdPlaceHolder = "\$postId"
        val postById: String = """
            query post($postIdPlaceHolder:ID!){
                postById(postId:$postIdPlaceHolder) {
                    id
                    title
                    content
                }
            }""".trim()
        val post = client.document(postById).variable("postId", postId)
            .execute()
            .map { response: ClientGraphQlResponse ->
                objectMapper.convertValue(
                    response.getData<Map<String, Any>>()!!["postById"],
                    Post::class.java
                )
            }
            .awaitSingleOrNull()

        post shouldNotBe null
        post?.id shouldBe postId

        post?.title shouldBe "my post created by Spring GraphQL".uppercase()
        post?.content shouldBe "content of my post"

        val subscriptionQuery = "subscription onCommentAdded { commentAdded { id content} }"
        val comments = CopyOnWriteArrayList<Comment>()
        client.document(subscriptionQuery)
            .executeSubscription()
            .map { response: ClientGraphQlResponse ->
                objectMapper.convertValue(
                    response.getData<Map<String, Any>>()!!["commentAdded"],
                    Comment::class.java
                )
            }
            .subscribe { comments.add(it) }

        addCommentToPost(postId!!)
        addCommentToPost(postId)
        addCommentToPost(postId)

        continually(5.seconds) {
            comments.size shouldBe 3
        }
    }

    suspend fun addCommentToPost(id: UUID) {
        val commentInputPlaceHolder = "\$commentInput"
        val addCommentQuery = """
                mutation addComment($commentInputPlaceHolder: CommentInput!){
                   addComment(commentInput:$commentInputPlaceHolder){
                       id 
                       content
                   }
                }""".trim()

        val addCommentVariables = mapOf(
            "commentInput" to
                    mapOf(
                        "postId" to id,
                        "content" to "comment of my post at " + LocalDateTime.now()
                    )
        )
        val comment = client.document(addCommentQuery)
            .variables(addCommentVariables)
            .execute()
            .map { response: ClientGraphQlResponse ->
                objectMapper.convertValue(
                    response.getData<Map<String, Any>>()!!["addComment"],
                    Comment::class.java
                )
            }
            .awaitSingle()

        comment shouldNotBe null
        comment?.id shouldNotBe null
    }
}

