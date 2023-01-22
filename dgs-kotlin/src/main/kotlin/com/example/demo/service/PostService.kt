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

    fun getPostById(id: String): Post = this.posts.findById(UUID.fromString(id))
        .map { it.asGqlType() }
        .orElseThrow { PostNotFoundException(id) }

    fun getPostsByAuthorId(id: String) = this.posts.findByAuthorId(UUID.fromString(id)).map { it.asGqlType() }

    fun createPost(postInput: CreatePostInput): Post {
        val data = PostEntity(title = postInput.title, content = postInput.content)
        val saved = this.posts.save(data)
        return saved.asGqlType()
    }

    fun addComment(commentInput: CommentInput): Comment {
        val postId = UUID.fromString(commentInput.postId)
        return this.posts.findById(postId)
            .map {
                val data = CommentEntity(content = commentInput.content, postId = postId)
                val saved = this.comments.save(data)
                val comment = saved.asGqlType()
                log.debug("emitting $comment to event `commentAdded`")
                sink.emitNext(comment, Sinks.EmitFailureHandler.FAIL_FAST)
                comment
            }
            .orElseThrow { PostNotFoundException(postId.toString()) }
    }

    val sink = Sinks.many().replay().latest<Comment>()
    fun commentAdded(): Publisher<Comment> = sink.asFlux()

    fun getCommentsByPostId(id: String): List<Comment> = this.comments.findByPostId(UUID.fromString(id))
        .map { it.asGqlType() }

    fun getCommentsByPostIdIn(ids: Set<String>): List<Comment> {
        val uuids = ids.map { UUID.fromString(it) };
        val authorEntities = comments.findByPostIdIn(uuids)
        return authorEntities.map { it.asGqlType() }
    }
}