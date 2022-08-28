package com.example.demo.gql.datafetchers

import com.example.demo.PostService
import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CompletableFuture

@Component
class PostsQuery(val postService: PostService) : Query {

    @GraphQLDescription("get all posts")
    fun allPosts(): Flow<Post> = postService.allPosts()

    @GraphQLDescription("get post by id")
    suspend fun getPostById(id: UUID): Post = postService.getPostById(id)
}


// loading from dataloaders
@Component
class CommentsDataFetcher : DataFetcher<CompletableFuture<List<Comment>>> {

    override fun get(environment: DataFetchingEnvironment): CompletableFuture<List<Comment>> {
        val postId = environment.getSource<Post>().id
        return environment.getValueFromDataLoader(CommentsDataLoader.name, postId)
    }
}

@Component
class AuthorDataFetcher : DataFetcher<CompletableFuture<Author>> {

    override fun get(environment: DataFetchingEnvironment): CompletableFuture<Author> {
        val authorId = environment.getSource<Post>().authorId
        return environment.getValueFromDataLoader(AuthorsDataLoader.name, authorId)
    }
}