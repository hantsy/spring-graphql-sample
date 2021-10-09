package com.example.demo;

import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.*;

@SpringBootTest
class QueryMockTests {

    @Autowired
    WebApplicationContext appContext;

    WebTestClient webClient;

    @MockBean
    PostService postService;

    @BeforeEach
    void setUp() {
        this.webClient = MockMvcWebTestClient.bindToApplicationContext(appContext).build();
    }

    @SneakyThrows
    @Test
    public void testGetAllPosts() {
        when(this.postService.getAllPosts()).thenReturn(
                List.of(
                        Post.builder().id(UUID.randomUUID().toString()).title("test post").content("test content").build()
                )
        );

        var query = """
                query {
                  allPosts{
                    id
                    title
                    content
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
                .jsonPath("data.allPosts[0].title").isEqualTo("test post")
                .jsonPath("data.allPosts.length()").isEqualTo(1);

        verify(this.postService, times(1)).getAllPosts();
        verifyNoMoreInteractions(this.postService);
    }
}
