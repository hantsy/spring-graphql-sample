package com.example.demo

import com.example.demo.gql.ExceptionHandlers
import com.example.demo.gql.datafetchers.PostsDataFetcher
import com.example.demo.gql.scalars.LocalDateTimeScalar
import com.example.demo.gql.scalars.UUIDScalar
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CommentInput
import com.example.demo.gql.types.Post
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.*

/**
 * GraphQL execution behaviour of the mutations, executed in-process through `DgsQueryExecutor`
 * with the services mocked out. See `IntegrationTests` for the transport level behaviour.
 */
@SpringBootTest(classes = [MutationTests.TestConfig::class])
@EnableDgsTest
class MutationTests {

    @Configuration
    @Import(
        value = [
            PostsDataFetcher::class,
            ExceptionHandlers::class,
            UUIDScalar::class,
            LocalDateTimeScalar::class,
        ]
    )
    class TestConfig

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockkBean
    lateinit var postService: PostService

    @Test
    fun `create a new post`() {
        val post = Post(id = UUID.randomUUID(), title = "test title", content = "test content")
        every { postService.createPost(any()) } returns post

        val query = """
            mutation createPost(${'$'}input: CreatePostInput!){
                createPost(createPostInput:${'$'}input){
                    id
                    title
                }
            }
        """.trimIndent()

        val variables = mapOf(
            "input" to mapOf(
                "title" to "test title",
                "content" to "test content"
            )
        )

        val title = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            query,
            "data.createPost.title",
            variables
        )

        title shouldBe post.title

        // verify
        verify(exactly = 1) { postService.createPost(any()) }
    }

    @Test
    fun `add a comment to a post`() {
        val comment = Comment(
            id = UUID.randomUUID(),
            content = "test comment",
            postId = UUID.randomUUID()
        )
        every { postService.addComment(any()) } returns comment

        val query = """
            mutation addComment(${'$'}input: CommentInput!){
                addComment(commentInput:${'$'}input){
                    id
                    postId
                    content
                }
            }
        """.trimIndent()

        val variables = mapOf(
            "input" to mapOf(
                "postId" to comment.postId,
                "content" to comment.content
            )
        )

        val content = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            query,
            "data.addComment.content",
            variables
        )

        content shouldBe comment.content

        // verify
        verify(exactly = 1) { postService.addComment(any()) }
    }
}
