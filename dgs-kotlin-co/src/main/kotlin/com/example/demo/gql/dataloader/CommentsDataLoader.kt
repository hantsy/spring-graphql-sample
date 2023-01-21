package com.example.demo.gql.dataloader

import com.example.demo.gql.types.Comment
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.DgsDataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.future
import org.dataloader.MappedBatchLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors

@DgsDataLoader(name = "commentsLoader")
class CommentsDataLoader(val postService: PostService) : MappedBatchLoader<String, List<Comment>> {
    val loaderScope = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())
    companion object {
        val log: Logger = LoggerFactory.getLogger(CommentsDataLoader::class.java)
    }

    override fun load(keys: Set<String>): CompletionStage<Map<String, List<Comment>>> = loaderScope.future {
        val comments = postService.getCommentsByPostIdIn(keys).toList()
        val mappedComments: MutableMap<String, List<Comment>> = mutableMapOf()
        keys.forEach { mappedComments[it] = comments.filter { c -> c.postId == it } }
        log.info("mapped comments: {}", mappedComments)
        mappedComments
    }
}