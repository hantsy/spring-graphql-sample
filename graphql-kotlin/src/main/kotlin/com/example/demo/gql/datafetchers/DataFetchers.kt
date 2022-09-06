package com.example.demo.gql.datafetchers

import com.example.demo.gql.types.Author
import com.example.demo.gql.types.Comment
import com.example.demo.gql.types.Post
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

// lazily loading data from dataloaders
@Component("CommentListDataFetcher")
class CommentsDataFetcher : DataFetcher<CompletableFuture<List<Comment>>> {
    companion object {
        private val log = LoggerFactory.getLogger(CommentsDataFetcher::class.java)
    }

    override fun get(environment: DataFetchingEnvironment): CompletableFuture<List<Comment>> {
        val postId = environment.getSource<Post>().id
        log.debug("Fetching comments of post: $postId")
        return environment.getValueFromDataLoader(CommentsDataLoader.name, postId)
    }
}

@Component("AuthorDataFetcher")
class AuthorDataFetcher : DataFetcher<CompletableFuture<Author>> {
    companion object {
        private val log = LoggerFactory.getLogger(AuthorDataFetcher::class.java)
    }

    override fun get(environment: DataFetchingEnvironment): CompletableFuture<Author> {
        val authorId = environment.getSource<Post>().authorId
        log.debug("Fetching author of post: $authorId")
        return environment.getValueFromDataLoader(AuthorsDataLoader.name, authorId)
    }
}