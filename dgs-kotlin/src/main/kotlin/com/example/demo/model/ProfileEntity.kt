package com.example.demo.model

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table(value = "profiles")
data class ProfileEntity(
    @Id
    @Column("id")
    var id: UUID? = null,

    @Column("cover_img_id")
    var coverImgId: String,

    @Column("bio")
    var bio: String? = null,

    @CreatedBy
    @Column("user_id")
    var userId: UUID? = null,

    @Column("created_at")
    @CreatedDate
    var createdAt: LocalDateTime? = null,
)