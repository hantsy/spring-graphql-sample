package com.example.demo

import com.example.demo.gql.ExceptionHandlers
import com.example.demo.gql.datafetchers.PostsDataFetcher
import com.example.demo.gql.scalars.LocalDateTimeScalar
import com.example.demo.gql.scalars.UUIDScalar
import com.example.demo.gql.types.CommentInput
import com.example.demo.model.CommentEntity
import com.example.demo.model.PostEntity
import com.example.demo.repository.CommentRepository
import com.example.demo.repository.PostRepository
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import graphql.ExecutionResult
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * GraphQL execution behaviour of the subscription, executed in-process through `DgsQueryExecutor`
 * with the repositories mocked out. See `IntegrationTests` for the transport level behaviour.
 */
@SpringBootTest(classes = [SubscriptionTests.SubscriptionTestsConfig::class])
@EnableDgsTest
class SubscriptionTests {

    @Configuration
    @Import(
        value = [
            PostsDataFetcher::class,
            PostService::class,
            ExceptionHandlers::class,
            UUIDScalar::class,
            LocalDateTimeScalar::class,
        ]
    )
    class SubscriptionTestsConfig

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockkBean
    lateinit var postRepository: PostRepository

    @MockkBean
    lateinit var commentRepository: CommentRepository

    // `PostService` is a spy, so the real implementation runs against the mocked repositories.
    @MockkSpyBean
    lateinit var postService: PostService

    @Test
    fun `test subscriptions`() {
        val subscriptionQuery = """
            subscription onCommentAdded {
                commentAdded {
                    id
                    postId
                    content
                }
            }
        """.trimIndent()

        val subscription = dgsQueryExecutor.execute(subscriptionQuery, emptyMap())
        val commentsPublisher = subscription.getData<Publisher<ExecutionResult>>()

        val comments = CopyOnWriteArrayList<Map<String, Any>>()
        commentsPublisher.subscribe(object : Subscriber<ExecutionResult> {
            override fun onSubscribe(s: Subscription) {
                s.request(2)
            }

            override fun onNext(t: ExecutionResult) {
                val data = t.getData<Map<String, Any>>()
                comments.add(data["commentAdded"] as Map<String, Any>)
            }

            override fun onError(t: Throwable) {
                throw t
            }

            override fun onComplete() {
                // no-op
            }
        })

        val postId = UUID.randomUUID()
        every { postRepository.findById(postId) } returns Optional.of(
            PostEntity(id = postId, title = "test title", content = "test content")
        )
        every { commentRepository.save(any()) } answers {
            val comment = firstArg<CommentEntity>()
            comment.copy(id = UUID.randomUUID(), createdAt = LocalDateTime.now())
        }

        postService.addComment(
            CommentInput(
                postId = postId,
                content = "Comment 1"
            )
        )

        postService.addComment(
            CommentInput(
                postId = postId,
                content = "Comment 2"
            )
        )

        comments.size shouldBe 2
        comments.map { it["content"] } shouldBe listOf("Comment 1", "Comment 2")

        // verify the invocation of the mocks.
        verify(exactly = 2) { postService.addComment(any()) }
        verify(exactly = 2) { postRepository.findById(postId) }
        verify(exactly = 2) { commentRepository.save(any()) }
    }
}
