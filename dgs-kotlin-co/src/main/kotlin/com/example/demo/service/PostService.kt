package com.example.demo.service

import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.CommentInput
import com.example.demo.gql.types.CreatePostInput
import com.example.demo.gql.types.Post
import kotlinx.coroutines.flow.Flow
import reactor.core.publisher.Flux

interface PostService {
    fun allPosts(): Flow<Post>

    suspend fun getPostById(id: String): Post
    fun getPostsByAuthorId(id: String): Flow<Post>

    suspend fun createPost(postInput: CreatePostInput): Post

    suspend fun addComment(commentInput: CommentInput): Comment

    // subscription: commentAdded
    fun commentAdded(): Flux<Comment>
    fun getCommentsByPostId(id: String): Flow<Comment>
    fun getCommentsByPostIdIn(ids: Set<String>): Flow<Comment>
}