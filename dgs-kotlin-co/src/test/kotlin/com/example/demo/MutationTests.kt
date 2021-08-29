package com.example.demo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MutationTests {

    lateinit var client: WebTestClient

    @LocalServerPort
    var port: Int = 8080

    @BeforeEach
    fun beforeAll() {
        client = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            // .filter(ExchangeFilterFunctions.basicAuthentication("user", "password"))
            .build()
    }

    @Test
    fun `create new post`() {
        val requestData = mapOf<String, Any>(
            "query" to "mutation createPost(\$input: CreatePostInput!){ createPost(createPostInput:\$input) {id, title} }",
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
        )

        this.client
            .mutate().filter(ExchangeFilterFunctions.basicAuthentication("user", "password")).build()
            .post().uri("/graphql")//.headers { it.setBasicAuth("user", "password") }
            .bodyValue(requestData)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody()
            .jsonPath("data.createPost.id").exists()
            .jsonPath("data.createPost.title").isEqualTo("test title")
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
        this.client
            .post().uri("/graphql")// no auth headers
            .bodyValue(requestData)
            .exchange()
            .expectStatus().is2xxSuccessful
            //
            // security is disabled.
            //.expectBody()
            //.jsonPath("errors.length()").value<Int> { assertThat(it).isGreaterThan(0) }

        // it is an INTERNAL errorType
        //.jsonPath("errors[0].extensions.errorType").isEqualTo("PERMISSION_DENIED")
    }


}


