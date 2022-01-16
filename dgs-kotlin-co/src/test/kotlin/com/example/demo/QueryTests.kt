package com.example.demo

import com.example.demo.gql.types.Post
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import reactor.test.StepVerifier
import java.util.*

@SpringBootTest
class QueryTests {

    @Autowired
    lateinit var dgsQueryExecutor: DgsReactiveQueryExecutor

    @MockBean
    lateinit var postService: PostService

    @Test
    fun `get all posts`() {
        `when`(postService.allPosts()).thenReturn(
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
        )
        val titles = dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(
            " { allPosts { title content }}",
            "data.allPosts[*].title"
        )

        StepVerifier.create(titles)
            .consumeNextWith { it: List<String>? ->
                assertThat(it).containsAll(listOf("test title", "test title2"))
            }
            .verifyComplete()

        verify(postService, times(1)).allPosts()
        verifyNoMoreInteractions(postService)
    }
}