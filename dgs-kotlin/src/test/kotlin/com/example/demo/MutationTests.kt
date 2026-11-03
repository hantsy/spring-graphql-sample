package com.example.demo

import com.example.demo.gql.types.AuthResult
import com.example.demo.gql.types.Post
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestcontainersConfiguration::class)
@ContextConfiguration(initializers = [TestConfigInitializer::class])
class MutationTests {
    private val log = LoggerFactory.getLogger(MutationTests::class.java)

    @LocalServerPort
    var port: Int = 8080

    lateinit var restTestClient: RestTestClient

    @BeforeEach
    fun setup() {
        restTestClient = RestTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `create new post without auth`() {

        val query = """
            mutation createPost(${'$'}input: CreatePostInput!){ 
                createPost(createPostInput:${'$'}input) {
                    id
                    title
                }
            }
        """.trimIndent()
        val requestData = mapOf(
            "query" to query,
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
        )

        val body = restTestClient.post().uri("graphql")
            .accept(MediaType.APPLICATION_JSON)
            // .header("X-Auth-Token", token)
            .body(requestData)
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, List<Any>>>() {})
            .returnResult().getResponseBody()!!

        val errors = body["errors"]
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

        val query = """
            mutation signIn(${'$'}input: Credentials!){
                signIn(credentials:${'$'}input) {
                    name
                    roles
                    token
                } 
            }
        """.trimIndent()
        val signinData = mapOf(
            "query" to query,
            "variables" to mapOf(
                "input" to mapOf(
                    "username" to "user",
                    "password" to "password"
                )
            )
        )

        val signinBody = restTestClient.post().uri("graphql")
            .accept(MediaType.APPLICATION_JSON)
            .body(signinData)
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, HashMap<String, AuthResult>>>() {})
            .returnResult().getResponseBody()!!

        assertThat(signinBody["data"]!!["signIn"]!!.name).isEqualTo("user")
        assertThat(signinBody["data"]!!["signIn"]!!.roles).contains("ROLE_USER")

        val token = signinBody["data"]!!["signIn"]!!.token

        assertThat(token).isNotNull

        val queryOfCreatingPost = """
            mutation createPost(${'$'}input: CreatePostInput!){ 
                createPost(createPostInput:${'$'}input) {
                    id, 
                    title
                } 
            }
        """.trimIndent()
        val requestData = mapOf(
            "query" to queryOfCreatingPost,
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
        )

        val body = restTestClient.post().uri("graphql")
            .accept(MediaType.APPLICATION_JSON)
            .header("X-Auth-Token", token)
            .body(requestData)
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, HashMap<String, Post>>>() {})
            .returnResult().getResponseBody()!!

        val post = body["data"]!!["createPost"]
        assertThat(post).isNotNull
        assertThat(post!!.id).isNotNull
        assertThat(post.title).isEqualTo("test title")
    }
}