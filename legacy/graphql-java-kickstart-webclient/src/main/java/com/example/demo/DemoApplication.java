package com.example.demo;

import com.example.demo.gql.types.Post;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.kickstart.spring.webclient.boot.GraphQLRequest;
import graphql.kickstart.spring.webclient.boot.GraphQLResponse;
import graphql.kickstart.spring.webclient.boot.GraphQLWebClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Flow;

@SpringBootApplication
@Slf4j
public class DemoApplication implements ApplicationRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Autowired
    WebClient webClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private GraphQLWebClient graphQLWebClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //getAllPosts();
        //uploadFile();
        createPostAndComment();
    }

    @SneakyThrows
    private void createPostAndComment() {
        var createPost = """
                mutation createPost($createPostInput: CreatePostInput!){
                   createPost(createPostInput:$createPostInput)
                }
                """;
        GraphQLRequest request = GraphQLRequest.builder()
                .query(createPost)
                .variables(
                        Map.of("createPostInput",
                                Map.of(
                                        "title", "My first post created by GraphQLWebClient",
                                        "content", "content of my post"
                                )
                        )
                )
                .build();
        GraphQLResponse response = graphQLWebClient.post(request).block();
        var postId = response.get("createPost", UUID.class);
        log.info("created post : {}", postId);

        addCommentToPost(postId);
        addCommentToPost(postId);
        addCommentToPost(postId);
        subscribeToCommentAdded();

    }

    private void subscribeToCommentAdded() throws URISyntaxException {

        WebSocketClient client = new ReactorNettyWebSocketClient();
        var msg = Map.of(
                "query", "subscription onCommentAdded { commentAdded { id content } }"
        );
        String json = toJson(msg);
        client
                .execute(
                        URI.create("ws://localhost:8080/subscriptions"),
                        session -> session.send(Mono.just(session.textMessage(json)))
                                .thenMany(session.receive()
                                        .map(WebSocketMessage::getPayloadAsText)
                                        .log()
                                )
                                .then()
                )
                .block(Duration.ofSeconds(10L));
        // connect to websocket.
    }

    private String toJson(Map<String, ?> msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addCommentToPost(UUID postId) {
        var addComment = """
                mutation addComment($commentInput: CommentInput!){
                   addComment(commentInput:$commentInput)
                }
                """;
        GraphQLRequest addCommentRequest = GraphQLRequest.builder()
                .query(addComment)
                .variables(
                        Map.of("commentInput",
                                Map.of(
                                        "postId", postId,
                                        "content", "content of my post at " + LocalDateTime.now()
                                )
                        )
                )
                .build();
        GraphQLResponse addCommentResponse = graphQLWebClient.post(addCommentRequest).block();
        var id = addCommentResponse.get("addComment", UUID.class);
        log.info("created comment : {}", id);
    }

    private void uploadFile() throws IOException {
        var query = "mutation upload($file:Upload!){ upload(file:$file)}";
        var variables = new HashMap<String, Object>();
        variables.put("file", null);
        var operations = Map.of(
                "query", query,
                "variables", variables
        );
        var map = Map.of("file0", List.of("variables.file"));

        var file = new FileSystemResource(new ClassPathResource("/test.txt").getFile());
        // log.info("file: {}", file);
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("operations", operations, MediaType.APPLICATION_JSON);
        builder.part("map", map, MediaType.APPLICATION_JSON);
        builder.part("file0", file, MediaType.TEXT_PLAIN).filename("test.txt");

        var result = webClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(builder.build())
                .retrieve()
                //@formatter:off
                .bodyToMono(new ParameterizedTypeReference<Map<String, Map<String, Boolean>>>() {})
                //@formatter:on
                .block(Duration.ofSeconds(5));

        log.info("result data: {}", result);
    }

    private void getAllPosts() {
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
