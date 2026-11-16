package com.example.demo

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

/**
 * Transport level behaviour of the GraphQL endpoint, exercised with the plain
 * `RestTestClient` - no GraphQL client library in between.
 * See `IntegrationTests` for the same behaviour seen through the GraphQL client libraries.
 */
@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestcontainersConfiguration::class)
class RestClient_IntegrationTests {

    companion object {
        private const val SIGN_IN_MUTATION = """
            mutation signIn(${'$'}input: Credentials!){
                signIn(credentials:${'$'}input) {
                    name
                    roles
                    token
                }
            }
        """

        private const val CREATE_POST_MUTATION = """
            mutation createPost(${'$'}input: CreatePostInput!){
                createPost(createPostInput:${'$'}input){
                    id
                    title
                }
            }
        """

        private val CREATE_POST_VARIABLES = mapOf(
            "input" to mapOf(
                "title" to "test title",
                "content" to "test content"
            )
        )
    }

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
    fun `create a post without auth should return errors PERMISSION_DENIED`() {
        val body = post(CREATE_POST_MUTATION, CREATE_POST_VARIABLES)

        val errors = body["errors"] as List<*>?
        errors shouldNotBe null
        errors!!.size shouldBe 1

        // Over HTTP the request goes through the Spring Security filter chain, which puts an
        // anonymous `Authentication` into the `SecurityContext`. Method security (`@Secured`) then
        // rejects the call with an `AccessDeniedException`, which DGS maps to PERMISSION_DENIED.
        @Suppress("UNCHECKED_CAST")
        val extensions = (errors[0] as Map<String, Any>)["extensions"] as Map<String, Any>
        extensions["errorType"] shouldBe "PERMISSION_DENIED"
    }

    @Test
    fun `sign in and create a post`() {
        val signInBody = post(
            SIGN_IN_MUTATION,
            mapOf(
                "input" to mapOf(
                    "username" to "user",
                    "password" to "password"
                )
            )
        )

        @Suppress("UNCHECKED_CAST")
        val data = signInBody["data"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val signIn = data["signIn"] as Map<String, Any>
        signIn["name"] shouldBe "user"
        @Suppress("UNCHECKED_CAST")
        (signIn["roles"] as List<String>) shouldContainExactly listOf("ROLE_USER")

        val token = signIn["token"] as String

        // the session id is passed back in the `X-Auth-Token` header.
        val body = restTestClient.post().uri("graphql")
            .accept(MediaType.APPLICATION_JSON)
            .header("X-Auth-Token", token)
            .body(mapOf("query" to CREATE_POST_MUTATION, "variables" to CREATE_POST_VARIABLES))
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, Any>>() {})
            .returnResult().getResponseBody()!!

        body["errors"] shouldBe null
        @Suppress("UNCHECKED_CAST")
        val postData = body["data"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val created = postData["createPost"] as Map<String, Any>
        created["id"] shouldNotBe null
        created["title"] shouldBe "test title"
    }

    private fun post(query: String, variables: Map<String, Any>): HashMap<String, Any> =
        restTestClient.post().uri("graphql")
            .accept(MediaType.APPLICATION_JSON)
            .body(mapOf("query" to query, "variables" to variables))
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, Any>>() {})
            .returnResult().getResponseBody()!!
}
