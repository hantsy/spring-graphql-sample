package com.example.demo.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

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