package com.example.demo

import com.netflix.graphql.dgs.DgsQueryExecutor
import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.security.test.context.support.WithMockUser
import reactor.test.StepVerifier
import java.util.*


@SpringBootTest(
    classes = [DemoApplication::class],
    properties = ["context.initializer.classes=com.example.demo.TestConfigInitializer"]
)
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
        val query = "query notExisted(\$id: String!){postById(postId:\$id){ id title }}"

        val result = dgsQueryExecutor.execute(query, mapOf("id" to UUID.randomUUID().toString()))
        assertThat(result.errors).isNotNull
        assertThat(result.errors[0].extensions["errorType"]).isEqualTo("NOT_FOUND")
    }

    //see: https://github.com/Netflix/dgs-framework/issues/453
    //and: https://github.com/Netflix/dgs-framework/issues/437#issuecomment-871100938
    @Test
    @Disabled // see `MutationTests` for the details.
    fun `create a post without auth should return errors PERMISSION_DENIED`() {
        val query = "mutation createPost(\$input: CreatePostInput!){createPost(createPostInput:\$input){ id title }}"

        val result = dgsQueryExecutor.execute(
            query,
            mapOf("input" to mapOf("title" to "my title", "content" to "my content of my title"))
        )

        assertThat(result.errors).isNotNull
        println("errors: " + result.errors)
        // it prints out:
        // errors: [TypedGraphQLError{message='org.springframework.security.authentication.AuthenticationCredentialsNotFoundException:
        // An Authentication object was not found in the SecurityContext',locations=[], path=[createPost], extensions={errorType=INTERNAL}}]
        //
        // see the explanation at  https://github.com/Netflix/dgs-framework/issues/437#issuecomment-871100938
        //assertThat(result.errors[0].extensions["errorType"]).isEqualTo("PERMISSION_DENIED")
    }
}