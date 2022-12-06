package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class IntegrationTests {

    WebSocketGraphQlClient client;

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var url = "ws://localhost:" + port + "/ws/graphql";
        log.debug("websocket connection url: {}", url);
        this.client = WebSocketGraphQlClient.builder(url, new ReactorNettyWebSocketClient()).build();
        this.client.start();
    }

    @AfterEach
    void tearDown() {
        this.client.stop();
    }

    @SneakyThrows
    @Test
    void createPostAndAddCommentAndSubscription() {
        //create post
        var createPostQuery = """
                mutation createPost($input: CreatePostInput!){
                    createPost(createPostInput:$input){
                        id
                        title
                        content
                    }
                }""".trim();

        Map<String, Object> createPostVariables = Map.of(
                "input", Map.of(
                        "title", "my post created by Spring GraphQL",
                        "content", "content of my post"
                )
        );

        var countDownLatch = new CountDownLatch(1);
        var postIdReference = new AtomicReference<String>();
        this.client.document(createPostQuery).variables(createPostVariables).execute()
                .map(response -> objectMapper.convertValue(
                        response.<Map<String, Object>>getData().get("createPost"),
                        Post.class)
                )
                .subscribe(post -> {
                    log.info("created post: {}", post);
                    postIdReference.set(post.getId());
                    countDownLatch.countDown();
                });

        countDownLatch.await(5, SECONDS);

        String postId = postIdReference.get();
        log.debug("created post id: {}", postId);
        assertThat(postId).isNotNull();

        var postById = """
                query post($postId:String!){
                   postById(postId:$postId) {
                     id
                     title
                     content
                   }
                 }""".trim();
        this.client.document(postById).variable("postId", postId)
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    var post = objectMapper.convertValue(
                            response.<Map<String, Object>>getData().get("postById"),
                            Post.class);
                    assertThat(post).isNotNull();
                    assertThat(post.getId()).isEqualTo(postId);
                    assertThat(post.getTitle()).isEqualTo("my post created by Spring GraphQL");
                    assertThat(post.getContent()).isEqualTo("content of my post");
                })
                .verifyComplete();


        var subscriptionQuery = "subscription onCommentAdded { commentAdded { id content } }";
        Flux<Comment> result = this.client.document(subscriptionQuery)
                .executeSubscription()
                .map(response -> objectMapper.convertValue(
                        response.<Map<String, Object>>getData().get("commentAdded"),
                        Comment.class)
                );

        var verify = StepVerifier.create(result)
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .thenCancel().verifyLater();

        addCommentToPost(postId);
        addCommentToPost(postId);
        addCommentToPost(postId);

        verify.verify();
    }

    private void addCommentToPost(String id) {
        var addCommentQuery = """
                mutation addComment($commentInput: CommentInput!){
                   addComment(commentInput:$commentInput){id}
                }""".trim();
        Map<String, Object> addCommentVariables = Map.of("commentInput",
                Map.of(
                        "postId", id,
                        "content", "comment of my post at " + LocalDateTime.now()
                )
        );
        client.document(addCommentQuery)
                .variables(addCommentVariables)
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    var comment = objectMapper.convertValue(
                            response.<Map<String, Object>>getData().get("addComment"),
                            Comment.class
                    );
                    assertThat(comment).isNotNull();
                    assertThat(comment.getId()).isNotNull();
                })
                .verifyComplete();
    }
}