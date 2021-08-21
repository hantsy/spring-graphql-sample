package com.example.demo

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class DemoApplication {
    private val log = LoggerFactory.getLogger(DemoApplication::class.java)

    @Bean
    fun dataInitializer(posts: PostRepository, comments: CommentRepository) = ApplicationRunner {
        val data = listOf(
            PostEntity(title = "Learn Spring", content = "content of Learn Spring"),
            PostEntity(title = "Learn Dgs framework", content = "content of Learn Dgs framework")
        )
        runBlocking {
            comments.deleteAll().awaitFirstOrNull()
            posts.deleteAll().awaitFirstOrNull()
            val saved = posts.saveAll(data)
                .asFlow()
                .toList()
            saved.forEach { log.debug("saved: {}", it) }
        }

    }

}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
