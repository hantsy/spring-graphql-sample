package com.example.demo.gql;

import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.Comment;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.MappedBatchLoader;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataLoaders {
    final AuthorService authorService;
    final PostService postService;

    public DataLoader<String, Author> authorsLoader() {
        BatchLoader<String, Author> batchLoader =
                (List<String> keys) -> CompletableFuture.supplyAsync(() -> authorService.getAuthorByIdIn(keys));
        return DataLoader.newDataLoader(batchLoader);
    }

    public DataLoader<String, List<Comment>> commentsLoader() {
        MappedBatchLoader<String, List<Comment>> batchLoader = (Set<String> keys) -> {
            List<Comment> comments = this.postService.getCommentsByPostIdIn(keys);

            Map<String, List<Comment>> mappedComments = new HashMap<>();
            keys.forEach(
                    k -> mappedComments.put(k, comments
                            .stream()
                            .filter(c -> c.getPostId().equals(k)).toList())
            );
            log.info("mapped comments: {}", mappedComments);
            return CompletableFuture.supplyAsync(() -> mappedComments);
        };
        return DataLoader.newMappedDataLoader(batchLoader);
    }
}
