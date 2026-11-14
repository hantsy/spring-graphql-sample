package com.example.demo;

import com.netflix.graphql.dgs.client.DgsGraphQLClient;
import com.netflix.graphql.dgs.client.DgsGraphQLResponse;
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class IntegrationTests {

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
}
