package com.example.demo

import com.example.demo.gql.types.Post
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import reactor.kotlin.test.test

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
            .expectBody()
            .jsonPath("errors.length()").value<Int> { assertThat(it).isGreaterThan(0) }
    }


}


