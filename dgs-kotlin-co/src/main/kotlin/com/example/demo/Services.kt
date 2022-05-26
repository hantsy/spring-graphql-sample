package com.example.demo

import com.example.demo.gql.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.*

class AuthorNotFoundException(id: String) : RuntimeException("Author: $id was not found.")
class PostNotFoundException(id: String) : RuntimeException("Post: $id was not found.")

@Service
class AuthorService(val authors: AuthorRepository) {

    suspend fun getAuthorById(id: String): Author {
        val author = this.authors.findById(UUID.fromString(id)) ?: throw AuthorNotFoundException(id)
        return author.asGqlType()
    }

    // alternative to use kotlin co `Flow`
    fun getAuthorByIdIn(ids: List<String>): Flow<Author> {
        val uuids = ids.map { UUID.fromString(it) };
        return authors.findAllById(uuids).map { it.asGqlType() }
    }
}

interface PostService {
    fun allPosts(): Flow<Post>

    suspend fun getPostById(id: String): Post
    fun getPostsByAuthorId(id: String): Flow<Post>

    suspend fun createPost(postInput: CreatePostInput): Post

    suspend fun addComment(commentInput: CommentInput): Comment

    // subscription: commentAdded
    fun commentAdded(): Flux<Comment>
    fun getCommentsByPostId(id: String): Flow<Comment>
    fun getCommentsByPostIdIn(ids: Set<String>): Flow<Comment>
}

@Service
class DefaultPostService(
    val posts: PostRepository,
    val comments: CommentRepository
) : PostService {
    private val log = LoggerFactory.getLogger(PostService::class.java)

    override fun allPosts() = this.posts.findAll().map { it.asGqlType() }

    override suspend fun getPostById(id: String): Post {
        val post = this.posts.findById(UUID.fromString(id)) ?: throw PostNotFoundException(id)
        return post.asGqlType()
    }

    override fun getPostsByAuthorId(id: String): Flow<Post> {
        return this.posts.findByAuthorId(UUID.fromString(id))
            .map { it.asGqlType() }
    }

    override suspend fun createPost(postInput: CreatePostInput): Post {
        val data = PostEntity(title = postInput.title, content = postInput.content)
        val saved = this.posts.save(data)
        return saved.asGqlType()
    }

    override suspend fun addComment(commentInput: CommentInput): Comment {
        val postId = UUID.fromString(commentInput.postId)
        val post = this.posts.findById(postId) ?: throw PostNotFoundException(postId.toString())
        val data = CommentEntity(content = commentInput.content, postId = postId)
        val savedComment = this.comments.save(data)
        val comment = savedComment.asGqlType()
        sink.emitNext(comment, Sinks.EmitFailureHandler.FAIL_FAST)

        return comment
    }

    val sink = Sinks.many().replay().latest<Comment>()

    // subscription: commentAdded
    override fun commentAdded() = sink.asFlux()

    override fun getCommentsByPostId(id: String): Flow<Comment> {
        return this.comments.findByPostId(UUID.fromString(id))
            .map { it.asGqlType() }
    }

    override fun getCommentsByPostIdIn(ids: Set<String>): Flow<Comment> {
        val uuids = ids.map { UUID.fromString(it) };
        return comments.findByPostIdIn(uuids).map { it.asGqlType() }
    }
}