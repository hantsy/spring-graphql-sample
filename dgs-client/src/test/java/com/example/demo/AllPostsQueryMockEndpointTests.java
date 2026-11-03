package com.example.demo;

import com.example.demo.gql.client.AllPostsGraphQLQuery;
import com.example.demo.gql.client.AllPostsProjectionRoot;
import com.example.demo.gql.client.AuthorProjection;
import com.example.demo.gql.client.PostProjection;
import com.example.demo.gql.types.Post;
import com.example.demo.gql.types.PostStatus;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.client.DgsGraphQLClient;
import com.netflix.graphql.dgs.client.DgsGraphQLResponse;
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import io.github.nilwurtz.GraphqlBodyMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Stubs the GraphQL endpoint with WireMock and the official wiremock-graphql-extension, so the
 * typesafe client is exercised without a running server. The extension compares the request body
 * semantically on the GraphQL query, ignoring whitespace and field order.
 */
class AllPostsQueryMockEndpointTests {

    private static final String RESPONSE_BODY = """
            {
              "data": {
                "allPosts": [
                  {
                    "id": "1",
                    "title": "Hello GraphQL",
                    "content": "The first post",
                    "status": "DRAFT",
                    "createdAt": "2026-11-03T10:15:30",
                    "author": {
                      "id": "a1",
                      "name": "Hantsy",
                      "email": "hantsy@example.com"
                    },
                    "comments": [
                      { "id": "c1", "content": "Nice read" }
                    ]
                  }
                ]
              }
            }
            """;

    private final WireMockServer wireMock = new WireMockServer(
            options().dynamicPort().extensions(new GraphqlBodyMatcher()));

    @BeforeEach
    void startWireMock() {
        wireMock.start();
    }

    @AfterEach
    void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void deserializesAllPostsFromTheMockedEndpoint() {
        GraphQLQueryRequest queryRequest =
                new GraphQLQueryRequest(new AllPostsGraphQLQuery(), projection());

        wireMock.stubFor(post(urlEqualTo("/graphql"))
                .andMatching(GraphqlBodyMatcher.extensionName,
                        GraphqlBodyMatcher.parameters(queryRequest.serialize(), Map.of()))
                .willReturn(okJson(RESPONSE_BODY)));

        DgsGraphQLClient client = client();
        DgsGraphQLResponse response = client.executeQuery(queryRequest.serialize(), Map.of());

        List<Post> posts = response.extractValueAsObject("allPosts", new TypeRef<List<Post>>() {
        });

        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.getId()).isEqualTo("1");
            assertThat(post.getTitle()).isEqualTo("Hello GraphQL");
            assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
            assertThat(post.getCreatedAt()).hasToString("2026-11-03T10:15:30");
            assertThat(post.getAuthor().getName()).isEqualTo("Hantsy");
            assertThat(post.getComments()).extracting("content").containsExactly("Nice read");
        });

        wireMock.verify(WireMock.postRequestedFor(urlEqualTo("/graphql")));
    }

    @Test
    void anUnexpectedQueryIsNotStubbed() {
        wireMock.stubFor(post(urlEqualTo("/graphql"))
                .andMatching(GraphqlBodyMatcher.extensionName,
                        GraphqlBodyMatcher.parameters("query { postById(postId: \"1\") { id } }", Map.of()))
                .willReturn(okJson(RESPONSE_BODY)));

        DgsGraphQLClient client = client();

        assertThatExceptionOfType(RestClientResponseException.class)
                .isThrownBy(() -> client.executeQuery(
                        new GraphQLQueryRequest(new AllPostsGraphQLQuery(), projection()).serialize(), Map.of()));
    }

    private DgsGraphQLClient client() {
        return new DgsRestClientGraphQLClient(RestClient.builder().baseUrl(wireMock.url("/graphql")).build());
    }

    private static AllPostsProjectionRoot<AuthorProjection<?, ?>, PostProjection<?, ?>> projection() {
        return new AllPostsProjectionRoot<AuthorProjection<?, ?>, PostProjection<?, ?>>()
                .id()
                .title()
                .content()
                .status()
                .parent()
                .author().id().name().email().parent()
                .comments().id().content().parent()
                .createdAt();
    }
}
