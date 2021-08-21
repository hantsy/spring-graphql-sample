package com.example.demo

import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post

fun PostEntity.asGqlType(): Post = Post(
    id = this.id!!.toString(),
    title = this.title,
    content = this.content,
    createdAt = this.createdAt,
    authorId = this.authorId.toString()
)

fun CommentEntity.asGqlType(): Comment = Comment(
    id = this.id!!.toString(),
    content = this.content,
    createdAt = this.createdAt,
    postId = this.postId!!.toString()
)

fun AuthorEntity.asGqlType(): Author = Author(
    id = this.id!!.toString(),
    name = this.name,
    email = this.email,
    createdAt = this.createdAt
)