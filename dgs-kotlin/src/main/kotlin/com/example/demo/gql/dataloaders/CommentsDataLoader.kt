package com.example.demo.gql.dataloaders

import com.example.demo.gql.types.Comment
import com.example.demo.service.PostService
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@DgsDataLoader(name = "comments")
class CommentsDataLoader(val postService: PostService) : MappedBatchLoader<String, List<Comment>> {

    override fun load(keys: Set<String>): CompletionStage<Map<String, List<Comment>>> {
        val comments = postService.getCommentsByPostIdIn(keys)
        val mappedComments: MutableMap<String, List<Comment>> = hashMapOf()
        keys.forEach {
            mappedComments[it] = comments.filter { (_, _, postId) -> postId == it }
        }
        log.info("mapped comments: {}", mappedComments)
        return CompletableFuture.supplyAsync { mappedComments }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(CommentsDataLoader::class.java)
    }
}