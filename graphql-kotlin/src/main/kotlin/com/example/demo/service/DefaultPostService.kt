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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

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
        //sink.emitNext(comment, Sinks.EmitFailureHandler.FAIL_FAST)
        flow.emit(comment)
        return comment
    }

    //val sink = Sinks.many().replay().latest<Comment>()
    val flow = MutableSharedFlow<Comment>(replay = 1)

    // subscription: commentAdded
    override fun commentAdded(): Flow<Comment> = flow.asSharedFlow()
    //= sink.asFlux()

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