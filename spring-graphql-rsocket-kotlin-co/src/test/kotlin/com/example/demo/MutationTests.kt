package com.example.demo

import com.example.demo.gql.datafetchers.PostController
import com.example.demo.gql.types.Post
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.graphql.test.tester.GraphQlTester
import java.time.LocalDateTime
import java.util.*

@GraphQlTest(controllers = [PostController::class])
internal class MutationTests {
    companion object {
        private val log = LoggerFactory.getLogger(MutationTests::class.java)
    }

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var authorService: AuthorService

    @Test
    fun createPosts() = runTest {
        coEvery { postService.createPost(any()) } returns
                Post(
                    id = UUID.randomUUID(),
                    title = "test title",
                    content = "test content",
                    status = PostStatus.DRAFT,
                    createdAt = LocalDateTime.now()
                )

        val inputHolder = "\$input"
        val query = """
                mutation createPost($inputHolder: CreatePostInput!){
                    createPost(createPostInput:$inputHolder){
                        id, 
                        title, 
                        content
                    }
                }
            """.trimIndent()
        graphQlTester.document(query)
            .variable(
                "input",
                mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
            .execute()
            .path("data.createPost.title")
            .entity(String::class.java)
            .isEqualTo("TEST TITLE")

        coVerify(exactly = 1) { postService.createPost(any()) }
    }
}
