package com.example.demo;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static java.util.Collections.emptyMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DemoApplicationTests {

    @LocalServerPort
    int port;

    WebTestClient webClient;

    @BeforeEach
    void setUp() {
        this.webClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @SneakyThrows
    @Test
    public void testGetAllPosts() {
        var query = """
                query {
                  allPosts{
                    id
                    title
                    content
                    comments{content}
                    author{email}
                  }
                }""".trim();
        Map<String, Object> body = Map.of("query", query, "variables", emptyMap());
        webClient.post().uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("data.allPosts.length()").isEqualTo(4);

    }
}
