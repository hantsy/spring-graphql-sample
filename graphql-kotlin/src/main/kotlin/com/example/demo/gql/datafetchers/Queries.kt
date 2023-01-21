package com.example.demo.gql.datafetchers

import com.example.demo.service.PostService
import com.example.demo.gql.types.Post
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class PostsQuery(val postService: PostService) : Query {
    companion object {
        private val log = LoggerFactory.getLogger(PostsQuery::class.java)
    }

    @GraphQLDescription("get all posts")
    // Kotlin Flow is not supported.
    // see: https://github.com/ExpediaGroup/graphql-kotlin/issues/1531
    //fun allPosts(): Flow<Post> = postService.allPosts()
    suspend fun allPosts(): List<Post> = postService.allPosts().toList()

    @GraphQLDescription("get post by id")
    suspend fun getPostById(postId: UUID): Post = postService.getPostById(postId)

//    fun comments(postId:UUID, environment: DataFetchingEnvironment): CompletableFuture<List<Comment>> {
////        val postId = environment.getSource<Post>().id
////        log.debug("Fetching comments of post: $postId")
//        return environment.getValueFromDataLoader(CommentsDataLoader.name, postId)
//    }
//
//    fun author(authorId:UUID, environment: DataFetchingEnvironment): CompletableFuture<Author> {
////        val authorId = environment.getSource<Post>().authorId
////        log.debug("Fetching author of post: $authorId")
//        return environment.getValueFromDataLoader(AuthorsDataLoader.name, authorId)
//    }
}
