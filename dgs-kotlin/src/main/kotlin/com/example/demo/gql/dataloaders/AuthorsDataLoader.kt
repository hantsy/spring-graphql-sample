package com.example.demo.gql.dataloaders

import com.example.demo.gql.types.Author
import com.example.demo.service.AuthorService
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.BatchLoader
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.CompletionStage

@DgsDataLoader(name = "authorsLoader")
class AuthorsDataLoader(val authorService: AuthorService) : BatchLoader<String, Author> {
    override fun load(keys: List<String>): CompletionStage<List<Author>> = supplyAsync {
        authorService.getAuthorByIdIn(keys)
    }
}

