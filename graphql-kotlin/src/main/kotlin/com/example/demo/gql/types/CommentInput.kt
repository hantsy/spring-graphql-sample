package com.example.demo.gql.types

import java.util.*

data class CommentInput(
    val content: String,
    val postId: UUID
)