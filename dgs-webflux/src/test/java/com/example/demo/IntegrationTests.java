package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.client.DgsGraphqlSSESubscriptionGraphQLClient;
import com.netflix.graphql.dgs.client.DgsWebClientGraphQLClient;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Slf4j
//@Disabled
//see: https://github.com/Netflix/dgs-framework/issues/689
class IntegrationTests {

    private static final String SSE_COMMENT = "sse test comment";

    private static final String HTTP_COMMENT = "http test comment";

    @LocalServerPort
    int port;

    // DgsWebClientGraphQLClient is the Jackson 3 variant of the Jackson 2 based WebClientGraphQLClient
    private DgsWebClientGraphQLClient client;

    private WebSocketGraphQlClient socketClient;

    private DgsGraphqlSSESubscriptionGraphQLClient sseClient;

    @BeforeEach
    public void setup() {
        var baseUrl = "http://localhost:" + port + "/graphql";
        var webClient = WebClient.create(baseUrl);
        this.client = new DgsWebClientGraphQLClient(webClient);
        // the Server-Sent-Events transport is served under the same path as the HTTP endpoint.
        this.sseClient = new DgsGraphqlSSESubscriptionGraphQLClient(baseUrl, webClient);
        this.socketClient = WebSocketGraphQlClient.create(URI.create("ws://localhost:" + port + "/graphql"), new ReactorNettyWebSocketClient());
        this.socketClient.start().subscribe();
    }

    @AfterEach
    public void teardown() {
        this.socketClient.stop().subscribe();
    }

    /**
     * The `createPost` mutation over plain HTTP.
     */
    @Test
    void createPostOverHttp() {
        Long postId = createPost("http mutation title", "http mutation content");

        assertThat(postId).isNotNull();
    }

