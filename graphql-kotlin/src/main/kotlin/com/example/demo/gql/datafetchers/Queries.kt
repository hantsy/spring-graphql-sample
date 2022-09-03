package com.example.demo.gql.datafetchers

import com.example.demo.PostService
import com.example.demo.gql.types.Post
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.util.*

@Component
class PostsQuery(val postService: PostService) : Query {

    @GraphQLDescription("get all posts")
    // Kotlin Flow is not supported.
    // see: https://github.com/ExpediaGroup/graphql-kotlin/issues/1531
    //fun allPosts(): Flow<Post> = postService.allPosts()
    suspend fun allPosts(): List<Post> = postService.allPosts().toList()

    @GraphQLDescription("get post by id")
    suspend fun getPostById(id: UUID): Post = postService.getPostById(id)
}
