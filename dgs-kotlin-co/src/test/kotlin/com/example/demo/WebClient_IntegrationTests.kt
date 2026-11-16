package com.example.demo

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Transport level behaviour of the GraphQL endpoint, exercised with the plain
 * `WebTestClient`, without any GraphQL client library.
 * See `IntegrationTests` for the same behaviour seen through the GraphQL client libraries.
 */
@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestcontainersConfiguration::class)
class WebClient_IntegrationTests {

    companion object {
        private const val CREATE_POST_MUTATION = """
            mutation createPost(${'$'}input: CreatePostInput!){
                createPost(createPostInput:${'$'}input){
                    id
                    title
                }
            }
        """

        private const val POST_BY_ID_QUERY = """
            query postById(${'$'}postId: UUID!) {
                postById(postId: ${'$'}postId) {
                    id
                    title
                }
            }
        """
    }

    @LocalServerPort
    var port: Int = 8080

    lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setup() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port/graphql")
            .defaultHeaders { it.setBasicAuth("user", "password") }
            .build()
    }

    @Test
    fun `create a post and query it back`() {
        val createBody = webTestClient.post()
            .bodyValue(
                mapOf(
                    "query" to CREATE_POST_MUTATION,
                    "variables" to mapOf(
                        "input" to mapOf(
                            "title" to "test title",
                            "content" to "test content"
                        )
                    )
                )
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, Any>>() {})
            .returnResult().responseBody!!

        @Suppress("UNCHECKED_CAST")
        val created = (createBody["data"] as Map<String, Any>)["createPost"] as Map<String, Any>
        created["title"] shouldBe "test title"
        val postId = created["id"] as String
        postId.isNotBlank() shouldBe true

        // query the created post back by its id - `allPosts` is not ordered, so the new post
        // cannot be addressed by its position in the list.
        val queryBody = webTestClient.post()
            .bodyValue(mapOf("query" to POST_BY_ID_QUERY, "variables" to mapOf("postId" to postId)))
            .exchange()
            .expectStatus().isOk()
            .expectBody(object : ParameterizedTypeReference<HashMap<String, Any>>() {})
            .returnResult().responseBody!!

        @Suppress("UNCHECKED_CAST")
        val post = (queryBody["data"] as Map<String, Any>)["postById"] as Map<String, Any>
        post["id"] shouldBe postId
        post["title"] shouldBe "test title"
    }
}
