package com.example.demo

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

	@LocalServerPort
	var port: Int = 0

	lateinit var client: WebTestClient

	@BeforeEach
	fun setup() {
		this.client = WebTestClient.bindToServer()
			.baseUrl("http://localhost:$port/graphql")
			.build()
	}
	@Test
	fun `fetching all posts`() {
		val body = mapOf(
			"query" to """
				query {
				  allPosts{ 
				  	id
				    title
					content
					comments { content}
					author {email}
				  }
				}
			""".trimIndent(),
			"variables" to emptyMap<String, String>()
		)
		client.post()
			.bodyValue(body)
			.exchange()
			.expectStatus().isOk
			.expectBody().jsonPath("$.data").isNotEmpty
			.jsonPath("$.data.allPosts[0].title").isEqualTo("LEARN SPRING")
			.jsonPath("$.errors").doesNotExist()
	}

}