    /**
     * The `addComment` mutation over plain HTTP.
     */
    @Test
    void addCommentOverHttp() {
        Long postId = createPost("http comment title", "http comment content");

        @Language("graphql") var addCommentQuery = """
                mutation addComment($input: CommentInput!) {
                    addComment(commentInput:$input) {
                        id
                        postId
                        content
                    }
                }
                """.stripIndent();

        var addCommentVariables = Map.of(
                "input", Map.of(
                        "postId", String.valueOf(postId),
                        "content", HTTP_COMMENT
                )
        );

        var comment = this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .block(Duration.ofSeconds(30));

        assertThat(comment).isNotNull();
        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getPostId()).isEqualTo(postId);
        assertThat(comment.getContent()).isEqualTo(HTTP_COMMENT);
    }

    /**
     * The `allPosts` query over plain HTTP.
     */
    @Test
    void queryAllPostsOverHttp() {
        Long postId = createPost("http query title", "http query content");

        var response = this.client.reactiveExecuteQuery("{ allPosts { id title content } }", Map.of())
                .block(Duration.ofSeconds(30));

        assertThat(response).isNotNull();
        assertThat(response.hasErrors()).isFalse();
        List<String> ids = response.extractValueAsObject("allPosts[*].id", new TypeRef<List<String>>() {});
        List<String> titles = response.extractValueAsObject("allPosts[*].title", new TypeRef<List<String>>() {});
        assertThat(ids).contains(String.valueOf(postId));
        assertThat(titles).contains("http query title");
    }

    /**
     * The `postById` query over plain HTTP.
     */
    @Test
    void queryPostByIdOverHttp() {
        Long postId = createPost("http post by id title", "http post by id content");

        var response = this.client.reactiveExecuteQuery(
                "query postById($postId: ID!) { postById(postId: $postId) { id title content } }",
                Map.of("postId", String.valueOf(postId)))
                .block(Duration.ofSeconds(30));

        assertThat(response).isNotNull();
        assertThat(response.hasErrors()).isFalse();
        assertThat(response.extractValueAsObject("postById.id", String.class)).isEqualTo(String.valueOf(postId));
        assertThat(response.extractValueAsObject("postById.title", String.class)).isEqualTo("http post by id title");
        assertThat(response.extractValueAsObject("postById.content", String.class)).isEqualTo("http post by id content");
    }

    /**
     * Creates a post over plain HTTP and returns its id.
     */
    private Long createPost(String title, String content) {
        @Language("graphql") var createPostQuery = """
                mutation createPost($input: CreatePostInput!){
                    createPost(createPostInput:$input) {
                        id
                        title
                    }
                }
                """.stripIndent();

        var createPostVariables = Map.of(
                "input", Map.of(
                        "title", title,
                        "content", content
                )
        );

        var post = this.client.reactiveExecuteQuery(createPostQuery, createPostVariables)
                .map(response -> response.extractValueAsObject("createPost", Post.class))
                .block(Duration.ofSeconds(30));

        assertThat(post).isNotNull();
        assertThat(post.getId()).isNotNull();
        return post.getId();
    }

    @Test
    void createCommentAndSubscription() {
        Long postId = createPost("test title", "test content");

        log.debug("created post with id: {}", postId);

        @Language("graphql") var subscriptionQuery = """
                subscription onCommentAdded {
                    commentAdded {
                        id
                        postId
                        content
                    }
                }
                """.stripIndent();

        // add two comments
        String comment1 = "test comment";
        String comment2 = "test comment2";

        var commentAddedDataPublisher = this.socketClient
                .document(subscriptionQuery).executeSubscription()
                .mapNotNull(GraphQlResponse::<Map<String, Map<String, Object>>>getData)
                .map(it -> it.get("commentAdded"))
                // the sink replays the last published comment to every new subscriber,
                // so ignore the comments published by the other tests.
                .filter(comment -> comment1.equals(comment.get("content")) || comment2.equals(comment.get("content")));

        var verifier = StepVerifier.create(commentAddedDataPublisher)
                .thenAwait(Duration.ofMillis(1000)) // see: https://github.com/Netflix/dgs-framework/issues/657
                .consumeNextWith(comment -> assertThat(comment.get("content")).isEqualTo(comment1))
                .consumeNextWith(comment -> assertThat(comment.get("content")).isEqualTo(comment2))
                .thenCancel()
                .verifyLater();

        // add comment
        @Language("graphql") var addCommentQuery = """
                mutation addComment($input: CommentInput!) {
                    addComment(commentInput:$input) {
                        id
                        postId
                        content
                    }
                }
                """.stripIndent();

        var addCommentVariables = Map.of(
                "input", Map.of(
                        "postId", postId,
                        "content", comment1
                )
        );

        var addCommentVariables2 = Map.of(
                "input", Map.of(
                        "postId", postId,
                        "content", comment2
                )
        );

        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo(comment1))
                .verifyComplete();

        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables2)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo(comment2))
                .verifyComplete();

        // verify
        // await().atMost(5, SECONDS).untilAsserted(verifier::verify);
        verifier.verify(Duration.ofSeconds(30));
    }

    /**
     * The subscription is delivered over the Server-Sent-Events transport; the very same
     * subscription is served over the WebSocket transport as well, see
     * {@link #createCommentAndSubscription()}.
     */
    @Test
    void subscribeToTheCommentsOverSse() {
        Long postId = createPost("sse test title", "sse test content");

        @Language("graphql") var addCommentQuery = """
                mutation addComment($input: CommentInput!) {
                    addComment(commentInput:$input) {
                        id
                        postId
                        content
                    }
                }
                """.stripIndent();

        var addCommentVariables = Map.of(
                "input", Map.of(
                        "postId", String.valueOf(postId),
                        "content", SSE_COMMENT
                )
        );

        // the service replays the last published comment to every new subscriber, so the
        // comment is published before the subscription is opened.
        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo(SSE_COMMENT))
                .verifyComplete();

        @Language("graphql") var subscriptionQuery = """
                subscription onCommentAdded {
                    commentAdded {
                        id
                        postId
                        content
                    }
                }
                """.stripIndent();

        var received = this.sseClient.reactiveExecuteQuery(subscriptionQuery, Map.of())
                .map(response -> response.extractValueAsObject("commentAdded.content", String.class))
                .filter(SSE_COMMENT::equals)
                .next()
                .block(Duration.ofSeconds(30));

        assertThat(received).isEqualTo(SSE_COMMENT);
    }
}
