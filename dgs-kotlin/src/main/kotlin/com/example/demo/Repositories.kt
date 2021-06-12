package com.example.demo

import org.springframework.data.repository.CrudRepository
import java.util.*

interface AuthorRepository : CrudRepository<AuthorEntity, UUID> {
}

interface CommentRepository : CrudRepository<CommentEntity, UUID> {
    fun findByPostId(id: UUID): List<CommentEntity>
}

interface PostRepository : CrudRepository<PostEntity, UUID> {
    fun findByAuthorId(id: UUID): List<PostEntity>
}