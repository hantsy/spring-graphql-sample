package com.example.demo.repository

import com.example.demo.model.PostEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import java.util.*

interface PostRepository : CoroutineSortingRepository<PostEntity, UUID> ,
    CoroutineCrudRepository<PostEntity, UUID>{
    fun findByAuthorId(id: UUID): Flow<PostEntity>
}