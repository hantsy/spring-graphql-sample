package com.example.demo

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.stereotype.Component

@SpringBootApplication()
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Configuration
@EnableR2dbcAuditing
class DataAuditConfig{}

@Component
class DataInitializer(val posts: PostRepository, val comments: CommentRepository) : ApplicationRunner {
    companion object {
        private val log = LoggerFactory.getLogger(DataInitializer::class.java)
    }

    override fun run(args: ApplicationArguments?) {
        val data = listOf(
            PostEntity(title = "Learn Spring", content = "content of Learn Spring"),
            PostEntity(title = "Learn Dgs framework", content = "content of Learn Dgs framework")
        )
        runBlocking {
            comments.deleteAll()
            posts.deleteAll()
            posts.saveAll(data)
                .map {
                    log.debug("saved: $it")
                }
                .collect()
        }
    }
}