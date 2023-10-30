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
    fun comments(environment: DataFetchingEnvironment): CompletableFuture<List<Comment>> {
        return environment.getValueFromDataLoader(CommentsDataLoader.NAME, id)
    }

    fun author(environment: DataFetchingEnvironment): CompletableFuture<Author> {
        return environment.getValueFromDataLoader(AuthorsDataLoader.NAME, authorId)
    }
}