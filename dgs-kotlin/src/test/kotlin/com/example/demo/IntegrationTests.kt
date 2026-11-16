package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.DgsGraphqlSSESubscriptionGraphQLClient
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient
import com.netflix.graphql.dgs.client.ErrorType
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.graphql.client.WebSocketGraphQlClient
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration

/**
 * Transport level behaviour of the GraphQL endpoint, exercised through the GraphQL client
 * libraries: the DGS REST client, the DGS SSE subscription client and Spring GraphQL's
 * built-in WebSocket client.
 * See `RestClient_IntegrationTests` for the same behaviour seen without any client library.
 */
@SpringBootTest(
    classes = [DemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestcontainersConfiguration::class)
class IntegrationTests {

    companion object {
        private val log = LoggerFactory.getLogger(IntegrationTests::class.java)

        private const val CREATE_POST_MUTATION = """
            mutation createPost(${'$'}input: CreatePostInput!){
                createPost(createPostInput:${'$'}input){
                    id
                    title
                }
            }
        """

        private const val ADD_COMMENT_MUTATION = """
            mutation addComment(${'$'}input: CommentInput!){
                addComment(commentInput:${'$'}input){
                    id
                    postId
                    content
                }
            }
        """

        private const val SIGN_IN_MUTATION = """
            mutation signIn(${'$'}input: Credentials!){
                signIn(credentials:${'$'}input){
                    name
                    roles
                    token
                }
            }
        """

        private const val COMMENT_ADDED_SUBSCRIPTION = """
            subscription onCommentAdded {
                commentAdded {
                    id
                    postId
                    content
                }
            }
        """

        private val CREATE_POST_VARIABLES = mapOf(
            "input" to mapOf(
                "title" to "test title",
                "content" to "test content"
            )
        )
    }

    @LocalServerPort
    var port: Int = 8080

    lateinit var restClient: DgsRestClientGraphQLClient

    lateinit var authenticatedRestClient: DgsRestClientGraphQLClient

    lateinit var sseClient: DgsGraphqlSSESubscriptionGraphQLClient

    lateinit var socketClient: WebSocketGraphQlClient

    @BeforeEach
    fun setup() {
        val graphqlUrl = "http://localhost:$port/graphql"

        restClient = DgsRestClientGraphQLClient(
            RestClient.builder().baseUrl(graphqlUrl).build()
        )

        // the session token of the `signIn` mutation is sent back in the `X-Auth-Token` header.
        val token = restClient
            .executeQuery(
                SIGN_IN_MUTATION,
                mapOf(
                    "input" to mapOf(
                        "username" to "user",
                        "password" to "password"
                    )
                )
            )
            .extractValueAsObject("data.signIn.token", String::class.java)

        authenticatedRestClient = DgsRestClientGraphQLClient(
            RestClient.builder()
                .baseUrl(graphqlUrl)
                .defaultHeaders { it.set("X-Auth-Token", token) }
                .build()
        )

        sseClient = DgsGraphqlSSESubscriptionGraphQLClient(graphqlUrl, WebClient.create(graphqlUrl))

        socketClient = WebSocketGraphQlClient.create(
            URI.create("ws://localhost:$port/subscriptions"),
            ReactorNettyWebSocketClient()
        )
        socketClient.start().block(Duration.ofSeconds(30))
    }

    @AfterEach
    fun teardown() {
        socketClient.stop().block(Duration.ofSeconds(30))
    }

    @Test
    fun `create a post without auth over REST should return errors PERMISSION_DENIED`() {
        val response = restClient.executeQuery(CREATE_POST_MUTATION, CREATE_POST_VARIABLES)
        log.debug("rest response: {}", response.json)

        assertThat(response.hasErrors()).isTrue()
        assertThat(response.errors).hasSize(1)
        assertThat(response.errors[0].extensions?.errorType).isEqualTo(ErrorType.PERMISSION_DENIED)
    }

    @Test
    fun `create a post without auth over websocket should return errors PERMISSION_DENIED`() {
        val response = socketClient
            .document(CREATE_POST_MUTATION)
            .variables(CREATE_POST_VARIABLES)
            .execute()
            .block(Duration.ofSeconds(30))!!
        log.debug("websocket response: {}", response)

        assertThat(response.errors).isNotEmpty
        assertThat(response.errors[0].extensions["errorType"]).isEqualTo("PERMISSION_DENIED")
    }

    @Test
    fun `create a post with auth over REST`() {
        val client = authenticatedRestClient

        val postId = client
            .executeQuery(CREATE_POST_MUTATION, CREATE_POST_VARIABLES)
            .extractValueAsObject("data.createPost.id", String::class.java)

        assertThat(postId).isNotBlank()
    }

    @Test
    fun `subscribe to the comments over SSE`() {
        val client = authenticatedRestClient

        // create the post the comment is added to.
        val postId = client
            .executeQuery(CREATE_POST_MUTATION, CREATE_POST_VARIABLES)
            .extractValueAsObject("data.createPost.id", String::class.java)

        val comments = sseClient
            .reactiveExecuteQuery(COMMENT_ADDED_SUBSCRIPTION, emptyMap<String, Any>())
            .map { response ->
                response.extractValueAsObject("data.commentAdded", object : TypeRef<Map<String, Any>>() {})
            }

        val verifier = StepVerifier.create(comments)
            .consumeNextWith { comment ->
                comment["content"] shouldBe "test comment"
            }
            .thenCancel()
            .verifyLater()

        // add the comment, the subscription should receive it.
        client.executeQuery(
            ADD_COMMENT_MUTATION,
            mapOf(
                "input" to mapOf(
                    "postId" to postId,
                    "content" to "test comment"
                )
            )
        )

        verifier.verify()
    }

}
