package com.example.demo.gql.types

import com.example.demo.gql.datafetchers.AuthorsDataLoader
import com.example.demo.gql.datafetchers.CommentsDataLoader
import com.example.demo.gql.directives.UpperCase
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import graphql.schema.DataFetchingEnvironment
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture

data class Post(
    val id: UUID?,
    @UpperCase
    val title: String,
    val content: String?,
    val status: String? = null,
    val createdAt: LocalDateTime?,
    val authorId: UUID? = null,
) {
    lateinit var author: Author
    lateinit var comments: List<Comment>
//    fun getComments(environment: DataFetchingEnvironment): CompletableFuture<List<Comment>> {
//        return environment.getValueFromDataLoader(CommentsDataLoader.name, id)
//    }
//
//    fun getAuthor(environment: DataFetchingEnvironment): CompletableFuture<Author> {
//        return environment.getValueFromDataLoader(AuthorsDataLoader.name, authorId)
//    }
}

//enum class PostStatus {
//    DRAFT, PENDING_MODERATION, PUBLISHED;
//}

data class Author(
    val id: UUID?,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime? = null,
    val posts: List<Post>? = emptyList()
)

data class Comment(
    val id: UUID?,
    val content: String,
    val createdAt: LocalDateTime? = null,
    val postId: UUID? = null
)

data class CreatePostInput(
    val title: String,
    val content: String,
)

data class CommentInput(
    val content: String,
    val postId: UUID
)