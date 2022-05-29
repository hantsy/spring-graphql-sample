package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class SubscriptionTestsWithGraphQLClient {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    HttpGraphQlClient client;
    WebSocketGraphQlClient socketClient;

    @BeforeEach
    void setup() {
        this.client = HttpGraphQlClient.create(WebClient.create("http://localhost:" + port + "/graphql"));
        this.socketClient = WebSocketGraphQlClient.create(URI.create("ws://localhost:" + port + "/graphql"), new ReactorNettyWebSocketClient());
    }

    @SneakyThrows
    @Test
    public void createPostAndAddComment() {
        var creatPost = """
                mutation createPost($createPostInput: CreatePostInput!){
                   createPost(createPostInput:$createPostInput){
                   id
                   title
                   content
                   }
                }""".trim();

        String TITLE = "my post created by Spring GraphQL";
        var postIdHolder = new PostIdHolder();
        var countDownLatch = new CountDownLatch(1);

        client.document(creatPost)
                .variable("createPostInput",
                        Map.of(
                                "title", TITLE,
                                "content", "content of my post"
                        ))
                .execute()
                .map(reponse -> objectMapper.convertValue(
                        reponse.<Map<String, Object>>getData().get("createPost"),
                        Post.class)

                )
                .doOnTerminate(countDownLatch::countDown)
                .subscribe(post -> {
                    log.debug("created post: {}", post);
                    postIdHolder.setId(post.getId());
                });

        countDownLatch.await(5, SECONDS);

        String id = postIdHolder.getId();
        log.info("created post id: {}", id);
        assertThat(id).isNotNull();

        var postByIdQuery = """
                query post($postId:String!){
                   postById(postId:$postId) {
                     id
                     title
                     content
                   }
                 }""".trim();

        Map<String, Object> postByIdVariables = Map.of(
                "postId", id
        );
        client.document(postByIdQuery)
                .variables(postByIdVariables)
                .execute()
                .map(response -> objectMapper
                        .convertValue(
                                response.<Map<String, Object>>getData().get("postById"),
                                Post.class
                        )
                )
                .as(StepVerifier::create)
                .consumeNextWith(post -> {
                    log.debug("postById: {}", post);
                    assertThat(post.getTitle()).isEqualTo(TITLE);
                })
                .verifyComplete();


        var subscriptionQuery = "subscription onCommentAdded { commentAdded { id content } }";
        Flux<Comment> result = socketClient.document(subscriptionQuery)
                .executeSubscription()
                .map(response -> objectMapper.convertValue(
                        response.<Map<String, Object>>getData().get("commentAdded"),
                        Comment.class
                ));

        var verify = StepVerifier.create(result)
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .thenCancel().verifyLater();

        addCommentToPost(id);
        addCommentToPost(id);
        addCommentToPost(id);

        verify.verify();
    }

    private void addCommentToPost(String id) {
        var addCommentQuery = """
                mutation addComment($commentInput: CommentInput!){
                   addComment(commentInput:$commentInput){id}
                }""";
        Map<String, Object> addCommentVariables = Map.of(
                "commentInput",
                Map.of(
                        "content", "comment of my post",
                        "postId", id
                )
        );

        client.document(addCommentQuery)
                .variables(addCommentVariables)
                .execute()
                .map(response -> objectMapper.convertValue(
                        response.<Map<String, Object>>getData().get("addComment"),
                        Comment.class
                ))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> {
                    log.debug("added comment: {}", comment);
                    assertThat(comment.getId()).isNotNull();
                })
                .verifyComplete();
    }

}

class PostIdHolder {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}