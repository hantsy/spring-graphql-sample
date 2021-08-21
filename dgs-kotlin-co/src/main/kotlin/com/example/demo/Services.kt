package com.example.demo

import com.example.demo.gql.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.*

class AuthorNotFoundException(id: String) : RuntimeException("Author: $id was not found.")
class PostNotFoundException(id: String) : RuntimeException("Post: $id was not found.")

@Service
class AuthorService(val authors: AuthorRepository) {

    suspend fun getAuthorById(id: String): Author = this.authors.findById(UUID.fromString(id))
        .map { it.asGqlType() }
        .awaitFirstOrElse { throw AuthorNotFoundException(id) }

    // alternative to use kotlin co `Flow`
    fun getAuthorByIdIn(ids: List<String>): Flow<Author> {
        val uuids = ids.map { UUID.fromString(it) };
        return authors.findAllById(uuids).map { it.asGqlType() }.asFlow()
    }
}

@Service
class PostService(
    val posts: PostRepository,
    val comments: CommentRepository
) {

    fun allPosts() = this.posts.findAll().map { it.asGqlType() }

    suspend fun getPostById(id: String): Post =
        this.posts.findById(UUID.fromString(id))
            .map { it.asGqlType() }
            .awaitFirstOrElse { throw PostNotFoundException(id) }


    fun getPostsByAuthorId(id: String) = this.posts.findByAuthorId(UUID.fromString(id))
        .map { it.asGqlType() }
        .asFlow()

    suspend fun createPost(postInput: CreatePostInput): Post {
        val data = PostEntity(title = postInput.title, content = postInput.content)
        return this.posts.save(data).map { it.asGqlType() }.awaitSingle()
    }

    suspend fun addComment(commentInput: CommentInput): Comment {
        val postId = UUID.fromString(commentInput.postId)
        return this.posts.findById(postId)
            .flatMap {
                val data = CommentEntity(content = commentInput.content, postId = postId)
                this.comments.save(data)
                    .map { it.asGqlType() }
                    .doOnNext {
                        sink.emitNext(it, Sinks.EmitFailureHandler.FAIL_FAST)
                    }
            }
            .awaitFirstOrElse { throw PostNotFoundException(postId.toString()) }
    }

    val sink = Sinks.many().replay().latest<Comment>()
    fun commentAdded(): Publisher<Comment> = sink.asFlux()

    fun getCommentsByPostId(id: String): Flow<Comment> = this.comments.findByPostId(UUID.fromString(id))
        .map { it.asGqlType() }
        .asFlow()

    fun getCommentsByPostIdIn(ids: Set<String>): Flow<Comment> {
        val uuids = ids.map { UUID.fromString(it) };
        return comments.findByPostIdIn(uuids).map { it.asGqlType() }.asFlow()
    }
}