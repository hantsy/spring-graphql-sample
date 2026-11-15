package com.example.demo

import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient
import com.netflix.graphql.dgs.client.ErrorType
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
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.net.URI
import java.time.Duration

/**
 * Verifies the error type a client actually receives for an unauthenticated mutation.
 *
 * Over the wire the request goes through the Spring Security filter chain, which puts an
 * anonymous `Authentication` into the `SecurityContext`. Method security (`@Secured`) then
 * rejects the call with an `AccessDeniedException`, which DGS maps to PERMISSION_DENIED.
 *
 * The in-process `DgsQueryExecutor` (see `QueryTests`) skips that filter chain, so the
 * `SecurityContext` stays empty and Spring Security throws
 * `AuthenticationCredentialsNotFoundException` instead. DGS' default exception handler only
 * recognises `AccessDeniedException`, so that path reports the error as INTERNAL.
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

    lateinit var socketClient: WebSocketGraphQlClient

    @BeforeEach
    fun setup() {
        restClient = DgsRestClientGraphQLClient(
            RestClient.builder()
                .baseUrl("http://localhost:$port/graphql")
                .build()
        )

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
}
