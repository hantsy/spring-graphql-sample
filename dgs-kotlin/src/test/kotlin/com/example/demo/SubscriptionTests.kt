package com.example.demo

import com.example.demo.gql.types.AuthResult
import com.netflix.graphql.dgs.DgsQueryExecutor
import graphql.ExecutionResult
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import reactor.test.StepVerifier
import java.util.*

@SpringBootTest(
    classes = [DemoApplication::class],
    properties = ["context.initializer.classes=com.example.demo.TestConfigInitializer"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SubscriptionTests {

    @LocalServerPort
    var port: Int = 8080

    var restTemplate: TestRestTemplate? = null

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @BeforeEach
    fun setup() {
        restTemplate = TestRestTemplate(
            RestTemplateBuilder()
                .defaultMessageConverters()
                .rootUri("http://localhost:$port")
        )
    }

    @Test
    fun `sign in and create a post and comment`() {
        // logged in
        val signinData = mapOf<String, Any>(
            "query" to "mutation signIn(\$input: Credentials!){ signIn(credentials:\$input) {name, roles, token} }",
            "variables" to mapOf(
                "input" to mapOf(
                    "username" to "user",
                    "password" to "password"
                )
            )
        )

        val signinEntity = RequestEntity.post("graphql")
            .accept(MediaType.APPLICATION_JSON)
            .body(signinData)

        val signinResponseEntity = restTemplate!!.exchange(
            signinEntity,
            object : ParameterizedTypeReference<HashMap<String, HashMap<String, AuthResult>>>() {}
        )

        assertThat(signinResponseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(signinResponseEntity.body!!["data"]!!["signIn"]!!.name).isEqualTo("user")
        assertThat(signinResponseEntity.body!!["data"]!!["signIn"]!!.roles).contains("ROLE_USER")

        val token = signinResponseEntity.body!!["data"]!!["signIn"]!!.token

        assertThat(token).isNotNull

        // create a new post
        val createPostQuery = "mutation createPost(\$input: CreatePostInput!){createPost(createPostInput:\$input){ id title }}"
        val createPostResult = dgsQueryExecutor.executeAndGetDocumentContext(
            createPostQuery,
            mapOf("input" to mapOf("title" to "my title", "content" to "my content of my title")),
            HttpHeaders().apply { add("X-Auth-Token", token) }
        )

        val postId = createPostResult.read("data.createPost.id", String::class.java)
        println("created post id: $postId")
        assertThat(postId).isNotNull

        // get post by id.
        val postByIdQuery = "query postById(\$id: String!){postById(postId:\$id){ id title }}"
        val postByIdResult = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            postByIdQuery,
            "data.postById.title",
            mapOf("id" to UUID.randomUUID().toString())
        )

        println("get post by id: $postId, result: $postByIdResult")
        assertThat(postByIdResult).isEqualTo("my title")

        //subscribe to commentAdded.
        val executionResult = dgsQueryExecutor.execute("subscription onCommentAdded{ commentAdded{ id content}}")
        val publisher = executionResult.getData<Publisher<ExecutionResult>>()

        val verifier = StepVerifier.create(publisher)
            .consumeNextWith {
                assertThat(
                    (it.getData<Map<String, Map<String, Any>>>()["commentAdded"]
                        ?.get("content") as String)
                ).contains("comment1")
            }
            .consumeNextWith {
                assertThat(
                    (it.getData<Map<String, Map<String, Any>>>()["commentAdded"]
                        ?.get("content") as String)
                ).contains("comment2")
            }
            .thenCancel()
            .verifyLater()


        val comment1 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { content}}",
            "data.commentAdded.content",
            mapOf("postId" to postId, "content" to "comment1 message")
        )
        assertThat(comment1).contains("comment1");

        val comment2 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { content}}",
            "data.commentAdded.content",
            mapOf("postId" to postId, "content" to "comment2 message")
        )
        assertThat(comment2).contains("comment2");

        //verify it now.
        verifier.verify();
    }

}