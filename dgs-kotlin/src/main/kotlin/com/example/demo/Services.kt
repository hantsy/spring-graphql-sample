package com.example.demo

import com.example.demo.gql.types.*
import org.reactivestreams.Publisher
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Sinks
import java.util.*

class AuthorNotFoundException(id: String) : RuntimeException("Author: $id was not found.")
class PostNotFoundException(id: String) : RuntimeException("Post: $id was not found.")

@Service
class AuthorService(val authors: AuthorRepository, val profiles: ProfileRepository, val gridFsTemplate: GridFsTemplate) {

    fun getAuthorById(id: String): Author = this.authors.findById(UUID.fromString(id))
        .map { it.asGqlType() }
        .orElseThrow { AuthorNotFoundException(id) }

    fun getAuthorByIdIn(ids: List<String>): List<Author> {
        val uuids = ids.map { UUID.fromString(it) };
        val authorEntities = authors.findAllById(uuids)
        return authorEntities.map { it.asGqlType() }
    }

    fun updateProfile(bio: String, coverImage: MultipartFile): Profile {
        val objectId = gridFsTemplate.store(coverImage.inputStream, coverImage.originalFilename, coverImage.contentType)
            .toHexString();
        return profiles.save(ProfileEntity(coverImgId = objectId, bio = bio)).asGqlType()
    }

    fun getProfileByUserId(id: String): Profile? {
        return profiles.findByUserId(UUID.fromString(id))?.asGqlType()
    }
}

@Service
class PostService(
    val posts: PostRepository,
    val comments: CommentRepository
) {

    fun allPosts() = this.posts.findAll().map { it.asGqlType() }

    fun getPostById(id: String): Post = this.posts.findById(UUID.fromString(id))
        .map { it.asGqlType() }
        .orElseThrow { PostNotFoundException(id) }

    fun getPostsByAuthorId(id: String) = this.posts.findByAuthorId(UUID.fromString(id)).map { it.asGqlType() }

    fun createPost(postInput: CreatePostInput): Post {
        val data = PostEntity(title = postInput.title, content = postInput.content)
        val saved = this.posts.save(data)
        return saved.asGqlType()
    }

    fun addComment(commentInput: CommentInput): Comment {
        val postId = UUID.fromString(commentInput.postId)
        return this.posts.findById(postId)
            .map {
                val data = CommentEntity(content = commentInput.content, postId = postId)
                val saved = this.comments.save(data)
                val comment = saved.asGqlType()
                sink.emitNext(comment, Sinks.EmitFailureHandler.FAIL_FAST)
                comment
            }
            .orElseThrow { PostNotFoundException(postId.toString()) }
    }

    val sink = Sinks.many().replay().latest<Comment>()
    fun commentAdded(): Publisher<Comment> = sink.asFlux()

    fun getCommentsByPostId(id: String): List<Comment> = this.comments.findByPostId(UUID.fromString(id))
        .map { it.asGqlType() }

    fun getCommentsByPostIdIn(ids: Set<String>): List<Comment> {
        val uuids = ids.map { UUID.fromString(it) };
        val authorEntities = comments.findByPostIdIn(uuids)
        return authorEntities.map { it.asGqlType() }
    }
}