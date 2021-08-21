package com.example.demo

import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

interface AuthorRepository : R2dbcRepository<AuthorEntity, UUID> {
    fun findByNameOrEmail(name: String, email: String): Mono<AuthorEntity>
}

//interface ProfileRepository : CrudRepository<ProfileEntity, UUID> {
//    fun findByUserId(id: UUID): ProfileEntity?
//}

interface CommentRepository : R2dbcRepository<CommentEntity, UUID> {
    fun findByPostId(id: UUID): Flux<CommentEntity>
    fun findByPostIdIn(uuids: List<UUID>): Flux<CommentEntity>
}

interface PostRepository : R2dbcRepository<PostEntity, UUID> {
    fun findByAuthorId(id: UUID): Flux<PostEntity>
}