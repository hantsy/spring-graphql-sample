package com.example.demo

import com.example.demo.gql.datafetchers.PostController
import com.example.demo.gql.types.Post
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.GraphQlTester
import java.time.LocalDateTime
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@GraphQlTest(controllers = [PostController::class])
@Import(ValidationConfig::class)
internal class QueryTests {
    companion object {
        private val log = LoggerFactory.getLogger(QueryTests::class.java)
    }

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var authorService: AuthorService

    @Test
    fun `get all posts`() = runTest {
        coEvery { postService.allPosts() } returns
                flowOf(
                    Post(
                        id = UUID.randomUUID(),
                        title = "Post 1",
                        content = "Post 1 content",
                        status = PostStatus.DRAFT,
                        createdAt = LocalDateTime.now()
                    ),
                    Post(
                        id = UUID.randomUUID(),
                        title = "Post 2",
                        content = "Post 2 content",
                        status = PostStatus.DRAFT,
                        createdAt = LocalDateTime.now()
                    )
                )
        val query = "{ allPosts { title content }}"
        graphQlTester.document(query).execute()
            .errors().satisfy { it.forEach { error -> log.debug("error message: ${error.message}") } }
//        graphQlTester.document(query)
//            .execute()
//            .path("data.allPosts[*].title")
//            .entityList(String::class.java).contains("test title", "test title2")

        coVerify(exactly = 1) { postService.allPosts() }
    }

    @Test
    fun `get post by id`() = runTest {
        coEvery { postService.getPostById(any()) } returns
                Post(
                    id = UUID.randomUUID(),
                    title = "test title",
                    content = "test content",
                    status = PostStatus.DRAFT,
                    createdAt = LocalDateTime.now()
                )
        val query = "query postById(\$postId:ID!){ postById(postId:\$postId) { title content }}"
        graphQlTester.document(query)
            .variable("postId", UUID.randomUUID())
            .execute()
            .path("data.postById.title")
            .entity(String::class.java).isEqualTo("test title".uppercase())

        coVerify(exactly = 1) { postService.getPostById(any()) }
    }

    @Test
    fun `get post by id when not found`() = runTest {
        val id = UUID.randomUUID()
        coEvery { postService.getPostById(any()) } throws PostNotFoundException(id)

        val query = "query postById(\$postId:ID!){ postById(postId:\$postId) { title content }}"
        graphQlTester.document(query)
            .variable("postId", id)
            .execute()
            .errors()
            .satisfy { it: List<ResponseError> ->
                it.size shouldBeGreaterThan 0
                it[0].message shouldBe "Post: $id was not found."
            }
        coVerify(exactly = 1) { postService.getPostById(any()) }
    }
}
