package com.example.demo.service

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CommentInput
import com.example.demo.gql.types.CreatePostInput
import com.example.demo.gql.types.Post
import kotlinx.coroutines.flow.Flow
import reactor.core.publisher.Flux
import java.util.UUID

interface PostService {
    fun allPosts(): Flow<Post>

    suspend fun getPostById(id: UUID): Post
    fun getPostsByAuthorId(id: UUID): Flow<Post>

    suspend fun createPost(postInput: CreatePostInput): Post

    suspend fun addComment(commentInput: CommentInput): Comment

    // subscription: commentAdded
    fun commentAdded(): Flow<Comment>
    fun getCommentsByPostId(id: UUID): Flow<Comment>
    fun getCommentsByPostIdIn(ids: List<UUID>): Flow<Comment>
}