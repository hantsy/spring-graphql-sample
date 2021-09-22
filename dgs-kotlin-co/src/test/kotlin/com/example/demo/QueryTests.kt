package com.example.demo

import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest
class QueryTests {

    @Autowired
    lateinit var dgsQueryExecutor: DgsReactiveQueryExecutor

    @Test
    fun `get all posts`() {
        val titles = dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(
            " { allPosts { title content }}",
            "data.allPosts[*].title"
        )

        StepVerifier.create(titles)
            .consumeNextWith { it: List<String>? ->
                assertThat(it).anyMatch { s -> s.contains("Dgs") }
            }
            .verifyComplete()
    }
}