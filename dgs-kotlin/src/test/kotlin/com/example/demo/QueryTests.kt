package com.example.demo

import com.netflix.graphql.dgs.DgsQueryExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.*


@SpringBootTest(
    classes = [DemoApplication::class]
)
@Import(TestcontainersConfiguration::class)
class QueryTests {

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
    fun `get an non-existing post should return errors NOT_FOUND`() {
        val query = """
            query notExisted(${'$'}id: UUID!){
                postById(postId:${'$'}id){ 
                    id
                    title 
                }
            }
            """.trimIndent()

        val result = dgsQueryExecutor.execute(query, mapOf("id" to UUID.randomUUID()))
        assertThat(result.errors).isNotNull
        assertThat(result.errors[0].extensions["errorType"]).isEqualTo("NOT_FOUND")
    }
}