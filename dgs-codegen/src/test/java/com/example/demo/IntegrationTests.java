package com.example.demo;

import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.client.DgsGraphQLClient;
import com.netflix.graphql.dgs.client.DgsGraphQLResponse;
import com.netflix.graphql.dgs.client.DgsGraphqlSSESubscriptionGraphQLClient;
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class IntegrationTests {

    private static final String SSE_COMMENT = "sse test comment";

    private static final String WS_COMMENT = "websocket test comment";

    private static final String HTTP_COMMENT = "http test comment";

    @LocalServerPort
    int port;

    private DgsGraphQLClient client;

    private DgsGraphqlSSESubscriptionGraphQLClient sseClient;

    private WebSocketGraphQlClient socketClient;

    @BeforeEach
    void setup() {
        String baseUrl = "http://localhost:" + port + "/graphql";

        client = new DgsRestClientGraphQLClient(
                RestClient.builder().baseUrl(baseUrl).build());

        sseClient = new DgsGraphqlSSESubscriptionGraphQLClient(baseUrl, WebClient.create(baseUrl));

        // the WebSocket transport is served under the same path as the HTTP endpoint.
        socketClient = WebSocketGraphQlClient.create(
                URI.create("ws://localhost:" + port + "/graphql"),
                new ReactorNettyWebSocketClient());
        socketClient.start().subscribe();
    }

    @AfterEach
    void teardown() {
        socketClient.stop().subscribe();
    }

    /**
     * The `createPost` mutation over plain HTTP.
     */
    @Test
    void createPostOverHttp() {
        String uuid = createPost("http mutation title", "http mutation content");

        // the schema declares the id as the custom `UUID` scalar.
        assertThat(UUID.fromString(uuid)).isNotNull();
    }

    /**
     * The `addComment` mutation over plain HTTP.
     */
    @Test
    void addCommentOverHttp() {
        String postId = createPost("http comment title", "http comment content");

        DgsGraphQLResponse response = client.executeQuery(
                """
                mutation addComment($input: CommentInput!) {
                    addComment(commentInput: $input) {
                        id
                        postId
                        content
                    }
                }
                """,
                Map.of("input", Map.of(
                        "postId", postId,
                        "content", HTTP_COMMENT
                )));

        assertThat(response.hasErrors()).isFalse();
        assertThat(response.extractValueAsObject("addComment.id", String.class)).isNotBlank();
        assertThat(response.extractValueAsObject("addComment.postId", String.class)).isEqualTo(postId);
        assertThat(response.extractValueAsObject("addComment.content", String.class)).isEqualTo(HTTP_COMMENT);
    }

    /**
     * The `allPosts` query over plain HTTP.
     */
    @Test
    void queryAllPostsOverHttp() {
        String uuid = createPost("http query title", "http query content");

        DgsGraphQLResponse response = client.executeQuery("{ allPosts { id title content } }", Map.of());

        assertThat(response.hasErrors()).isFalse();
        assertThat(response.extractValueAsObject("allPosts[*].id", new TypeRef<List<String>>() {}))
                .contains(uuid);
        // the @uppercase directive uppercases Post.title
        assertThat(response.extractValueAsObject("allPosts[*].title", new TypeRef<List<String>>() {}))
                .contains("HTTP QUERY TITLE");
    }

    /**
     * The `postById` query over plain HTTP.
     */
    @Test
    void queryPostByIdOverHttp() {
        String uuid = createPost("http post by id title", "http post by id content");

        DgsGraphQLResponse response = client.executeQuery(
                """
                query postById($postId: String!) {
                    postById(postId: $postId) {
                        id
                        title
                        content
                    }
                }
                """,
                Map.of("postId", uuid));

        assertThat(response.hasErrors()).isFalse();
        assertThat(response.extractValueAsObject("postById.id", String.class)).isEqualTo(uuid);
        // the @uppercase directive uppercases Post.title
        assertThat(response.extractValueAsObject("postById.title", String.class)).isEqualTo("HTTP POST BY ID TITLE");
        assertThat(response.extractValueAsObject("postById.content", String.class)).isEqualTo("http post by id content");
    }

    /**
     * Creates a post over plain HTTP and returns its id.
     */
    private String createPost(String title, String content) {
        DgsGraphQLResponse response = client.executeQuery(
                """
                mutation createPost($input: CreatePostInput!) {
                    createPost(createPostInput: $input)
                }
                """,
                Map.of("input", Map.of(
                        "title", title,
                        "content", content
                )));

        assertThat(response.hasErrors()).isFalse();
        String uuid = response.extractValueAsObject("createPost", String.class);
        assertThat(uuid).isNotBlank();
        return uuid;
    }

    /**
     * The subscription is delivered over the Server-Sent-Events transport; the very same
     * subscription is served over the WebSocket transport as well, see
     * {@link #subscribeToTheCommentsOverWebSocket()}.
     */
    @Test
    void subscribeToTheCommentsOverSse() {
        // create the post the comment is added to.
        String postId = createPost("sse test title", "sse test content");

        var subscriptionQuery = """
                subscription onCommentAdded {
                    commentAdded {
                        id
                        postId
                        content
                    }
                }
                """;

        var comments = sseClient.reactiveExecuteQuery(subscriptionQuery, Map.of())
                .map(response -> response.extractValueAsObject("data.commentAdded.content", String.class))
                // the service replays the last published comment to every new subscriber
                .filter(SSE_COMMENT::equals);

        var verifier = StepVerifier.create(comments)
                .consumeNextWith(content -> assertThat(content).isEqualTo(SSE_COMMENT))
                .thenCancel()
                .verifyLater();

        // add the comment, the subscription should receive it.
        var addCommentMutation = """
                mutation addComment($input: CommentInput!) {
                    addComment(commentInput: $input) {
                        id
                        postId
                        content
                    }
                }
                """;
        client.executeQuery(addCommentMutation, Map.of(
                "input", Map.of(
                        "postId", postId,
                        "content", SSE_COMMENT
                )
        ));

        verifier.verify(Duration.ofSeconds(30));
    }

    /**
     * The very same subscription is served over the WebSocket transport; the client speaks the
     * `graphql-transport-ws` sub-protocol.
     */
    @Test
    void subscribeToTheCommentsOverWebSocket() {
        // create the post the comment is added to.
        String postId = createPost("websocket test title", "websocket test content");

        var subscriptionQuery = """
                subscription onCommentAdded {
                    commentAdded {
                        id
                        postId
                        content
                    }
                }
                """;

        var comments = socketClient.document(subscriptionQuery).executeSubscription()
                .filter(GraphQlResponse::isValid)
                .map(response -> response.<Map<String, Object>>getData())
                .mapNotNull(data -> data.get("commentAdded"))
                .map(comment -> ((Map<String, Object>) comment).get("content"))
                .cast(String.class)
                // the service replays the last published comment to every new subscriber,
                // so ignore the comments published by the other tests.
                .filter(WS_COMMENT::equals);

        var verifier = StepVerifier.create(comments)
                .consumeNextWith(content -> assertThat(content).isEqualTo(WS_COMMENT))
                .thenCancel()
                .verifyLater();

        // add the comment, the subscription should receive it.
        var addCommentMutation = """
                mutation addComment($input: CommentInput!) {
                    addComment(commentInput: $input) {
                        id
                        postId
                        content
                    }
                }
                """;
        client.executeQuery(addCommentMutation, Map.of(
                "input", Map.of(
                        "postId", postId,
                        "content", WS_COMMENT
                )
        ));

        verifier.verify(Duration.ofSeconds(30));
    }
}
