package com.example.demo;

import com.example.demo.gql.types.Post;
import graphql.kickstart.spring.webclient.boot.GraphQLRequest;
import graphql.kickstart.spring.webclient.boot.GraphQLResponse;
import graphql.kickstart.spring.webclient.boot.GraphQLWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@Slf4j
public class DemoApplication implements ApplicationRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Autowired
    private GraphQLWebClient graphQLWebClient;

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
        GraphQLRequest request = GraphQLRequest.builder().query(allPosts).build();
        GraphQLResponse response = graphQLWebClient.post(request).block();
        var data = response.getList("allPosts", Post.class);
        log.info("fetched all posts from client: {}", data);
    }
}
