package com.example.demo

import com.example.demo.gql.types.AuthResult
import com.example.demo.gql.types.Post
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [TestConfigInitializer::class])
class MutationTests {
    private val log = LoggerFactory.getLogger(MutationTests::class.java)

    @LocalServerPort
    var port: Int = 8080

    var restTemplate: TestRestTemplate? = null

    @BeforeEach
    fun setup() {
        restTemplate = TestRestTemplate(
            RestTemplateBuilder()
                .defaultMessageConverters()
                .rootUri("http://localhost:$port")
        )
    }

    @Test
    fun `create new post without auth`() {

        val requestData = mapOf<String, Any>(
            "query" to "mutation createPost(\$input: CreatePostInput!){ createPost(createPostInput:\$input) {id, title} }",
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
        )

        val requestEntity = RequestEntity.post("graphql")
            .accept(MediaType.APPLICATION_JSON)
            // .headers { it.add("X-Auth-Token", token) }
            .body(requestData)

        val responseEntity = restTemplate!!.exchange(
            requestEntity,
            object : ParameterizedTypeReference<HashMap<String, List<Any>>>() {}
            // object : ParameterizedTypeReference<HashMap<String, List<TypedGraphQLError>>>() {}
        )

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        val errors = responseEntity.body!!["errors"]
        log.debug("errors: {}", errors)
        // it prints out:
        // errors: [{message=org.springframework.security.access.AccessDeniedException: Access is denied,
        // locations=[], path=[createPost], extensions={errorType=PERMISSION_DENIED}}]
        assertThat(errors).isNotEmpty
        // if pr https://github.com/Netflix/dgs-framework/pull/556 is merged.
        //assertThat(errors!![0].extensions["errorType"]).isEqualTo("PERMISSION_DENIED")
    }

    // 1. sign in with user/password
    // 2. extract the token and put to http header and send request to create a post
    @Test
    fun `create new post`() {

        val signinData = mapOf<String, Any>(
            "query" to "mutation signIn(\$input: Credentials!){ signIn(credentials:\$input) {name, roles, token} }",
            "variables" to mapOf(
                "input" to mapOf(
                    "username" to "user",
                    "password" to "password"
                )
            )
        )

        val signinEntity = RequestEntity.post("graphql")
            .accept(MediaType.APPLICATION_JSON)
            .body(signinData)

        val signinResponseEntity = restTemplate!!.exchange(
            signinEntity,
            object : ParameterizedTypeReference<HashMap<String, HashMap<String, AuthResult>>>() {}
        )

        assertThat(signinResponseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(signinResponseEntity.body!!["data"]!!["signIn"]!!.name).isEqualTo("user")
        assertThat(signinResponseEntity.body!!["data"]!!["signIn"]!!.roles).contains("ROLE_USER")

        val token = signinResponseEntity.body!!["data"]!!["signIn"]!!.token

        assertThat(token).isNotNull

        val requestData = mapOf<String, Any>(
            "query" to "mutation createPost(\$input: CreatePostInput!){ createPost(createPostInput:\$input) {id, title} }",
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
        )

        val requestEntity = RequestEntity.post("graphql")
            .accept(MediaType.APPLICATION_JSON)
            .headers { it.add("X-Auth-Token", token) }
            .body(requestData)

        val responseEntity = restTemplate!!.exchange(
            requestEntity,
            object : ParameterizedTypeReference<HashMap<String, HashMap<String, Post>>>() {})

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        val post = responseEntity.body!!["data"]!!["createPost"]
        assertThat(post).isNotNull
        assertThat(post!!.id).isNotNull
        assertThat(post.title).isEqualTo("test title")
    }
}