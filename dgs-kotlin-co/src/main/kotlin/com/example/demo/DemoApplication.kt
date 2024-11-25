package com.example.demo

import com.example.demo.model.PostEntity
import com.example.demo.repository.CommentRepository
import com.example.demo.repository.PostRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.stereotype.Component

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Component
class DataInitializer(val posts: PostRepository, val comments: CommentRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(DataInitializer::class.java)
    }

    @EventListener(ApplicationReadyEvent::class)
    suspend fun init() {
        val data = listOf(
            PostEntity(title = "Learn Spring", content = "content of Learn Spring"),
            PostEntity(title = "Learn Dgs framework", content = "content of Learn Dgs framework")
        )

        comments.deleteAll()
        posts.deleteAll()

        val saved = posts.saveAll(data).toList()
        saved.forEach { log.debug("saved: {}", it) }
    }
}

@Configuration
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic {}
            .securityMatcher(PathPatternParserServerWebExchangeMatcher("/graphql"))
            .build()
    }

}
