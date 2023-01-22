package com.example.demo.gql.datafetchers

import com.example.demo.gql.DgsConstants
import com.example.demo.gql.dataloaders.CommentsDataLoader
import com.example.demo.gql.types.*
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.*
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
import java.util.concurrent.CompletableFuture

@DgsComponent
class PostsDataFetcher(val postService: PostService) {

    @DgsQuery
    fun allPosts() = postService.allPosts()

    @DgsQuery
    fun postById(@InputArgument postId: String) = postService.getPostById(postId)

    @DgsData(parentType = DgsConstants.POST.TYPE_NAME, field = DgsConstants.POST.Author)
    fun author(dfe: DgsDataFetchingEnvironment): CompletableFuture<Author> {
        val dataLoader = dfe.getDataLoader<String, Author>("authorsLoader")
        val post = dfe.getSource<Post>()
        return dataLoader.load(post.authorId)
    }

    @DgsData(parentType = DgsConstants.POST.TYPE_NAME, field = DgsConstants.POST.Comments)
    fun comments(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Comment>> {
        val dataLoader = dfe.getDataLoader<String, List<Comment>>(
            CommentsDataLoader::class.java
        )
        val (id) = dfe.getSource<Post>()
        return dataLoader.load(id)
    }

    @DgsMutation
    @Secured("ROLE_USER")
    fun createPost(@InputArgument("createPostInput") input: CreatePostInput) = postService.createPost(input)

    @DgsMutation
    @PreAuthorize("isAuthenticated()")
    fun addComment(@InputArgument("commentInput") input: CommentInput) = postService.addComment(input)

    @DgsSubscription
    //@PreAuthorize("isAuthenticated()")
    fun commentAdded() = postService.commentAdded()
}