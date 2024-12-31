package com.example.demo.gql.dataloaders

import com.example.demo.gql.types.Comment
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@DgsDataLoader(name = "comments")
class CommentsDataLoader(val postService: PostService) : MappedBatchLoader<UUID, List<Comment>> {

    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, List<Comment>>> {
        val comments = postService.getCommentsByPostIdIn(keys.toList())
        val mappedComments: MutableMap<UUID, List<Comment>> = hashMapOf()
        keys.forEach {
            mappedComments[it] = comments.filter { (_, _, postId) -> postId == it }
        }
        log.info("mapped comments: {}", mappedComments)
        return CompletableFuture.completedFuture(mappedComments)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(CommentsDataLoader::class.java)
    }
}