package com.example.demo.repository

import com.example.demo.model.PostEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface PostRepository : CrudRepository<PostEntity, UUID>,
    PagingAndSortingRepository<PostEntity, UUID> {
    fun findByAuthorId(id: UUID): List<PostEntity>
}