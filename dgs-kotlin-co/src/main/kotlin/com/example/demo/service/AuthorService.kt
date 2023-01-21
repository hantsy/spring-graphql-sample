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

    suspend fun getAuthorById(id: String): Author {
        val author = this.authors.findById(UUID.fromString(id)) ?: throw AuthorNotFoundException(id)
        return author.asGqlType()
    }

    // alternative to use kotlin co `Flow`
    fun getAuthorByIdIn(ids: List<String>): Flow<Author> {
        val uuids = ids.map { UUID.fromString(it) };
        return authors.findAllById(uuids).map { it.asGqlType() }
    }
}