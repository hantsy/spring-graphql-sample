package com.example.demo.gql.datafetchers;

import com.example.demo.AuthorService
import com.example.demo.PostService
import com.example.demo.gql.types.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asPublisher
import org.reactivestreams.Publisher
import org.springframework.graphql.data.method.annotation.*
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import java.util.*
import javax.validation.Valid

@Controller
@Validated
class PostController(
    private val postService: PostService,
    private val authorService: AuthorService
) {
    // Flow is not supported as return type.
    // see: https://github.com/spring-projects/spring-graphql/issues/393
    @QueryMapping
    suspend fun allPosts(): List<Post> = postService.allPosts().toList()

    @QueryMapping
    suspend fun postById(@Argument postId: UUID) = postService.getPostById(postId)


    @BatchMapping
    suspend fun comments(posts: List<Post>): Map<Post, List<Comment>> {
        val keys = posts.map { it.authorId!! }.toList()
        val comments = postService.getCommentsByPostIdIn(keys).toList()
        val mappedComments = mutableMapOf<Post, List<Comment>>()
        posts.forEach { post ->
            mappedComments[post] = comments.filter { it.postId == post.id }
        }
        return mappedComments
    }

    @BatchMapping
    suspend fun author(posts: List<Post>): List<Author?> {
        val keys = posts.map { it.authorId!! }.toList()
        val authorByIds = authorService.getAuthorByIdIn(keys).toList()
        return keys.map { k -> authorByIds.firstOrNull { author: Author -> author.id == k } }
    }

    @MutationMapping
    suspend fun createPost(@Argument("createPostInput") @Valid input: CreatePostInput): Post {
        return postService.createPost(input)
    }

    @MutationMapping
    suspend fun addComment(@Argument("commentInput") @Valid input: CommentInput): Comment {
        return postService.addComment(input)
    }

    // subscription return type does not support Kotlin Flow
    // see: https://github.com/spring-projects/spring-graphql/issues/393
    @SubscriptionMapping
    fun commentAdded(): Publisher<Comment> {
        return postService.commentAdded().asPublisher()
    }
}