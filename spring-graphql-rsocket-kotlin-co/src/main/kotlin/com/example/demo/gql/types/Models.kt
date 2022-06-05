package com.example.demo.gql.types

import com.example.demo.PostStatus
import java.time.LocalDateTime
import java.util.*

data class Post(
    val id: UUID?,
    val title: String,
    val content: String?,
    val status: PostStatus? = PostStatus.DRAFT,
    val createdAt: LocalDateTime?,
    val author: Author? = null,
    val authorId: UUID? = null,
    val comments: List<Comment>? = emptyList()
)

//enum class PostStatus {
//    DRAFT, PENDING_MODERATION, PUBLISHED;
//}

data class Author(
    val id: UUID?,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime? = null,
    val posts: List<Post>? = emptyList()
)

data class Comment(
    val id: UUID?,
    val content: String,
    val createdAt: LocalDateTime? = null,
    val postId: UUID? = null
)

data class CreatePostInput(
    val title: String,
    val content: String,
)

data class CommentInput(
    val content: String,
    val postId: UUID
)