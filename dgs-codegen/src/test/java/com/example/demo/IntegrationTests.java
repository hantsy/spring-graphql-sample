package com.example.demo;

import com.netflix.graphql.dgs.client.DgsGraphQLClient;
import com.netflix.graphql.dgs.client.DgsGraphQLResponse;
import com.netflix.graphql.dgs.client.DgsGraphqlSSESubscriptionGraphQLClient;
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class IntegrationTests {

    private static final String SSE_COMMENT = "sse test comment";

    @LocalServerPort
    int port;

    private DgsGraphQLClient client() {
        return new DgsRestClientGraphQLClient(
                RestClient.builder().baseUrl("http://localhost:" + port + "/graphql").build());
    }

    @Test
    void createPostAndQueryIt() {
        var client = client();

        var mutation = """
                mutation createPost($input: CreatePostInput!) {
                    createPost(createPostInput: $input)
                }
                """;
        DgsGraphQLResponse createResponse = client.executeQuery(mutation, Map.of(
                "input", Map.of(
                        "title", "integration test",
                        "content", "integration content"
                )
        ));

        String uuid = createResponse.extractValueAsObject("createPost", String.class);
        assertThat(uuid).isNotBlank();

        // the @uppercase directive uppercases Post.title
        String query = "{ allPosts { title } }";
        DgsGraphQLResponse listResponse = client.executeQuery(query, Map.of());

        assertThat(listResponse.getJson()).contains("INTEGRATION TEST");
    }

    /**
     * The subscription is delivered over the Server-Sent-Events transport - the reactive stack
     * would use a WebSocket instead, but the servlet stack of this module supports SSE.
     */
    @Test
    void subscribeToTheCommentsOverSse() {
        var client = client();
        var baseUrl = "http://localhost:" + port + "/graphql";
        var sseClient = new DgsGraphqlSSESubscriptionGraphQLClient(baseUrl, WebClient.create(baseUrl));

        // create the post the comment is added to.
        var createPostMutation = """
                mutation createPost($input: CreatePostInput!) {
                    createPost(createPostInput: $input)
                }
                """;
        String postId = client.executeQuery(createPostMutation, Map.of(
                "input", Map.of(
                        "title", "sse test title",
                        "content", "sse test content"
                )
        )).extractValueAsObject("data.createPost", String.class);
        assertThat(postId).isNotBlank();

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
}
