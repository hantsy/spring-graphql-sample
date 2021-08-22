package com.example.demo

import com.example.demo.gql.DgsConstants
import com.example.demo.gql.types.*
import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.internal.DgsWebMvcRequestData
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.CompletableFuture
import javax.servlet.http.HttpSession

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

    @DgsMutation
    @PreAuthorize("isAuthenticated()")
    fun updateProfile(@InputArgument("bio") bio: String, @InputArgument("coverImage") file: MultipartFile) =
        authorService.updateProfile(bio, file)

    @DgsData(parentType = DgsConstants.AUTHOR.TYPE_NAME, field = DgsConstants.AUTHOR.Profile)
    fun profile(dfe: DgsDataFetchingEnvironment): Profile? {
        val a: Author = dfe.getSource()
        return authorService.getProfileByUserId(a.id)
    }
}

@DgsComponent
class AuthDataFetcher(
    val authenticationManager: AuthenticationManager
) {

    @DgsMutation
    fun signIn(@InputArgument credentials: Credentials, dfe: DgsDataFetchingEnvironment): Map<String, Any> {
        var auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                credentials.username,
                credentials.password
            )
        )
        SecurityContextHolder.getContext().authentication = auth

        // get the session id from redis.
        val req = dfe.getDgsContext().requestData as DgsWebMvcRequestData
        val session = req.webRequest?.sessionMutex as HttpSession
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        )
        return mapOf(
            "name" to auth.principal.toString(),
            "roles" to auth.authorities.map { it.authority },
            "token" to session.id
        )
    }

    @DgsMutation
    @PreAuthorize("isAuthenticated()")
    fun logout(dfe: DgsDataFetchingEnvironment): Boolean {
        val req = dfe.getDgsContext().requestData as DgsWebMvcRequestData
        val session = req.webRequest?.sessionMutex as HttpSession?
        session?.invalidate()
        return true
    }
}

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
    @PreAuthorize("isAuthenticated()")
    fun commentAdded() = postService.commentAdded()
}