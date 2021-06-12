package com.example.demo

import com.example.demo.gql.types.*
import org.springframework.stereotype.Service
import java.util.*

class AuthorNotFoundException(id: String) : RuntimeException("Author: $id was not found.")
class PostNotFoundException(id: String) : RuntimeException("Post: $id was not found.")

@Service
class AuthorService(val authors: AuthorRepository) {

    fun getAuthorById(id: String): Author = this.authors.findById(UUID.fromString(id))
        .map { it.asGqlType() }
        .orElseThrow { AuthorNotFoundException(id) }

}

@Service
class PostService(
    val posts: PostRepository,
    val comments: CommentRepository
) {

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
                saved.asGqlType()
            }
            .orElseThrow { PostNotFoundException(postId.toString()) }
    }

    fun getCommentsByPostId(id: String): List<Comment> = this.comments.findByPostId(UUID.fromString(id))
        .map { it.asGqlType() }
}