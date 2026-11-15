package com.example.demo

import com.example.demo.model.AuthorEntity
import com.example.demo.model.CommentEntity
import com.example.demo.model.PostEntity
import com.example.demo.repository.AuthorRepository
import com.example.demo.repository.CommentRepository
import com.example.demo.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.*
import java.util.stream.IntStream

@Component
class DataInitializer(
    private val posts: PostRepository,
    private val comments: CommentRepository,
    private val authors: AuthorRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(DataInitializer::class.java)
    }

    override fun run(args: ApplicationArguments) {
        log.info("start data initialization...")

        comments.deleteAll()
        posts.deleteAll()
        authors.deleteAll()

        val author = AuthorEntity(
            name = "user",
            password = passwordEncoder.encode("password")!!,
            email = "user@example.com"
        )
        val savedAuthor = authors.save(author)
        log.info("saved author: {}", savedAuthor)
//            for (i in 1..10) {
//                val postData = PostEntity(
//                    title = "Dgs post #$i",
//                    content = "Content of post #$i",
//                    authorId = savedAuthor.id
//                )
//                val savedPost = posts.save(postData);
//                IntStream.range(1, Random().nextInt(5) + 1)
//                    .mapToObj { CommentEntity(content = "comment # $it", postId = savedPost.id) }
//                    .forEach { comments.save(it) }
//            }
//
//            posts.findAll().forEach { log.info("post: {}", it) }
//            comments.findAll().forEach { log.info("comment: {}", it) }
//            authors.findAll().forEach { log.info("author: {}", it) }
        log.info("done data initialization...")
    }
}
