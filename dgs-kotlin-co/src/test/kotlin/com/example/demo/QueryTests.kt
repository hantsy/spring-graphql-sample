package com.example.demo

import com.example.demo.gql.types.Post
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor
import com.netflix.graphql.dgs.webflux.autoconfiguration.DgsWebFluxAutoConfiguration
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldContainAll
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(
    classes = [
        QueryTests.TestConfig::class,
    ]
)
class QueryTests {
    companion object {
        private val log = LoggerFactory.getLogger(QueryTests::class.java)
    }

    @Configuration
    @Import(
        value = [
            AuthorsDataFetcher::class,
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
    class TestConfig {

    }

    @Autowired
    lateinit var dgsQueryExecutor: DgsReactiveQueryExecutor

    @MockkBean
    lateinit var authorService: AuthorService

    @MockkBean
    lateinit var postService: PostService

    @Test
    fun `get all posts`() = runTest {
        coEvery { postService.allPosts() } returns
                flowOf(
                    Post(
                        id = UUID.randomUUID().toString(),
                        title = "test title",
                        content = "test content"
                    ),
                    Post(
                        id = UUID.randomUUID().toString(),
                        title = "test title2",
                        content = "test content2"
                    ),
                )

        val titles = dgsQueryExecutor
            .executeAndExtractJsonPath<List<String>>(
                "{ allPosts { title content }}",
                "data.allPosts[*].title"
            )
            .awaitSingle()

        titles shouldContainAll listOf("test title", "test title2")

        //verify
        coVerify(exactly = 1) { postService.allPosts() }
    }
}