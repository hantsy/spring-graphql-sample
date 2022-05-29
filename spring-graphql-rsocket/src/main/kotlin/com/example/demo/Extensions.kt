package com.example.demo

import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post

fun PostEntity.asGqlType(): Post = Post(
    id = this.id,
    title = this.title,
    content = this.content,
    status = this.status,
    createdAt = this.createdAt,
    authorId = this.authorId
)

fun CommentEntity.asGqlType(): Comment = Comment(
    id = this.id,
    content = this.content,
    createdAt = this.createdAt,
    postId = this.postId
)

fun AuthorEntity.asGqlType(): Author = Author(
    id = this.id,
    name = this.name,
    email = this.email,
    createdAt = this.createdAt
)