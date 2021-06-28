package com.example.demo;

import com.example.demo.gql.client.AllPostsGraphQLQuery;
import com.example.demo.gql.client.AllPostsProjectionRoot;
import com.example.demo.gql.types.Post;
import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.client.DefaultGraphQLClient;
import com.netflix.graphql.dgs.client.GraphQLClient;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.HttpResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
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

    private static HttpResponse execute(String url, Map<String, ? extends List<String>> headers, String body) {
        /**
         * The requestHeaders providers headers typically required to call a GraphQL endpoint, including the Accept and Content-Type headers.
         * To use RestTemplate, the requestHeaders need to be transformed into Spring's HttpHeaders.
         */
        HttpHeaders requestHeaders = new HttpHeaders();
        headers.forEach(requestHeaders::put);

        /**
         * Use RestTemplate to call the GraphQL service.
         * The response type should simply be String, because the parsing will be done by the GraphQLClient.
         */
        var dgsRestTemplate = new RestTemplate();
        ResponseEntity<String> exchange = dgsRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity(body, requestHeaders), String.class);

        /**
         * Return a HttpResponse, which contains the HTTP status code and response body (as a String).
         * The way to get these depend on the HTTP client.
         */
        return new HttpResponse(exchange.getStatusCodeValue(), exchange.getBody());
    }


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
                        new AllPostsProjectionRoot()
                                .id()
                                .title()
                                .content()
                                .status()
                                .parent()
                                .author().id().name().email().parent()
                                .comments().id().content().parent()
                                .createdAt()
                );

        String query = graphQLQueryRequest.serialize();
        GraphQLClient client = new DefaultGraphQLClient(url);
        GraphQLResponse response = client.executeQuery(query, new HashMap<>(), DemoApplication::execute);

        var data = response.extractValueAsObject("allPosts", new TypeRef<List<Post>>() {
        });
        log.info("fetched all posts from client: {}", data);
    }
}
