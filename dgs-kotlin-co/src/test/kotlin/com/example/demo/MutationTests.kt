package com.example.demo

import com.example.demo.gql.types.Post
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest
class MutationTests {

    @Autowired
    lateinit var dgsQueryExecutor: DgsReactiveQueryExecutor

    @MockkBean
    lateinit var postService: PostService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `create new post`() = runTest {
        val post = Post(
            id = UUID.randomUUID().toString(),
            title = "test title",
            content = "test content"
        )
        coEvery { postService.createPost(any()) } returns post

        val requestData = mapOf<String, Any>(
            "query" to "mutation createPost(\$input: CreatePostInput!){ createPost(createPostInput:\$input) {id, title} }",
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
        )

        val titles = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            objectMapper.writeValueAsString(requestData),
            "data.createPost.title"
        )

        StepVerifier.create(titles)
            .consumeNextWith { it shouldBe post.title }
            .verifyComplete()

        coVerify(atLeast = 1) { postService.createPost(any()) }
    }
}


