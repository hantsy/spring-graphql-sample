package com.example.demo.service

import com.example.demo.asGqlType
import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Profile
import com.example.demo.model.ProfileEntity
import com.example.demo.repository.AuthorRepository
import com.example.demo.repository.ProfileRepository
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class AuthorService(val authors: AuthorRepository, val profiles: ProfileRepository, val gridFsTemplate: GridFsTemplate) {

    fun getAuthorById(id: UUID): Author = this.authors.findById(id)
        .map { it.asGqlType() }
        .orElseThrow { AuthorNotFoundException(id) }

    fun getAuthorByIdIn(ids: List<UUID>): List<Author> {
        val authorEntities = authors.findAllById(ids)
        return authorEntities.map { it.asGqlType() }
    }

    fun updateProfile(bio: String, coverImage: MultipartFile): Profile {
        val objectId = gridFsTemplate.store(coverImage.inputStream, coverImage.originalFilename, coverImage.contentType)
            .toHexString();
        return profiles.save(ProfileEntity(coverImgId = objectId, bio = bio)).asGqlType()
    }

    fun getProfileByUserId(id: UUID): Profile? {
        return profiles.findByUserId(id)?.asGqlType()
    }
}