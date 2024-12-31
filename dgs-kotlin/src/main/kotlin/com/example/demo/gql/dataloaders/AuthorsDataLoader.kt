package com.example.demo.gql.dataloaders

import com.example.demo.gql.types.Author
import com.example.demo.service.AuthorService
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.BatchLoader
import java.util.UUID
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.CompletionStage

@DgsDataLoader(name = "authorsLoader")
class AuthorsDataLoader(val authorService: AuthorService) : BatchLoader<UUID, Author> {
    override fun load(keys: List<UUID>): CompletionStage<List<Author>> = completedFuture(
        authorService.getAuthorByIdIn(keys)
    )
}

