package com.example.demo.gql.types

import java.time.LocalDateTime
import java.util.*

data class Comment(
    val id: UUID?,
    val content: String,
    val createdAt: LocalDateTime? = null,
    val postId: UUID? = null
)