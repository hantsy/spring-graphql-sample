package com.example.demo.gql.datafetchers

import com.example.demo.PostService
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CommentInput
import com.example.demo.gql.types.CreatePostInput
import com.example.demo.gql.types.Post
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.springframework.stereotype.Component
import javax.validation.Valid

@Component
class Mutations(val postService: PostService) : Mutation {

    @GraphQLDescription("Create a new post")
    suspend fun createPost(@Valid input: CreatePostInput): Post {
        return postService.createPost(input)
    }

    @GraphQLDescription("Add comment to post")
    suspend fun addComment(@Valid input: CommentInput): Comment {
        return postService.addComment(input)
    }
}