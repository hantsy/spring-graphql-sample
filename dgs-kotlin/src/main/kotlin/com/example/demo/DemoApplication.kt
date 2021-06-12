package com.example.demo

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.util.*
import java.util.stream.IntStream

@SpringBootApplication
@EnableJdbcAuditing
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args) {
        addInitializers(beans)
    }
}

val beans = beans {
    bean {
        ApplicationRunner {
            println("start data initialization...")
            val posts = ref<PostRepository>()
            val comments = ref<CommentRepository>()
            val authors = ref<AuthorRepository>()

            comments.deleteAll()
            posts.deleteAll()
            authors.deleteAll()

            val author = AuthorEntity(name = "user", email = "user@example.com")
            val savedAuthor = authors.save(author)
            for (i in 1..10) {
                val postData = PostEntity(
                    title = "Dgs post #$i",
                    content = "Content of post #$i",
                    authorId = savedAuthor.id
                )
                val savedPost = posts.save(postData);
                IntStream.range(1, Random().nextInt(5) + 1)
                    .mapToObj { CommentEntity(content = "comment # $it", postId = savedPost.id) }
                    .forEach { comments.save(it) }
            }

            posts.findAll().forEach { println("post: $it") }
            comments.findAll().forEach { println("comment: $it") }
            authors.findAll().forEach { println("author: $it") }
            println("done data initialization...")
        }
    }

    profile("cors") {
        bean("corsFilter") {

            //val config = CorsConfiguration().apply {
            // allowedOrigins = listOf("http://allowed-origin.com")
            // maxAge = 8000L
            // addAllowedMethod("PUT")
            // addAllowedHeader("X-Allowed")
            //}

            val config = CorsConfiguration().applyPermitDefaultValues()

            val source = UrlBasedCorsConfigurationSource().apply {
                registerCorsConfiguration("/**", config)
            }

            CorsFilter(source)
        }
    }

}