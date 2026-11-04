package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.graphql.test.tester.WebSocketGraphQlTester;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Slf4j
class IntegrationTests {

    @Nested
    class WithGraphQlTesters {

        WebSocketGraphQlTester graphQlTester;

        @LocalServerPort
        int port;

        @BeforeEach
        void setUp() {
            this.graphQlTester = WebSocketGraphQlTester.create(URI.create("ws://localhost:" + port + "/subscriptions"), new ReactorNettyWebSocketClient());
        }

        @SneakyThrows
        @Test
        public void createPostAndAddComment() {
            var creatPost = """
                    mutation createPost($createPostInput: CreatePostInput!){
                       createPost(createPostInput:$createPostInput){
                       id
                       title
                       content
                       }
                    }""".trim();

            String TITLE = "my post created by Spring GraphQL";
            String id = graphQlTester.document(creatPost)
                    .variable("createPostInput",
                            Map.of(
                                    "title", TITLE,
                                    "content", "content of my post"
                            ))
                    .execute()
                    .path("createPost.id").entity(String.class).get();

            log.info("created post: {}", id);
            assertThat(id).isNotNull();

            var postById = """
                    query post($postId:String!){
                       postById(postId:$postId) {
                         id
                         title
                         content
                       }
                     }""";
            graphQlTester.document(postById).variable("postId", id.toString())
                    .execute()
                    .path("postById.title")
                    .entity(String.class)
                    .satisfies(titles -> assertThat(titles).isEqualTo(TITLE));


            Flux<Comment> result = this.graphQlTester.document("subscription onCommentAdded { commentAdded { id content } }")
                    .executeSubscription()
                    .toFlux("commentAdded", Comment.class);

            var verify = StepVerifier.create(result)
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .thenCancel().verifyLater();

            addCommentToPost(id);
            addCommentToPost(id);
            addCommentToPost(id);

            verify.verify(Duration.ofSeconds(5));
        }

        private void addCommentToPost(String id) {
            var addComment = """
                    mutation addComment($commentInput: CommentInput!){
                       addComment(commentInput:$commentInput){id}
                    }""";

            String commentId = graphQlTester.document(addComment)
                    .variable("commentInput",
                            Map.of(
                                    "postId", id,
                                    "content", "comment of my post at " + LocalDateTime.now()
                            ))
                    .execute()
                    .path("addComment.id")
                    .entity(String.class).get();

            log.info("added comment of post: {}", commentId);
            assertThat(commentId).isNotNull();
        }
    }

    @Nested
    class WithGraphQlClients {

        @LocalServerPort
        int port;

        @Autowired
        JsonMapper jsonMapper;

        WebSocketGraphQlClient client;

        @BeforeEach
        void setUp() {
            this.client = WebSocketGraphQlClient.create(
                    URI.create("ws://localhost:" + port + "/subscriptions"), new ReactorNettyWebSocketClient());
            // start()/stop() return Mono<Void>: nothing happens until the Mono is subscribed
            this.client.start().subscribe();
        }

        @AfterEach
        void tearDown() {
            this.client.stop().subscribe();
        }

        @Test
        void allThreeOperationTypesOverWebSocket() throws InterruptedException {
            var id = createPost();
            getPostById(id);

            Flux<Comment> result = client.document("subscription onCommentAdded { commentAdded { id content } }")
                    .executeSubscription()
                    .map(response -> jsonMapper.convertValue(
                            response.<Map<String, Object>>getData().get("commentAdded"), Comment.class));

            var verifier = StepVerifier.create(result)
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .thenCancel()
                    .verifyLater();

            Thread.sleep(1000);

            addCommentToPost(id);
            addCommentToPost(id);
            addCommentToPost(id);

            verifier.verify(Duration.ofSeconds(5));
        }

        private String createPost() {
            var createPostMutation = """
                    mutation createPost($createPostInput: CreatePostInput!){
                       createPost(createPostInput:$createPostInput){
                           id
                           title
                           content
                       }
                    }""".stripIndent();

            var postId = new AtomicReference<String>();
            client.document(createPostMutation)
                    .variable("createPostInput", Map.of(
                            "title", "SpringGraphQL",
                            "content", "content of my post"
                    ))
                    .execute()
                    .map(response -> jsonMapper.convertValue(
                            response.<Map<String, Object>>getData().get("createPost"), Post.class))
                    .doOnNext(post -> postId.set(post.getId()))
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();

            log.info("created post: {}", postId.get());
            assertThat(postId.get()).isNotNull();
            return postId.get();
        }

        private void getPostById(String id) {
            var postByIdQuery = """
                    query post($postId:String!){
                       postById(postId:$postId) {
                          id
                          title
                          content
                       }
                    }""".stripIndent();

            client.document(postByIdQuery)
                    .variable("postId", id)
                    .execute()
                    .as(StepVerifier::create)
                    .consumeNextWith(response -> {
                        var post = jsonMapper.convertValue(
                                response.<Map<String, Object>>getData().get("postById"), Post.class);
                        assertThat(post.getId()).isEqualTo(id);
                        assertThat(post.getTitle()).isEqualTo("SpringGraphQL");
                    })
                    .verifyComplete();
        }

        private void addCommentToPost(String id) {
            var addCommentMutation = """
                    mutation addComment($commentInput: CommentInput!){
                        addComment(commentInput:$commentInput){
                            id
                        }
                    }""".stripIndent();

            client.document(addCommentMutation)
                    .variable("commentInput", Map.of(
                            "postId", id,
                            "content", "comment of my post at " + LocalDateTime.now()
                    ))
                    .execute()
                    .as(StepVerifier::create)
                    .consumeNextWith(response -> {
                        var comment = jsonMapper.convertValue(
                                response.<Map<String, Object>>getData().get("addComment"), Comment.class);
                        assertThat(comment.getId()).isNotNull();
                    })
                    .verifyComplete();
        }
    }
}
