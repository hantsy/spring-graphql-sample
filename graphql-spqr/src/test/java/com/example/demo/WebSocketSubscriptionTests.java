package com.example.demo;

import com.example.demo.service.PostService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@Disabled
class WebSocketSubscriptionTests {

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


        //handle subscription to /graphql websocket endpoints
        Map<String, Object> queryPayload = Map.of(
                "query", "subscription onCommentAdded { commentAdded { id postId content } }",
                "extensions", emptyMap(),
                "variables", emptyMap());
        var body = Map.of(
                "payload", queryPayload,
                "type", "start",
                "id", "1"
        );

        var commentsReplay = new ArrayList<String>();
        var socketClient = new ReactorNettyWebSocketClient();
        WebSocketHandler socketHandler = session -> {
            Mono<Void> receiveMono = session.receive().doOnNext(
                    it -> {
                        log.debug("next item: {}", it);
//                        String text = it.getPayloadAsText();
//                        log.debug("receiving message as text: {}", text);
//                        if ("data".equals(JsonPath.read(text, "type"))) {
//                            String comment = JsonPath.read(text, "payload.data.commentAdded.content");
//                            commentsReplay.add(comment);
//                        }
                    }
            ).log().then();

            String message = null;
            try {
                message = objectMapper.writeValueAsString(body);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return session
                    .send(Mono.delay(Duration.ofMillis(100)).thenMany(Flux.just(message).map(session::textMessage)))
                    .log()
                    .then(receiveMono);
        };

        MultiValueMapAdapter<String, String> queryParams = new MultiValueMapAdapter<>(Map.of("query", List.<String>of("subscription onCommentAdded { commentAdded { id postId content } }")));

        URI uri = new DefaultUriBuilderFactory("ws://localhost:" + port + "/graphql").builder().queryParams(queryParams).build();
        socketClient.execute(uri, socketHandler).block(Duration.ofMillis(500));

        assertThat(commentsReplay.size()).isEqualTo(1);
        assertThat(commentsReplay.get(0)).isEqualTo("test comment");
    }
}
