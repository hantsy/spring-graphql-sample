package com.example.demo.repository

import com.example.demo.model.ProfileEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface ProfileRepository : CrudRepository<ProfileEntity, UUID>,
    PagingAndSortingRepository<ProfileEntity, UUID> {
    fun findByUserId(id: UUID): ProfileEntity?
}