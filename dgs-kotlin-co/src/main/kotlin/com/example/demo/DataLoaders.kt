package com.example.demo

import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Comment
import com.netflix.graphql.dgs.DgsDataLoader
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.future
import org.dataloader.BatchLoader
import org.dataloader.MappedBatchLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletionStage

@OptIn(DelicateCoroutinesApi::class)
@DgsDataLoader(name = "authorsLoader")
class AuthorsDataLoader(val authorService: AuthorService) : BatchLoader<String, Author> {
    override fun load(keys: List<String>): CompletionStage<List<Author>> = GlobalScope.future {
        authorService.getAuthorByIdIn(keys).toList()
    }
}

@OptIn(DelicateCoroutinesApi::class)
@DgsDataLoader(name = "commentsLoader")
class CommentsDataLoader(val postService: PostService) : MappedBatchLoader<String, List<Comment>> {
    private val log: Logger = LoggerFactory.getLogger(CommentsDataLoader::class.java)

    override fun load(keys: Set<String>): CompletionStage<Map<String, List<Comment>>> = GlobalScope.future {
        val comments = postService.getCommentsByPostIdIn(keys).toList()
        val mappedComments: MutableMap<String, List<Comment>> = mutableMapOf()
        keys.forEach { mappedComments[it] = comments.filter { c -> c.postId == it } }
        log.info("mapped comments: {}", mappedComments)
        mappedComments
    }

}