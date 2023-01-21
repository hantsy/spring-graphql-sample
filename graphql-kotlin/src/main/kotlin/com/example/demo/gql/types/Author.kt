package com.example.demo.gql.types

import java.time.LocalDateTime
import java.util.*

data class Author(
    val id: UUID?,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime? = null,
    val posts: List<Post>? = emptyList()
)