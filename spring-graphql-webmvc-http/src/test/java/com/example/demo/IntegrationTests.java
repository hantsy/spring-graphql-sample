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
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Slf4j
public class IntegrationTests {

    @LocalServerPort
    int port;

    HttpSyncGraphQlClient client;
    HttpGraphQlClient sseClient;

    @BeforeEach
    void setup() {
        var url = "http://localhost:" + port + "/graphql";
        this.client = HttpSyncGraphQlClient.create(RestClient.create(url));
        // SSE needs the reactive client: HttpSyncGraphQlClient.executeSubscription() always fails
        this.sseClient = HttpGraphQlClient.create(WebClient.create(url));
    }

    @Test
    public void createPostAndAddCommentOverSse() throws InterruptedException {
        var id = createPost();
        getPostById(id);

        var subscriptionQuery = "subscription onCommentAdded { commentAdded { id content } }";
        Flux<Comment> result = sseClient.document(subscriptionQuery)
                .retrieveSubscription("commentAdded")
                .toEntity(Comment.class);

        var verifier = StepVerifier.create(result)
                .thenAwait(Duration.ofMillis(1000))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .thenCancel()
                .verifyLater();

        // the SSE subscription must be registered before the mutations emit, otherwise the
        // replay().latest() sink only keeps the last comment
        Thread.sleep(1000);

        addCommentToPost(id);
        addCommentToPost(id);
        addCommentToPost(id);

        await().atMost(5000, MILLISECONDS).untilAsserted(verifier::verify);
    }

    private String createPost() {
        var createPostMutation = """
                mutation createPost($createPostInput: CreatePostInput!){
                   createPost(createPostInput:$createPostInput){
                       id
                       title
                       content
                   }
                }""".stripIndent();

        var post = client.document(createPostMutation)
                .variable("createPostInput", Map.of(
                        "title", "SpringGraphQL",
                        "content", "content of my post"
                ))
                .retrieveSync("createPost")
                .toEntity(Post.class);

        log.info("created post: {}", post);
        assertThat(post.getId()).isNotNull();
        return post.getId();
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

        var post = client.document(postByIdQuery)
                .variable("postId", id)
                .retrieveSync("postById")
                .toEntity(Post.class);

        log.debug("postById: {}", post);
        assertThat(post.getTitle()).isEqualTo("SpringGraphQL");
    }

    private void addCommentToPost(String id) {
        var addCommentMutation = """
                mutation addComment($commentInput: CommentInput!){
                    addComment(commentInput:$commentInput){
                        id
                    }
                }""".stripIndent();

        var comment = client.document(addCommentMutation)
                .variable("commentInput", Map.of(
                        "postId", id,
                        "content", "comment of my post at " + LocalDateTime.now()
                ))
                .retrieveSync("addComment")
                .toEntity(Comment.class);

        log.debug("added comment: {}", comment);
        assertThat(comment.getId()).isNotNull();
    }
}
