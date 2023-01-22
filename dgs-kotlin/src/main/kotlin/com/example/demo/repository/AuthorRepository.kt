package com.example.demo.repository

import com.example.demo.model.AuthorEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface AuthorRepository : CrudRepository<AuthorEntity, UUID>,
    PagingAndSortingRepository<AuthorEntity, UUID> {
    fun findByNameOrEmail(name: String, email: String): AuthorEntity?
}