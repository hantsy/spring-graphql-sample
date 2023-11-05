package com.example.demo;

import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class SseSubscriptionTests {

    @LocalServerPort
    int port;

    WebTestClient webClient;

    @Autowired
    PostService postService;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.webClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @SneakyThrows
    @Test
    public void testAddComment() {
        // there are 4 posts initialized.
        String postId = this.postService.getAllPosts().get(0).getId();
        log.debug("post id: {}", postId);
        // add comment
        var addCommentQuery = """
                mutation addNewComment($postId:String!, $content:String!){
                    addComment(postId:$postId, content:$content){
                        id
                        postId
                        content
                    }
                }""".trim();
        var addCommentVariables = Map.of(
                "postId", postId,
                "content", "test comment"
        );
        Map<String, Object> addCommentBody = Map.of("query", addCommentQuery, "variables", addCommentVariables);
        webClient.post().uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(addCommentBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("data.addComment.id").exists()
                .jsonPath("data.addComment.content").isEqualTo("test comment");

        verifySseSubscription();
    }

    private void verifySseSubscription() {
        //handle subscription to /graphql http SSE endpoints
        Map<String, List<String>> queryPayload = Map.of("query", List.of("subscription onCommentAdded { commentAdded { id postId content } }"));

        var client = WebClient.builder().baseUrl("http://localhost:" + port).clientConnector(new ReactorClientHttpConnector()).build();
        client.get().uri(uriBuilder -> uriBuilder.path("/graphql").queryParams(new MultiValueMapAdapter<>(queryPayload)).build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchangeToFlux(clientResponse -> clientResponse.bodyToFlux(String.class))
                .as(StepVerifier::create)
                .consumeNextWith(it -> {
                    log.debug("response from sse endpoint: {}", it);
                    String content = JsonPath.read(it, "payload.data.commentAdded.content");
                    assertThat(content).isEqualTo("test comment");
                })
                .thenCancel()
                .verify();
    }
}
