package com.example.demo

import com.netflix.graphql.dgs.DgsQueryExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*


@SpringBootTest(
    classes = [DemoApplication::class],
    properties = ["context.initializer.classes=com.example.demo.TestConfigInitializer"]
)
class DemoApplicationTests {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun `get all posts`() {
        val query = """
			query posts{
                allPosts{
                    id
                    title
                    content
                    author { id name email }
                    comments { id content }
                }
			}""".trimIndent()

        val titles = dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(query, "data.allPosts[*].title")
        assertThat(titles.size).isEqualTo(0);
    }

    @Test
    fun `get an non-existing post`() {
        val query = "query notExisted(\$id: String!){postById(postId:\$id){ id title }}"

        val errorType = dgsQueryExecutor.executeAndExtractJsonPath<Object>(
            query,
            "errors[0].*",
            mapOf("id" to UUID.randomUUID().toString())
        )
        assertThat(errorType).isNotNull
        //assertThat(errorType).isEqualTo("NOT_FOUND")
    }

}
