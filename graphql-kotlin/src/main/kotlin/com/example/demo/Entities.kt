package com.example.demo

import org.springframework.data.annotation.*
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table(value = "users")
data class AuthorEntity(
    @Id
    @Column("id")
    var id: UUID? = null,

    @Column("name")
    var name: String,

    @Column("email")
    var email: String,

    @Column("created_at")
    @CreatedDate
    var createdAt: LocalDateTime? = null,
)

@Table(value = "comments")
data class CommentEntity(
    @Id
    @Column("id")
    var id: UUID? = null,

    @Column("content")
    var content: String,

    @Column("post_id")
    var postId: UUID? = null,

    @Column("created_at")
    @CreatedDate
    var createdAt: LocalDateTime? = null
)

@Table(value = "posts")
data class PostEntity(
    @Id
    @Column("id")
    var id: UUID? = null,

    @Column("title")
    var title: String,

    @Column("content")
    var content: String? = null,

    @Column("status")
    var status: PostStatus = PostStatus.DRAFT,

    @CreatedBy
    @Column("author_id")
    var authorId: UUID? = null,

    @Column("created_at")
    @CreatedDate
    var createdAt: LocalDateTime? = null,

    @Column("updated_at")
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)

enum class PostStatus {
    DRAFT, PENDING_MODERATION, PUBLISHED
}