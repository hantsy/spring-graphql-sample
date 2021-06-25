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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.http.codec.multipart.Part;

import java.io.File;
import java.nio.file.FileSystem;
import java.util.Map;

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

// how to handle file upload?
//        var query = "fileupload.gql";
//        var file = new FileSystemResource(new ClassPathResource("/test.txt").getFile());
//       // log.info("file: {}", file);
//        var result = graphQLWebClient.post(
//                query,
//                Map.of("file", file),
//                Boolean.class)
//                .block();
//        log.info("file upload result: {}", result);
    }
}
