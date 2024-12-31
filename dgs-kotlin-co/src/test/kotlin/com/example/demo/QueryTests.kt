package com.example.demo

import com.example.demo.gql.datafetcher.PostsDataFetcher
import com.example.demo.gql.scalar.LocalDateTimeScalar
import com.example.demo.gql.scalars.UUIDScalar
import com.example.demo.gql.types.Post
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import com.ninjasquad.springmockk.MockkBean
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(classes = [QueryTests.TestConfig::class])
@EnableDgsTest
class QueryTests {
    companion object {
        private val log = LoggerFactory.getLogger(QueryTests::class.java)
    }

    @Configuration
    @Import(
        value = [
            PostsDataFetcher::class,
            LocalDateTimeScalar::class
        ]
    )
    @ImportAutoConfiguration(
        value = [
            WebFluxAutoConfiguration::class
        ]
    )
    class TestConfig{
        @Bean
        fun customRuntimeWiring(): RuntimeWiringConfigurer{
            return object: RuntimeWiringConfigurer {
                override fun configure(builder: RuntimeWiring.Builder) {
                    builder.scalar(
                        GraphQLScalarType.newScalar()
                            .name("UUID")
                            .description("UUID type")
                            .coercing(UUIDScalar())
                            .build()
                    )
                }
            }
        }
    }

    @Autowired
    lateinit var dgsQueryExecutor: DgsReactiveQueryExecutor

    @MockkBean
    lateinit var postService: PostService

    @Test
    fun `get all posts`() = runTest {
        coEvery { postService.allPosts() } returns
                flowOf(
                    Post(
                        id = UUID.randomUUID(),
                        title = "test title",
                        content = "test content"
                    ),
                    Post(
                        id = UUID.randomUUID(),
                        title = "test title2",
                        content = "test content2"
                    ),
                )

        val query = """
            query allPosts 
            { 
                allPosts 
                { 
                    title 
                    content 
                }
            }
        """.trimIndent()

        val titles = dgsQueryExecutor
            .executeAndExtractJsonPath<List<String>>(
                query,
                "data.allPosts[*].title"
            )
            .awaitSingle()

        titles shouldContainAll listOf("test title", "test title2")

        //verify
        coVerify(exactly = 1) { postService.allPosts() }
    }
}