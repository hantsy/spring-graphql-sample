package com.example.demo.gql.dataloaders;

import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import org.springframework.graphql.execution.DataLoaderRegistrar;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoaderConfig implements DataLoaderRegistrar {
    private final AuthorsDataLoader authorsDataLoader;
    private final CommentsDataLoader commentsDataLoader;

    @Override
    public void registerDataLoaders(DataLoaderRegistry registry) {
        registry.register("commentsLoader", DataLoaderFactory.newMappedDataLoader(commentsDataLoader));
        registry.register("authorsLoader", DataLoaderFactory.newDataLoader(authorsDataLoader));
    }
}
