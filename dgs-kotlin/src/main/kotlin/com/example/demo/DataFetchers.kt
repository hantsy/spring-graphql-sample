package com.example.demo

import com.example.demo.gql.DgsConstants
import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CreatePostInput
import com.example.demo.gql.types.Post
import com.netflix.graphql.dgs.*

@DgsComponent
class AuthorsDataFetcher(
    val postService: PostService,
    val authorService: AuthorService
) {

    @DgsQuery
    fun author(@InputArgument authorId: String) = authorService.getAuthorById(authorId)

    @DgsData(parentType = DgsConstants.AUTHOR.TYPE_NAME, field = DgsConstants.AUTHOR.Posts)
    fun posts(dfe: DgsDataFetchingEnvironment): List<Post> {
        val a: Author = dfe.getSource()
        return postService.getPostsByAuthorId(a.id)
    }

}

@DgsComponent
class PostsDataFetcher(
    val postService: PostService,
    val authorService: AuthorService
) {

    @DgsQuery
    fun allPosts() = postService.allPosts()

    @DgsQuery
    fun postById(@InputArgument postId: String) = postService.getPostById(postId)

    @DgsData(parentType = DgsConstants.POST.TYPE_NAME, field = DgsConstants.POST.Author)
    fun author(dfe: DgsDataFetchingEnvironment): Author {
        val post: Post = dfe.getSource()
        val authorId: String = post.authorId!!
        return authorService.getAuthorById(authorId)
    }

    @DgsData(parentType = DgsConstants.POST.TYPE_NAME, field = DgsConstants.POST.Comments)
    fun comments(dfe: DgsDataFetchingEnvironment): List<Comment> {
        val post: Post = dfe.getSource()
        return postService.getCommentsByPostId(post.id)
    }

    @DgsMutation
    fun createPost(@InputArgument("createPostInput") input: CreatePostInput) = postService.createPost(input)
}