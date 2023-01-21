package com.example.demo

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface AuthorRepository : CrudRepository<AuthorEntity, UUID>,
    PagingAndSortingRepository<AuthorEntity, UUID> {
    fun findByNameOrEmail(name: String, email: String): AuthorEntity?
}

interface ProfileRepository : CrudRepository<ProfileEntity, UUID>,
    PagingAndSortingRepository<ProfileEntity, UUID> {
    fun findByUserId(id: UUID): ProfileEntity?
}

interface CommentRepository : CrudRepository<CommentEntity, UUID>,
    PagingAndSortingRepository<CommentEntity, UUID> {
    fun findByPostId(id: UUID): List<CommentEntity>
    fun findByPostIdIn(uuids: List<UUID>): List<CommentEntity>
}

interface PostRepository : CrudRepository<PostEntity, UUID>,
    PagingAndSortingRepository<PostEntity, UUID> {
    fun findByAuthorId(id: UUID): List<PostEntity>
}