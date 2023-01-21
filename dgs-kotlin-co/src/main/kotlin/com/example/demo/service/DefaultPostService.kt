package com.example.demo.service

import com.example.demo.model.CommentEntity
import com.example.demo.model.PostEntity
import com.example.demo.asGqlType
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CommentInput
import com.example.demo.gql.types.CreatePostInput
import com.example.demo.gql.types.Post
import com.example.demo.repository.CommentRepository
import com.example.demo.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.*

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
        if (!this.posts.existsById(postId)) {
            throw PostNotFoundException(postId.toString())
        }
        val data = CommentEntity(content = commentInput.content, postId = postId)
        val savedComment = this.comments.save(data)
        val comment = savedComment.asGqlType()
        sink.emitNext(comment, Sinks.EmitFailureHandler.FAIL_FAST)

        return comment
    }

    val sink = Sinks.many().replay().latest<Comment>()

    // subscription: commentAdded
    override fun commentAdded(): Flux<Comment> = sink.asFlux()

    override fun getCommentsByPostId(id: String): Flow<Comment> {
        return this.comments.findByPostId(UUID.fromString(id))
            .map { it.asGqlType() }
    }

    override fun getCommentsByPostIdIn(ids: Set<String>): Flow<Comment> {
        val uuids = ids.map { UUID.fromString(it) };
        return comments.findByPostIdIn(uuids).map { it.asGqlType() }
    }
}