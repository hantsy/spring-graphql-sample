package com.example.demo

import com.example.demo.gql.DgsConstants
import com.example.demo.gql.types.*
import com.netflix.graphql.dgs.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.CompletableFuture

@DgsComponent
class AuthorsDataFetcher(
    val postService: PostService,
    val authorService: AuthorService
) {

    @DgsQuery
    suspend fun author(@InputArgument authorId: String) = authorService.getAuthorById(authorId)

    @DgsData(parentType = DgsConstants.AUTHOR.TYPE_NAME, field = DgsConstants.AUTHOR.Posts)
    fun posts(dfe: DgsDataFetchingEnvironment): Flow<Post> {
        val a: Author = dfe.getSource()
        return postService.getPostsByAuthorId(a.id)
    }
}

@DgsComponent
class PostsDataFetcher(val postService: PostService) {

    @DgsQuery
    fun allPosts() = postService.allPosts()

    @DgsQuery
    suspend fun postById(@InputArgument postId: String) = postService.getPostById(postId)

    @DgsData(parentType = DgsConstants.POST.TYPE_NAME, field = DgsConstants.POST.Author)
    fun author(dfe: DgsDataFetchingEnvironment): CompletableFuture<Author> {
        val dataLoader = dfe.getDataLoader<String, Author>("authorsLoader")
        val post = dfe.getSource<Post>()
        return dataLoader.load(post.authorId)
    }

    @DgsData(parentType = DgsConstants.POST.TYPE_NAME, field = DgsConstants.POST.Comments)
    fun comments(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Comment>> {
        val dataLoader = dfe.getDataLoader<String, List<Comment>>(CommentsDataLoader::class.java)
        val (id) = dfe.getSource<Post>()
        return dataLoader.load(id)
    }

    @DgsMutation
    suspend// @Secured("ROLE_USER")
    fun createPost(@InputArgument("createPostInput") input: CreatePostInput) = postService.createPost(input)

    @DgsMutation
    suspend// @PreAuthorize("isAuthenticated()")
    fun addComment(@InputArgument("commentInput") input: CommentInput) = postService.addComment(input)

    @DgsSubscription
    // @PreAuthorize("isAuthenticated()")
    fun commentAdded() = postService.commentAdded()
}