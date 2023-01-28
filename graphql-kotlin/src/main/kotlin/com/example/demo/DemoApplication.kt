package com.example.demo

import com.example.demo.model.AuthorEntity
import com.example.demo.model.CommentEntity
import com.example.demo.model.PostEntity
import com.example.demo.repository.AuthorRepository
import com.example.demo.repository.CommentRepository
import com.example.demo.repository.PostRepository
import com.expediagroup.graphql.server.spring.GraphQLAutoConfiguration
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.stereotype.Component

@SpringBootApplication()
// GraphQL kotlin autoconfig does not work in Spring Boot 3.0, fixed in 7.0.x
// see: https://github.com/ExpediaGroup/graphql-kotlin/issues/1609
// @ImportAutoConfiguration(GraphQLAutoConfiguration::class)
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Configuration
@EnableR2dbcAuditing
class DataAuditConfig {}

@Component
class DataInitializer(
    val posts: PostRepository,
    val authors: AuthorRepository,
    val comments: CommentRepository
) : ApplicationRunner {
    companion object {
        private val log = LoggerFactory.getLogger(DataInitializer::class.java)
    }

    override fun run(args: ApplicationArguments?) {
        runBlocking {
            comments.deleteAll()
            posts.deleteAll()
            authors.deleteAll()

            val author = authors.save(AuthorEntity(name = "Foo bar", email = "foo@example.com"))
            val data = listOf(
                PostEntity(title = "Learn Spring", content = "content of Learn Spring", authorId = author.id),
                PostEntity(
                    title = "Learn Dgs framework",
                    content = "content of Learn Dgs framework",
                    authorId = author.id
                )
            )

            posts.saveAll(data)
                .onEach {
                    (1..10).onEach { commentIt ->
                        comments.save(CommentEntity(postId = it.id, content = "comments $commentIt of ${it.id}"))
                    }
                    log.debug("saved: $it")
                }
                .collect()
        }
    }
}