package com.example.demo.gql.datafetchers

import com.example.demo.gql.DgsConstants
import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Post
import com.example.demo.gql.types.Profile
import com.example.demo.service.AuthorService
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.multipart.MultipartFile
import java.util.*

@DgsComponent
class AuthorsDataFetcher(
    val postService: PostService,
    val authorService: AuthorService
) {

    @DgsQuery
    fun author(@InputArgument authorId: UUID) = authorService.getAuthorById(authorId)

    @DgsData(parentType = DgsConstants.AUTHOR.TYPE_NAME, field = DgsConstants.AUTHOR.Posts)
    fun posts(dfe: DgsDataFetchingEnvironment): List<Post> {
        val a: Author = dfe.getSource()!!
        return postService.getPostsByAuthorId(a.id)
    }

//    @DgsMutation
//    @PreAuthorize("isAuthenticated()")
//    fun updateProfile(
//        @InputArgument("bio") bio: String,/* @InputArgument("coverImage") file: MultipartFile*/
//        env: DgsDataFetchingEnvironment
//    ): Profile {
//        val file = env.getArgument<MultipartFile>("coverImage")
//            ?: throw IllegalArgumentException("Upload file is not valid")
//        return authorService.updateProfile(bio, file)
//    }


    @DgsData(parentType = DgsConstants.AUTHOR.TYPE_NAME, field = DgsConstants.AUTHOR.Profile)
    fun profile(dfe: DgsDataFetchingEnvironment): Profile? {
        val a: Author = dfe.getSource()!!
        return authorService.getProfileByUserId(a.id)
    }
}

