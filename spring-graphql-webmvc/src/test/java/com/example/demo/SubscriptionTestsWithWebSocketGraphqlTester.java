package com.example.demo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.test.tester.WebSocketGraphQlTester;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class SubscriptionTestsWithWebSocketGraphqlTester {

    WebSocketGraphQlTester graphQlTester;

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        this.graphQlTester = WebSocketGraphQlTester.create(URI.create("ws://localhost:" + port + "/subscriptions"), new ReactorNettyWebSocketClient());
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
        String id = graphQlTester.document(creatPost)
                .variable("createPostInput",
                        Map.of(
                                "title", TITLE,
                                "content", "content of my post"
                        ))
                .execute()
                .path("createPost.id").entity(String.class).get();

        log.info("created post: {}", id);
        assertThat(id).isNotNull();

        var postById = """
                query post($postId:String!){
                   postById(postId:$postId) {
                     id
                     title
                     content
                   }
                 }""".stripIndent();
        graphQlTester.document(postById).variable("postId", id.toString())
                .execute()
                .path("postById.title")
                .entity(String.class)
                .satisfies(titles -> assertThat(titles).isEqualTo(TITLE));


        String subscriptionQuery = """
                subscription onCommentAdded{
                    commentAdded{
                        id 
                        content 
                    } 
                }
                """.stripIndent();
        var subscribedCommentsFlux = this.graphQlTester.document(subscriptionQuery)
                .executeSubscription()
                .toFlux("commentAdded.content", String.class);

        var verifier = StepVerifier.create(subscribedCommentsFlux)
                .thenAwait(Duration.ofMillis(1000))
                .consumeNextWith(c -> assertThat(c).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c).startsWith("comment of my post at "))
                .thenCancel()
                .verifyLater();

        addCommentToPost(id);
        addCommentToPost(id);
        addCommentToPost(id);

        Awaitility.await().atMost(Duration.ofMillis(500)).untilAsserted(verifier::verify);
    }

    private void addCommentToPost(String id) {
        var addComment = """
                mutation addComment($commentInput: CommentInput!){
                   addComment(commentInput:$commentInput){id}
                }""";

        String commentId = graphQlTester.document(addComment)
                .variable("commentInput",
                        Map.of(
                                "postId", id,
                                "content", "comment of my post at " + LocalDateTime.now()
                        ))
                .execute()
                .path("addComment.id")
                .entity(String.class).get();

        log.info("added comment of post: {}", commentId);
        assertThat(commentId).isNotNull();
    }

}
