package com.example.demo

import com.example.demo.gql.types.AuthResult
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.client.CustomGraphQLClient
import com.netflix.graphql.dgs.client.GraphQLClient
import com.netflix.graphql.dgs.client.HttpResponse
import com.netflix.graphql.dgs.client.RequestExecutor
import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.POST
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import reactor.test.StepVerifier


@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [TestConfigInitializer::class])
@Import(SubscriptionWithGraphQLClientTests.TestConfig::class)
@Disabled
class SubscriptionWithGraphQLClientTests {

    @LocalServerPort
    var port: Int = 8080

    @Autowired
    lateinit var restTemplate: RestTemplate

    lateinit var client: CustomGraphQLClient

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @BeforeEach
    fun setup() {
        val requestExecutor = RequestExecutor { url, headers, body ->
            val requestHeaders = HttpHeaders()
            headers.forEach { requestHeaders[it.key] = it.value }
            requestHeaders.set("Content-Type", "application/graphql")
            requestHeaders.set("Accept", "application/json, plain/text, */*")

            val result = restTemplate.exchange(
                url,
                POST,
                HttpEntity(body, requestHeaders),
                String::class.java
            )
            HttpResponse(result.statusCode.value(), result.body)
        }
        client = GraphQLClient.createCustom("http://localhost:$port/graphql", requestExecutor)
    }

    @Test
    fun `sign in and create a post and comment`() {
        // logged in
        val query = "mutation signIn(\$input:Credentials!){ signIn(credentials:\$input) {name, roles, token} }"

        val variables = mapOf(
            "input" to mapOf(
                "username" to "user",
                "password" to "password"
            )
        )


        val signinResult = client.executeQuery(query, variables)

        val authResult: AuthResult = signinResult.extractValueAsObject("signIn", AuthResult::class.java)
        assertThat(authResult).isNotNull
        assertThat(authResult.name).isEqualTo("user")
        assertThat(authResult.roles).contains("ROLE_USER")

        val token = authResult.token
        assertThat(token).isNotNull

        // create a new post
        val createPostQuery =
            "mutation createPost(\$input: CreatePostInput!){createPost(createPostInput:\$input){ id  title }}"

        val createPostVariables = mapOf(
            "input" to mapOf(
                "title" to "my title",
                "content" to "my content of my title"
            )
        )

        val requestExecutorWithAuth = RequestExecutor { url, headers, body ->

            val requestHeaders = HttpHeaders()
            headers.forEach { requestHeaders[it.key] = it.value }
            requestHeaders.add("X-Auth-Token", token)
            requestHeaders.set("Content-Type", "application/graphql")
            requestHeaders.set("Accept", "application/json, plain/text, */*")
            val result = restTemplate.exchange(
                url,
                POST,
                HttpEntity(body, requestHeaders),
                String::class.java
            )
            HttpResponse(result.statusCode.value(), result.body)
        }


        client = GraphQLClient.createCustom("http://localhost:$port/graphql", requestExecutorWithAuth)
        val createPostResponse =
            client.executeQuery(createPostQuery, createPostVariables)

        val createPostResult = createPostResponse.extractValueAsObject("createPost", Post::class.java)
        val postId = createPostResult.id
        assertThat(postId).isNotNull
        assertThat(createPostResult.title).isEqualTo("my title")

        // get post by id.
        val postByIdQuery = "query postById(\$id: String!){postById(postId:\$id){ id title }}"
        val postByIdVariables = mapOf(
            "id" to postId
        )

        val postByIdResponse = client.executeQuery(postByIdQuery, postByIdVariables)

        val postByIdResult = postByIdResponse.extractValueAsObject("postById", Post::class.java)
        assertThat(postByIdResult.title).isEqualTo("my title")

        // subscribe to commentAdded.
        // TODO: authentication is disabled.
        val executionResult =
            dgsQueryExecutor.execute("subscription onCommentAdded{ commentAdded{ id postId  content}}")
        val publisher = executionResult.getData<Publisher<ExecutionResult>>()

        val verifier = StepVerifier.create(publisher)
            .consumeNextWith {
                assertThat(
                    (it.getData<Map<String, Map<String, Any>>>()["commentAdded"]
                        ?.get("content") as String)
                ).isEqualTo("comment1")
            }
            .consumeNextWith {
                assertThat(
                    (it.getData<Map<String, Map<String, Any>>>()["commentAdded"]
                        ?.get("content") as String)
                ).isEqualTo("comment2")
            }
            .thenCancel()
            .verifyLater()

        val commentQuery =
            "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { id postId content}}"
        val comment1Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to "comment1"
            )
        )
        val comment2Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to "comment2"
            )
        )

        val comment1Response = client.executeQuery(commentQuery, comment1Variables)

        val comment1Result = comment1Response.extractValueAsObject("addComment", Comment::class.java)

        assertThat(comment1Result.content).isEqualTo("comment1")

        val comment2Response = client.executeQuery(commentQuery, comment2Variables)

        val comment2Result = comment2Response.extractValueAsObject("addComment", Comment::class.java)

        assertThat(comment2Result.content).isEqualTo("comment2")

        //verify it now.
        verifier.verify();
    }

    @TestConfiguration
    class TestConfig {

        @Bean
        fun restTemplate() = RestTemplate()
    }
}
