package com.example.demo.repository

import com.example.demo.model.CommentEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface CommentRepository : CrudRepository<CommentEntity, UUID>,
    PagingAndSortingRepository<CommentEntity, UUID> {
    fun findByPostId(id: UUID): List<CommentEntity>
    fun findByPostIdIn(uuids: List<UUID>): List<CommentEntity>
}