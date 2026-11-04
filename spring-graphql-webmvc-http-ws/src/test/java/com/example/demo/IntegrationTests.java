package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
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

    @Autowired
    JsonMapper jsonMapper;

    HttpSyncGraphQlClient client;
    WebSocketGraphQlClient socketClient;

    @BeforeEach
    void setup() {
        // RestClient is the servlet-stack flavour; HttpGraphQlClient.create(WebClient.create(url)) is the reactive one
        this.client = HttpSyncGraphQlClient.create(RestClient.create("http://localhost:" + port + "/graphql"));
        this.socketClient = WebSocketGraphQlClient.create(
                URI.create("ws://localhost:" + port + "/subscriptions"), new ReactorNettyWebSocketClient());
        this.socketClient.start();
    }

    @AfterEach
    void tearDown() {
        this.socketClient.stop();
    }

    @Test
    public void createPostAndAddComment() throws InterruptedException {
        var id = createPost();
        getPostById(id);

        var subscriptionQuery = "subscription onCommentAdded { commentAdded { id content } }";
        Flux<Comment> result = this.socketClient.document(subscriptionQuery)
                .executeSubscription()
                .map(response -> {
                    Comment comment = jsonMapper.convertValue(
                            response.<Map<String, Object>>getData().get("commentAdded"), Comment.class);
                    log.debug("received comment: {}", comment);
                    return comment;
                });

        var verifier = StepVerifier.create(result)
                .thenAwait(Duration.ofMillis(1000))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                .thenCancel()
                .verifyLater();

        // give the subscription time to register: the server sink is replay().latest(), so a late
        // subscriber would only see the last comment
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
