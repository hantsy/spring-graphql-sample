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
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = TestDemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class IntegrationTests {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    HttpGraphQlClient client;
    WebSocketGraphQlClient socketClient;

    @BeforeEach
    void setup() {
        this.client = HttpGraphQlClient.create(WebClient.create("http://localhost:" + port + "/graphql"));
        this.socketClient = WebSocketGraphQlClient.create(URI.create("ws://localhost:" + port + "/subscriptions"), new ReactorNettyWebSocketClient());
        this.socketClient.start();
    }

    @AfterEach
    void tearDown() {
        this.socketClient.stop();
    }

    @SneakyThrows
    @Test
    public void createPostAndAddComment() {

        // create a post
        var id = createPost();

        // get post by id
        getPostById(id);


        var subscriptionQuery = "subscription onCommentAdded { commentAdded { id content } }";
        var comments = new CopyOnWriteArrayList<Comment>();
        socketClient.document(subscriptionQuery)
                .executeSubscription()
                .map(response -> objectMapper.convertValue(
                        response.<Map<String, Object>>getData().get("commentAdded"),
                        Comment.class
                ))
                .doOnNext(it -> log.debug("received CommentAdded: {}", it))
                .subscribe(comments::add);

        addCommentToPost(id, "comment1");
        addCommentToPost(id, "comment2");
        addCommentToPost(id, "comment3");

        await().atMost(500, MILLISECONDS)
                .untilAsserted(
                        () -> assertThat(comments.size()).isEqualTo(3)
                );
    }

    private void getPostById(String id) {
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
                    assertThat(post.getTitle()).isEqualTo("SpringGraphQL");
                })
                .verifyComplete();
    }

    @SneakyThrows
    private String createPost() {
        var creatPost = """
                mutation createPost($createPostInput: CreatePostInput!){
                   createPost(createPostInput:$createPostInput){
                   id
                   title
                   content
                   }
                }""".trim();

        var postIdHolder = new AtomicReference<String>();
        var countDownLatch = new CountDownLatch(1);

        client.document(creatPost)
                .variable("createPostInput",
                        Map.of(
                                "title", "SpringGraphQL",
                                "content", "content of my post"
                        ))
                .execute()
                .map(response -> objectMapper.convertValue(
                        response.<Map<String, Object>>getData().get("createPost"),
                        Post.class)

                )
                //.doOnTerminate(countDownLatch::countDown)
                .subscribe(post -> {
                    log.debug("created post: {}", post);
                    postIdHolder.set(post.getId());
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        String id = postIdHolder.get();
        log.info("created post id: {}", id);
        assertThat(id).isNotNull();

        return id;
    }

    private void addCommentToPost(String id, String comment) {
        var addCommentQuery = """
                mutation addComment($commentInput: CommentInput!){
                   addComment(commentInput:$commentInput){id}
                }""";
        Map<String, Object> addCommentVariables = Map.of(
                "commentInput",
                Map.of(
                        "content", comment,
                        "postId", id
                )
        );

        client.document(addCommentQuery)
                .variables(addCommentVariables)
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    var addedComment = objectMapper.convertValue(
                            response.<Map<String, Object>>getData().get("addComment"),
                            Comment.class
                    );
                    log.debug("added comment: {}", addedComment);
                    assertThat(addedComment).isNotNull();
                    assertThat(addedComment.getId()).isNotNull();
                })
                .verifyComplete();
    }

}