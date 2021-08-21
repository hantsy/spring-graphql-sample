package com.example.demo

import com.netflix.graphql.dgs.DgsQueryExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class QueryTests {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun `get all posts`() {
        val titles = dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(
            " { allPosts { title content }}",
            "data.allPosts[*].title"
        )

        assertThat(titles).anyMatch { s -> s.contains("Dgs") }
    }
}