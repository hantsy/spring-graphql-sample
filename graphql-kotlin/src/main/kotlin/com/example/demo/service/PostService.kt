package com.example.demo.service

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CommentInput
import com.example.demo.gql.types.CreatePostInput
import com.example.demo.gql.types.Post
import kotlinx.coroutines.flow.Flow
import java.util.*

interface PostService {
    fun allPosts(): Flow<Post>

    suspend fun getPostById(id: UUID): Post
    fun getPostsByAuthorId(id: UUID): Flow<Post>

    suspend fun createPost(postInput: CreatePostInput): Post

    suspend fun addComment(commentInput: CommentInput): Comment

    // subscription: commentAdded
    // use Flow instead of Publisher
    fun commentAdded(): Flow<Comment>
    fun getCommentsByPostId(id: UUID): Flow<Comment>
    fun getCommentsByPostIdIn(ids: Set<UUID>): Flow<Comment>

    fun getCommentsByPostsIn(ids: Set<Post>): Flow<Comment>
}