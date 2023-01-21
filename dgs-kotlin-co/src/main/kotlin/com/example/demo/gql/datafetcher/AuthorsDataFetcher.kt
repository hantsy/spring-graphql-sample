package com.example.demo.gql.datafetcher

import com.example.demo.gql.DgsConstants
import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Post
import com.example.demo.service.AuthorService
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.*
import kotlinx.coroutines.flow.toList

@DgsComponent
class AuthorsDataFetcher(
    val postService: PostService,
    val authorService: AuthorService
) {

    @DgsQuery
    suspend fun author(@InputArgument authorId: String) = authorService.getAuthorById(authorId)

    @DgsData(parentType = DgsConstants.AUTHOR.TYPE_NAME, field = DgsConstants.AUTHOR.Posts)
    suspend fun posts(dfe: DgsDataFetchingEnvironment): List<Post> {
        val a: Author = dfe.getSource()
        return postService.getPostsByAuthorId(a.id).toList()
    }
}