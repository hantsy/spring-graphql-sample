package com.example.demo

import com.example.demo.SubscriptionTests.SubscriptionTestsConfig
import com.example.demo.gql.datafetcher.AuthorsDataFetcher
import com.example.demo.gql.scalar.LocalDateTimeScalar
import com.example.demo.gql.datafetcher.PostsDataFetcher
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CommentInput
import com.example.demo.model.CommentEntity
import com.example.demo.repository.CommentRepository
import com.example.demo.repository.PostRepository
import com.example.demo.service.AuthorService
import com.example.demo.service.DefaultPostService
import com.example.demo.service.PostService
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor
import com.netflix.graphql.dgs.webflux.autoconfiguration.DgsWebFluxAutoConfiguration
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import graphql.ExecutionResult
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(classes = [SubscriptionTestsConfig::class])
class SubscriptionTests {
    companion object {
        private val log = LoggerFactory.getLogger(SubscriptionTests::class.java)
    }

    @Configuration
    @Import(
        value = [
            AuthorsDataFetcher::class,
            PostsDataFetcher::class,
            LocalDateTimeScalar::class,
            DefaultPostService::class,
        ]
    )
    @ImportAutoConfiguration(
        value = [
            DgsWebFluxAutoConfiguration::class,
            DgsAutoConfiguration::class,
            WebFluxAutoConfiguration::class
        ]
    )
    class SubscriptionTestsConfig

    @Autowired
    lateinit var dgsQueryExecutor: DgsReactiveQueryExecutor

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var postRepository: PostRepository

    @MockkBean
    lateinit var commentRepository: CommentRepository

    @SpykBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var  authorService: AuthorService

    @Test
    fun `test subscriptions`() = runTest {
        val subscriptionQuery = """
            subscription onCommentAdded { 
                commentAdded { 
                    id 
                    postId 
                    content 
                } 
            }
        """.trimIndent()
        val subscription = dgsQueryExecutor.execute(subscriptionQuery, emptyMap()).awaitSingle()
        val commentsPublisher = subscription.getData<Publisher<ExecutionResult>>()

        val comments = CopyOnWriteArrayList<Comment>()
        commentsPublisher.subscribe(object : Subscriber<ExecutionResult> {
            override fun onSubscribe(s: Subscription) {
                s.request(2)
            }

            override fun onNext(t: ExecutionResult) {
                val data = t.getData<Map<String, Any>>()
                val comment = objectMapper.convertValue(data["commentAdded"], Comment::class.java)
                comments.add(comment)
                log.info("Received comment: $comment")
            }

            override fun onError(t: Throwable) {
                log.error("Error", t)
            }

            override fun onComplete() {
                log.info("Subscription completed")
            }
        })


        coEvery { postRepository.existsById(any()) } returns true
        coEvery { commentRepository.save(any()) } returns
                CommentEntity(
                    id = UUID.randomUUID(),
                    postId = UUID.randomUUID(),
                    content = "Comment 1",
                    createdAt = LocalDateTime.now()
                )

        postService.addComment(
            CommentInput(
                postId = UUID.randomUUID().toString(),
                content = "Comment 1"
            )
        )

        postService.addComment(
            CommentInput(
                postId = UUID.randomUUID().toString(),
                content = "Comment 1"
            )
        )

        comments.size shouldBe 2

        // verify mock callings
        coVerify(exactly = 2) { postRepository.existsById(any()) }
        coVerify(exactly = 2) { commentRepository.save(any()) }
        coVerify(atLeast = 2) { postService.addComment(any())}
    }
}