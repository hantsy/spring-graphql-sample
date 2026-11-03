package com.example.demo;

import com.example.demo.gql.client.AllPostsGraphQLQuery;
import com.example.demo.gql.client.AllPostsProjectionRoot;
import com.example.demo.gql.client.AuthorProjection;
import com.example.demo.gql.client.PostProjection;
import com.example.demo.gql.types.Post;
import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.client.DgsGraphQLClient;
import com.netflix.graphql.dgs.client.DgsGraphQLResponse;
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@Slf4j
public class DemoApplication implements ApplicationRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Value("${graphql.client.url}")
    private String url;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var allPosts = """
                query posts{
                   allPosts{
                     id
                     title
                     content
                     status
                     createdAt
                     author{
                        id
                        name
                        email
                     }
                     comments{
                        id
                        content
                     }
                   }
                 }
                """;

        GraphQLQueryRequest graphQLQueryRequest =
                new GraphQLQueryRequest(
                        new AllPostsGraphQLQuery(),
                        new AllPostsProjectionRoot<AuthorProjection<?, ?>, PostProjection<?, ?>>()
                                .id()
                                .title()
                                .content()
                                .status()
                                .parent()
                                .author().id().name().email().parent()
                                .comments().id().content().parent()
                                .createdAt()
                );

        @Language("graphql") String query = graphQLQueryRequest.serialize();
        log.info("query string: {}", query);

        // DgsRestClientGraphQLClient is the Jackson 3 based client added in DGS 12; the older
        // CustomGraphQLClient(url, executor) still works but pulls Jackson 2 (jackson-module-kotlin)
        // onto the classpath because it builds its own ObjectMapper.
        DgsGraphQLClient client = new DgsRestClientGraphQLClient(RestClient.builder().baseUrl(url).build());
        DgsGraphQLResponse response = client.executeQuery(query, Map.of());

        var data = response.extractValueAsObject("allPosts", new TypeRef<List<Post>>() {
        });
        log.info("fetched all posts from client: {}", data);
    }
}
