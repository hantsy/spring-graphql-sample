package com.example.demo;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MutationTests {

    @LocalServerPort
    int port;

    WebTestClient webClient;

    @BeforeEach
    void setUp() {
        this.webClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @SneakyThrows
    @Test
    public void testCreatePost() {
        var query = """
                mutation createPost($title:String!, $content:String){
                    createPost(title:$title, content:$content){
                        id
                        title
                        content
                    }
                }
                """.trim();
        var variables = Map.of(
                "title", "test title",
                "content", "test content"
        );
        Map<String, Object> body = Map.of("query", query, "variables", variables);
        webClient.post().uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("data.createPost.id").exists()
                .jsonPath("data.createPost.content").isEqualTo("test content")
                .jsonPath("data.createPost.title").isEqualTo("test title");

    }
}
