package com.example.demo.repository

import com.example.demo.model.CommentEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import java.util.*

interface CommentRepository : CoroutineSortingRepository<CommentEntity, UUID>,
    CoroutineCrudRepository<CommentEntity, UUID> {
    fun findByPostId(id: UUID): Flow<CommentEntity>
    fun findByPostIdIn(uuids: List<UUID>): Flow<CommentEntity>
}