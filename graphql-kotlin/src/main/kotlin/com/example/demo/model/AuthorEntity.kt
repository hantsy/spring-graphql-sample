package com.example.demo.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
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