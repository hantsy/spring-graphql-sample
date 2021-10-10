package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.boot.test.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@AutoConfigureGraphQlTester
@AutoConfigureWebTestClient
class IntegrationTests {

    @Autowired
    WebGraphQlTester graphQlTester;

    @Test
    void createPostAndAddCommentAndSubscription() {
        //create post
        var createPostQuery = "mutation createPost($input: CreatePostInput!){createPost(createPostInput:$input){id, title, content}}";
        var postId = graphQlTester.query(createPostQuery)
                .variable("input", Map.of(
                        "title", "test title",
                        "content", "test content"
                ))
                .execute()
                .path("data.createPost.title").entity(String.class).isEqualTo("test title")
                .path("data.createPost.id").entity(String.class).get();
        log.debug("saved post id: {}", postId);
        assertThat(postId).isNotNull();

        // subscribe commentAdded event
        String subscriptionQuery = "subscription onCommentAdded { commentAdded { id postId content } }";
        var verifier = graphQlTester.query(subscriptionQuery)
                .executeSubscription()
                .toFlux("commentAdded.content", String.class)
                .as(StepVerifier::create)
                .expectNext("test comment")
                .thenCancel()
                .verifyLater();

        // add comment
        var addCommentQuery = "mutation addComment($input: CommentInput!){addComment(commentInput:$input){id, content}}";
        graphQlTester.query(createPostQuery)
                .variable("input", Map.of(
                        "postId", postId,
                        "content", "test comment"
                ))
                .execute()
                .path("data.addComment.content").entity(String.class).isEqualTo("test comment");

        // verify the subscription now.
        verifier.verify();
    }
}
