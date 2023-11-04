package com.example.demo.gql.datafetchers

import com.example.demo.service.AuthorService
import com.example.demo.service.PostService
import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Comment
import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
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
        private const val NAME = "AuthorsDataLoader"
    }

    override val dataLoaderName = NAME
    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<UUID, Author> {
        return DataLoaderFactory.newDataLoader { keys, _ ->
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
        private const val NAME = "CommentsDataLoader"
    }

    override val dataLoaderName = NAME
    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<UUID, List<Comment>> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
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