package com.example.demo.repository

import com.example.demo.model.AuthorEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import java.util.*

interface AuthorRepository : CoroutineSortingRepository<AuthorEntity, UUID>,
    CoroutineCrudRepository<AuthorEntity, UUID> {
    suspend fun findOneByNameOrEmail(name: String, email: String): AuthorEntity
}