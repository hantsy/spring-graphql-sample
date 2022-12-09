package com.example.demo

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import java.util.UUID

interface AuthorRepository : CoroutineSortingRepository<AuthorEntity, UUID>,
    CoroutineCrudRepository<AuthorEntity, UUID> {
    suspend fun findByNameOrEmail(name: String, email: String): AuthorEntity
}

interface CommentRepository : CoroutineSortingRepository<CommentEntity, UUID>,
    CoroutineCrudRepository<CommentEntity, UUID> {
    fun findByPostId(id: UUID): Flow<CommentEntity>
    fun findByPostIdIn(uuids: List<UUID>): Flow<CommentEntity>
}

interface PostRepository : CoroutineSortingRepository<PostEntity, UUID>,
    CoroutineCrudRepository<PostEntity, UUID> {
    fun findByAuthorId(id: UUID): Flow<PostEntity>
}