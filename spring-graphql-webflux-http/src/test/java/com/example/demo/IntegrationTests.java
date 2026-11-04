package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Slf4j
class IntegrationTests {

    static final String TITLE = "my post created by Spring GraphQL";

    @LocalServerPort
    int port;

    HttpGraphQlClient client;

    @BeforeEach
    void setup() {
        this.client = HttpGraphQlClient.create(WebClient.create("http://localhost:" + port + "/graphql"));
    }

    @Test
    void createPostAndAddCommentOverSse() throws InterruptedException {
        var id = createPost();
        getPostById(id);

        var subscriptionQuery = "subscription onCommentAdded { commentAdded { id content } }";
        Flux<Comment> result = client.document(subscriptionQuery)
                .retrieveSubscription("commentAdded")
                .toEntity(Comment.class);

        var verifier = StepVerifier.create(result)
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .thenCancel()
                .verifyLater();

        // the subscription must be registered before the mutations emit events
        Thread.sleep(1000);
        addCommentToPost(id);
        addCommentToPost(id);
        addCommentToPost(id);

        verifier.verify(Duration.ofSeconds(10));
    }

    private String createPost() {
        var createPostMutation = """
                mutation createPost($input: CreatePostInput!){
                    createPost(createPostInput:$input){
                        id
                        title
                        content
                    }
                }""".stripIndent();

        var postId = new AtomicReference<String>();
        client.document(createPostMutation)
                .variables(Map.of("input", Map.of("title", TITLE, "content", "content of my post")))
                .retrieve("createPost")
                .toEntity(Post.class)
                .doOnNext(post -> {
                    log.info("created post: {}", post);
                    postId.set(post.getId());
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        assertThat(postId.get()).isNotNull();
        return postId.get();
    }

    private void getPostById(String id) {
        var postByIdQuery = """
                query post($postId:String!){
                   postById(postId:$postId) {
                     id
                     title
                     content
                   }
                 }""".stripIndent();

        client.document(postByIdQuery)
                .variable("postId", id)
                .retrieve("postById")
                .toEntity(Post.class)
                .as(StepVerifier::create)
                .consumeNextWith(post -> {
                    assertThat(post.getId()).isEqualTo(id);
                    assertThat(post.getTitle()).isEqualTo(TITLE);
                })
                .verifyComplete();
    }

    private void addCommentToPost(String id) {
        var addCommentMutation = """
                mutation addComment($commentInput: CommentInput!){
                   addComment(commentInput:$commentInput){id}
                }""".stripIndent();

        client.document(addCommentMutation)
                .variables(Map.of("commentInput", Map.of(
                        "postId", id,
                        "content", "comment of my post at " + LocalDateTime.now()
                )))
                .retrieve("addComment")
                .toEntity(Comment.class)
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getId()).isNotNull())
                .verifyComplete();
    }
}
