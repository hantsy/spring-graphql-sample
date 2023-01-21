package com.example.demo.service

import com.example.demo.asGqlType
import com.example.demo.gql.types.Author
import com.example.demo.repository.AuthorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthorService(val authors: AuthorRepository) {

    suspend fun getAuthorById(id: UUID): Author {
        val author = this.authors.findById(id) ?: throw AuthorNotFoundException(id)
        return author.asGqlType()
    }

    // alternative to use kotlin `Flow`
    fun getAuthorByIdIn(ids: List<UUID>): Flow<Author> {
        return authors.findAllById(ids.toList()).map { it.asGqlType() }
    }
}