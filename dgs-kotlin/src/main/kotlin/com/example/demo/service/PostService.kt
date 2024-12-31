package com.example.demo.service

import com.example.demo.asGqlType
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CommentInput
import com.example.demo.gql.types.CreatePostInput
import com.example.demo.gql.types.Post
import com.example.demo.model.CommentEntity
import com.example.demo.model.PostEntity
import com.example.demo.repository.CommentRepository
import com.example.demo.repository.PostRepository
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import java.util.*

@Service
class PostService(
    val posts: PostRepository,
    val comments: CommentRepository
) {
    companion object{
        private val log = LoggerFactory.getLogger(PostService::class.java)
    }

    fun allPosts() = this.posts.findAll().map { it.asGqlType() }

    fun getPostById(id: UUID): Post = this.posts.findById(id)
        .map { it.asGqlType() }
        .orElseThrow { PostNotFoundException(id) }

    fun getPostsByAuthorId(id: UUID) = this.posts.findByAuthorId(id).map { it.asGqlType() }

    fun createPost(postInput: CreatePostInput): Post {
        val data = PostEntity(title = postInput.title, content = postInput.content)
        val saved = this.posts.save(data)
        return saved.asGqlType()
    }

    fun addComment(commentInput: CommentInput): Comment {
        val postId = commentInput.postId
        return this.posts.findById(postId)
            .map {
                val data = CommentEntity(content = commentInput.content, postId = postId)
                val saved = this.comments.save(data)
                val comment = saved.asGqlType()
                log.debug("emitting $comment to event `commentAdded`")
                sink.emitNext(comment, Sinks.EmitFailureHandler.FAIL_FAST)
                comment
            }
            .orElseThrow { PostNotFoundException(postId) }
    }

    val sink = Sinks.many().replay().latest<Comment>()
    fun commentAdded(): Publisher<Comment> = sink.asFlux()

    fun getCommentsByPostId(id: UUID): List<Comment> = this.comments.findByPostId(id)
        .map { it.asGqlType() }

    fun getCommentsByPostIdIn(ids: List<UUID>): List<Comment> {
        val authorEntities = comments.findByPostIdIn(ids)
        return authorEntities.map { it.asGqlType() }
    }
}