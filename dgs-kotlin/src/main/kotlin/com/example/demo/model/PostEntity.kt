package com.example.demo.model

import org.springframework.data.annotation.*
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

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

    @Column("version")
    @Version
    var version: Long? = null
)