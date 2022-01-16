package com.example.demo

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor
import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubscriptionWithGraphQLClientTests {
    private val log = LoggerFactory.getLogger(SubscriptionWithGraphQLClientTests::class.java)

    @LocalServerPort
    var port: Int = 8080

    lateinit var webClient: WebClient

    lateinit var client: WebClientGraphQLClient

    @Autowired
    lateinit var dgsQueryExecutor: DgsReactiveQueryExecutor

    @BeforeEach
    fun setup() {
        webClient = WebClient.builder()
            .defaultHeaders { it.setBasicAuth("user", "password") }
            .baseUrl("http://localhost:$port")
            .build()

        client = WebClientGraphQLClient(this.webClient)
    }

    @Test
    fun `create new post without auth`() {

        val requestData = mapOf<String, Any>(
            "query" to "mutation createPost(\$input: CreatePostInput!){ createPost(createPostInput:\$input) {id, title} }",
            "variables" to mapOf(
                "input" to mapOf(
                    "title" to "test title",
                    "content" to "test content"
                )
            )
        )
        this.webClient
            .post().uri("/graphql")// no auth headers
            .bodyValue(requestData)
            .exchangeToMono { it.bodyToMono<Post>() }
            .`as` { StepVerifier.create(it) }
        //
        // security is disabled.
        //.expectBody()
        //.jsonPath("errors.length()").value<Int> { assertThat(it).isGreaterThan(0) }

        // it is an INTERNAL errorType
        //.jsonPath("errors[0].extensions.errorType").isEqualTo("PERMISSION_DENIED")
    }

    @Test
    fun `sign in and create a post and comment`() {
//        val requestExecutor = MonoRequestExecutor { _, headers, body ->
//            webClient.post().uri("/graphql")
//                .headers {
//                    headers.forEach { (t, u) -> it[t] = u }
//                }
//                .bodyValue(body)
//                .retrieve()
//                .toEntity(String::class.java)
//                .map { HttpResponse(it.statusCodeValue, it.body) }
//        }
//
//        val requestExecutorWithAuth = MonoRequestExecutor { _, headers, body ->
//            webClient.post().uri("/graphql")
//                .headers {
//                    it.setBasicAuth("user", "password")
//                    headers.forEach { (t, u) -> it[t] = u }
//                }
//                .bodyValue(body)
//                .retrieve()
//                .toEntity(String::class.java)
//                .map { HttpResponse(it.statusCodeValue, it.body) }
//        }

        val createPostQuery =
            "mutation createPost(\$input: CreatePostInput!){ createPost(createPostInput:\$input) {id, title} }"
        val createPostQueryVariables = mapOf(
            "input" to mapOf(
                "title" to "test title",
                "content" to "test content"
            )
        )
        val createPostResult =
            this.client.reactiveExecuteQuery(createPostQuery, createPostQueryVariables)
                .map { it.extractValueAsObject("createPost", Post::class.java) }
                .block(Duration.ofSeconds(5L))
        val postId = createPostResult?.id
        assertThat(postId).isNotNull
        assertThat(createPostResult?.title).isEqualTo("test title")

        // get post by id.
        val postByIdQuery = "query postById(\$id: String!){postById(postId:\$id){ id title }}"
        val postByIdVariables = mapOf(
            "id" to postId as Any
        )

        this.client.reactiveExecuteQuery(postByIdQuery, postByIdVariables)
            .map { it.extractValueAsObject("postById", Post::class.java) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it!!.title).isEqualTo("test title") }
            .verifyComplete()

        // Simply use `DgsQueryExecutor` to subscribe to commentAdded.
        // TODO: authentication is disabled.
        // The `DgsQueryExecutor` does not work in a web layer.
        val executionResultMono =
            dgsQueryExecutor.execute("subscription onCommentAdded{ commentAdded{ id postId  content}}", emptyMap())
        val publisher = executionResultMono.flatMapMany {
            Flux.from(it.getData<Publisher<ExecutionResult>>())
        }

        val verifier = StepVerifier.create(publisher)
            .consumeNextWith {
                log.debug("comment@1: {}", it)
                assertThat(
                    (it.getData<Map<String, Map<String, Any>>>()["commentAdded"]
                        ?.get("content") as String)
                ).isEqualTo("comment1")
            }
            .consumeNextWith {
                log.debug("comment@2: {}", it)
                assertThat(
                    (it.getData<Map<String, Map<String, Any>>>()["commentAdded"]
                        ?.get("content") as String)
                ).isEqualTo("comment2")
            }
            .thenCancel()
            .verifyLater()//delay to verify

        val commentQuery =
            "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { id postId content}}"
        val comment1Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to "comment1"
            )
        )
        val comment2Variables = mapOf(
            "input" to mapOf(
                "postId" to postId,
                "content" to "comment2"
            )
        )

        this.client.reactiveExecuteQuery(commentQuery, comment1Variables)
            .map { it.extractValueAsObject("addComment", Comment::class.java) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it.content).isEqualTo("comment1") }
            .verifyComplete()

        this.client.reactiveExecuteQuery(commentQuery, comment2Variables)
            .map { it.extractValueAsObject("addComment", Comment::class.java) }
            .`as` { StepVerifier.create(it) }
            .consumeNextWith { assertThat(it.content).isEqualTo("comment2") }
            .verifyComplete()

        // verify now.
        verifier.verify()
    }
}