package com.example.demo.gql.dataloader

import com.example.demo.gql.types.Author
import com.example.demo.service.AuthorService
import com.netflix.graphql.dgs.DgsDataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.future
import org.dataloader.BatchLoader
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors

@DgsDataLoader(name = "authorsLoader")
class AuthorsDataLoader(val authorService: AuthorService) : BatchLoader<String, Author> {
    val loaderScope = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())
    override fun load(keys: List<String>): CompletionStage<List<Author>> = loaderScope.future {
        authorService.getAuthorByIdIn(keys).toList()
    }
}