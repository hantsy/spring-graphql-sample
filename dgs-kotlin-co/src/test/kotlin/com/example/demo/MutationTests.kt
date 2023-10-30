package com.example.demo

import com.example.demo.gql.datafetcher.AuthorsDataFetcher
import com.example.demo.gql.scalar.LocalDateTimeScalar
import com.example.demo.gql.datafetcher.PostsDataFetcher
import com.example.demo.gql.types.Post
import com.example.demo.service.AuthorService
import com.example.demo.service.PostService
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor
import com.netflix.graphql.dgs.webflux.autoconfiguration.DgsWebFluxAutoConfiguration
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(classes = [QueryTests.TestConfig::class])
class MutationTests {

    @Configuration
    @Import(
        value = [
            PostsDataFetcher::class,
            LocalDateTimeScalar::class
        ]
    )
    @ImportAutoConfiguration(
        value = [
            DgsWebFluxAutoConfiguration::class,
            DgsAutoConfiguration::class,
            WebFluxAutoConfiguration::class
        ]
    )
    class TestConfig

    @Autowired
    lateinit var dgsQueryExecutor: DgsReactiveQueryExecutor


    @MockkBean
    lateinit var postService: PostService

    @Test
    fun `create new post`() = runTest {
        val post = Post(
            id = UUID.randomUUID().toString(),
            title = "test title",
            content = "test content"
        )
        coEvery { postService.createPost(any()) } returns post

        val query = """
            mutation createPost(${'$'}input: CreatePostInput!){
                 createPost(createPostInput:${'$'}input) 
                 {
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

        val titles = dgsQueryExecutor
            .executeAndExtractJsonPath<String>(
                query,
                "data.createPost.title",
                variables
            )
            .awaitSingle()

        titles shouldBe post.title

        // verify
        coVerify(atLeast = 1) { postService.createPost(any()) }
    }
}


