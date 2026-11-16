package com.example.demo

import com.example.demo.gql.ExceptionHandlers
import com.example.demo.gql.datafetchers.PostsDataFetcher
import com.example.demo.gql.scalars.LocalDateTimeScalar
import com.example.demo.gql.scalars.UUIDScalar
import com.example.demo.gql.types.Post
import com.example.demo.service.PostNotFoundException
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldContainAll
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
 * GraphQL execution behaviour of the queries, executed in-process through `DgsQueryExecutor`
 * with the services mocked out. See `IntegrationTests` for the transport level behaviour.
 */
@SpringBootTest(classes = [QueryTests.TestConfig::class])
@EnableDgsTest
class QueryTests {

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
    fun `get all posts`() {
        every { postService.allPosts() } returns listOf(
            Post(id = UUID.randomUUID(), title = "test title", content = "test content"),
            Post(id = UUID.randomUUID(), title = "test title2", content = "test content2")
        )

        val query = """
            query allPosts {
                allPosts {
                    title
                    content
                }
            }
        """.trimIndent()

        val titles = dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(query, "data.allPosts[*].title")

        titles shouldContainAll listOf("test title", "test title2")

        // verify
        verify(exactly = 1) { postService.allPosts() }
    }

    @Test
    fun `get a post by id`() {
        val post = Post(id = UUID.randomUUID(), title = "test title", content = "test content")
        every { postService.getPostById(post.id) } returns post

        val query = """
            query postById(${'$'}id: UUID!){
                postById(postId:${'$'}id){
                    id
                    title
                }
            }
        """.trimIndent()

        val title = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            query,
            "data.postById.title",
            mapOf("id" to post.id)
        )

        title shouldBe post.title

        // verify
        verify(exactly = 1) { postService.getPostById(post.id) }
    }

    @Test
    fun `get an non-existing post should return errors NOT_FOUND`() {
        val postId = UUID.randomUUID()
        every { postService.getPostById(postId) } throws PostNotFoundException(postId)

        val query = """
            query postById(${'$'}id: UUID!){
                postById(postId:${'$'}id){
                    id
                    title
                }
            }
        """.trimIndent()

        val result = dgsQueryExecutor.execute(query, mapOf("id" to postId))

        result.errors.size shouldBe 1
        result.errors[0].extensions["errorType"] shouldBe "NOT_FOUND"
    }
}
