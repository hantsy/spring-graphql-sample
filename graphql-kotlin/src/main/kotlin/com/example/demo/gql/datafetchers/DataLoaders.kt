package com.example.demo.gql.datafetchers

import com.example.demo.AuthorService
import com.example.demo.PostService
import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Comment
import com.expediagroup.graphql.dataloader.KotlinDataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.future
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.Executors

val loaderScope = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())

@Component
class AuthorsDataLoader(val authorService: AuthorService) : KotlinDataLoader<UUID, Author> {
    companion object {
        const val name = "AuthorsDataLoader"
    }

    override val dataLoaderName = name

    override fun getDataLoader(): DataLoader<UUID, Author> {
        return DataLoaderFactory.newDataLoader { keys, environment ->
            loaderScope.future {
                authorService.getAuthorByIdIn(keys).toList()
            }
        }
    }
}


@Component
class CommentsDataLoader(val postService: PostService) : KotlinDataLoader<UUID, List<Comment>> {
    companion object {
        private val log = LoggerFactory.getLogger(CommentsDataLoader::class.java)
        const val name = "CommentsDataLoader"
    }

    override val dataLoaderName = name

    override fun getDataLoader(): DataLoader<UUID, List<Comment>> {
        return DataLoaderFactory.newMappedDataLoader { keys, environment ->
            loaderScope.future {
                val comments = postService.getCommentsByPostIdIn(keys).toList()
                val mappedComments: MutableMap<UUID, List<Comment>> = mutableMapOf()
                keys.forEach { mappedComments[it] = comments.filter { c -> c.postId == it } }
                log.info("mapped comments: {}", mappedComments)
                mappedComments
            }
        }
    }
}