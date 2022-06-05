package com.example.demo

import com.example.demo.gql.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.*

class AuthorNotFoundException(id: UUID) : RuntimeException("Author: $id was not found.")
class PostNotFoundException(id: UUID) : RuntimeException("Post: $id was not found.")

@Service
class AuthorService(val authors: AuthorRepository) {

    suspend fun getAuthorById(id: UUID): Author {
        val author = this.authors.findById(id) ?: throw AuthorNotFoundException(id)
        return author.asGqlType()
    }

    // alternative to use kotlin `Flow`
    fun getAuthorByIdIn(ids: List<UUID>): Flow<Author> {
        return authors.findAllById(ids.toList()).map { it.asGqlType() }
    }
}

interface PostService {
    fun allPosts(): Flow<Post>

    suspend fun getPostById(id: UUID): Post
    fun getPostsByAuthorId(id: UUID): Flow<Post>

    suspend fun createPost(postInput: CreatePostInput): Post

    suspend fun addComment(commentInput: CommentInput): Comment

    // subscription: commentAdded
    fun commentAdded(): Flux<Comment>
    fun getCommentsByPostId(id: UUID): Flow<Comment>
    fun getCommentsByPostIdIn(ids: Set<UUID>): Flow<Comment>

    fun getCommentsByPostsIn(ids: Set<Post>): Flow<Comment>
}

@Service
class DefaultPostService(
    val posts: PostRepository,
    val comments: CommentRepository
) : PostService {
    private val log = LoggerFactory.getLogger(PostService::class.java)

    override fun allPosts() = this.posts.findAll().map { it.asGqlType() }

    override suspend fun getPostById(id: UUID): Post {
        val post = this.posts.findById(id) ?: throw PostNotFoundException(id)
        return post.asGqlType()
    }

    override fun getPostsByAuthorId(id: UUID): Flow<Post> {
        return this.posts.findByAuthorId(id)
            .map { it.asGqlType() }
    }

    override suspend fun createPost(postInput: CreatePostInput): Post {
        val data = PostEntity(title = postInput.title, content = postInput.content)
        val saved = this.posts.save(data)
        return saved.asGqlType()
    }

    override suspend fun addComment(commentInput: CommentInput): Comment {
        val postId = commentInput.postId
        if (!this.posts.existsById(postId)) {
            throw PostNotFoundException(postId)
        }
        val data = CommentEntity(content = commentInput.content, postId = postId)
        val savedComment = this.comments.save(data)
        log.debug("Comment saved: $savedComment")
        val comment = savedComment.asGqlType()
        log.debug("converted Gql Comment: $comment")
        sink.emitNext(comment, Sinks.EmitFailureHandler.FAIL_FAST)
        return comment
    }

    val sink = Sinks.many().replay().latest<Comment>()

    // subscription: commentAdded
    override fun commentAdded(): Flux<Comment> = sink.asFlux()

    override fun getCommentsByPostId(id: UUID): Flow<Comment> {
        return this.comments.findByPostId(id)
            .map { it.asGqlType() }
    }

    override fun getCommentsByPostIdIn(ids: Set<UUID>): Flow<Comment> {
        return comments.findByPostIdIn(ids.toList()).map { it.asGqlType() }
    }

    override fun getCommentsByPostsIn(ids: Set<Post>): Flow<Comment> {
        return comments.findByPostIdIn(ids.map { it.id!! }.toList()).map { it.asGqlType() }
    }
}