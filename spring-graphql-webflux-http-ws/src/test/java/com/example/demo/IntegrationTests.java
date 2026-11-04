package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.graphql.test.tester.WebSocketGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

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

        @LocalServerPort
        int port;

        HttpGraphQlTester httpTester;

        WebSocketGraphQlTester wsTester;

        @BeforeEach
        void setUp() {
            this.httpTester = HttpGraphQlTester.create(WebTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port + "/graphql")
                    .build());
            this.wsTester = WebSocketGraphQlTester.create(
                    URI.create("ws://localhost:" + port + "/subscriptions"), new ReactorNettyWebSocketClient());
        }

        @Test
        void writesOverHttpAndSubscriptionOverWebSocket() throws InterruptedException {
            var id = createPost();
            getPostById(id);

            Flux<Comment> result = wsTester.document("subscription onCommentAdded { commentAdded { id content } }")
                    .executeSubscription()
                    .toFlux("commentAdded", Comment.class);

            var verifier = StepVerifier.create(result)
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .thenCancel()
                    .verifyLater();

            // register the subscription before the mutations emit: the server sink is replay().latest(),
            // so a late subscriber would only see the last comment
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

            var post = httpTester.document(createPostMutation)
                    .variable("createPostInput", Map.of(
                            "title", "SpringGraphQL",
                            "content", "content of my post"
                    ))
                    .execute()
                    .path("createPost")
                    .entity(Post.class)
                    .get();

            log.info("created post: {}", post);
            assertThat(post.getId()).isNotNull();
            return post.getId();
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

            httpTester.document(postByIdQuery)
                    .variable("postId", id)
                    .execute()
                    .path("postById.title")
                    .entity(String.class)
                    .satisfies(title -> assertThat(title).isEqualTo("SpringGraphQL"));
        }

        private void addCommentToPost(String id) {
            var addCommentMutation = """
                    mutation addComment($commentInput: CommentInput!){
                        addComment(commentInput:$commentInput){
                            id
                        }
                    }""".stripIndent();

            var commentId = httpTester.document(addCommentMutation)
                    .variable("commentInput", Map.of(
                            "postId", id,
                            "content", "comment of my post at " + LocalDateTime.now()
                    ))
                    .execute()
                    .path("addComment.id")
                    .entity(String.class)
                    .get();

            log.info("added comment of post: {}", commentId);
            assertThat(commentId).isNotNull();
        }
    }

    @Nested
    class WithGraphQlClients {

        static final String TITLE = "my post created by Spring GraphQL";

        @LocalServerPort
        int port;

        HttpGraphQlClient client;
        WebSocketGraphQlClient socketClient;

        @BeforeEach
        void setup() {
            // WebClient is the reactive-stack flavour; servlet modules use HttpSyncGraphQlClient + RestClient
            this.client = HttpGraphQlClient.create(WebClient.create("http://localhost:" + port + "/graphql"));
            this.socketClient = WebSocketGraphQlClient.create(
                    URI.create("ws://localhost:" + port + "/subscriptions"), new ReactorNettyWebSocketClient());
            this.socketClient.start().subscribe();
        }

        @AfterEach
        void tearDown() {
            this.socketClient.stop().subscribe();
        }

        @Test
        void createPostAndAddComment() throws InterruptedException {
            var id = createPost();
            getPostById(id);

            var subscriptionQuery = "subscription onCommentAdded { commentAdded { id content } }";
            Flux<Comment> result = socketClient.document(subscriptionQuery)
                    .executeSubscription()
                    .map(response -> response.field("commentAdded").toEntity(Comment.class));

            var verifier = StepVerifier.create(result)
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .consumeNextWith(c -> assertThat(c.getContent()).startsWith("comment of my post at "))
                    .thenCancel()
                    .verifyLater();

            // register the subscription before emitting, then leave a full second for the events
            Thread.sleep(1000);
            addCommentToPost(id);
            addCommentToPost(id);
            addCommentToPost(id);

            verifier.verify(Duration.ofSeconds(10));
        }

        private String createPost() {
            var createPostMutation = """
                    mutation createPost($input: CreatePostInput!){
                        createPost(createPostInput:$input){
                            id
                            title
                            content
                        }
                    }""".stripIndent();

            var postId = new AtomicReference<String>();
            client.document(createPostMutation)
                    .variables(Map.of("input", Map.of("title", TITLE, "content", "content of my post")))
                    .retrieve("createPost")
                    .toEntity(Post.class)
                    .doOnNext(post -> {
                        log.info("created post: {}", post);
                        postId.set(post.getId());
                    })
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();

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
                    .retrieve("postById")
                    .toEntity(Post.class)
                    .as(StepVerifier::create)
                    .consumeNextWith(post -> {
                        assertThat(post.getId()).isEqualTo(id);
                        assertThat(post.getTitle()).isEqualTo(TITLE);
                        assertThat(post.getContent()).isEqualTo("content of my post");
                    })
                    .verifyComplete();
        }

        private void addCommentToPost(String id) {
            var addCommentMutation = """
                    mutation addComment($commentInput: CommentInput!){
                       addComment(commentInput:$commentInput){id}
                    }""".stripIndent();

            client.document(addCommentMutation)
                    .variables(Map.of("commentInput", Map.of(
                            "postId", id,
                            "content", "comment of my post at " + LocalDateTime.now()
                    )))
                    .retrieve("addComment")
                    .toEntity(Comment.class)
                    .as(StepVerifier::create)
                    .consumeNextWith(comment -> assertThat(comment.getId()).isNotNull())
                    .verifyComplete();
        }
    }
}
