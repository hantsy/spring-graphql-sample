package com.example.demo.gql.dataloaders;

import io.leangen.graphql.spqr.spring.autoconfigure.DataLoaderRegistryFactory;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimpleDataLoaderRegistryFactory implements DataLoaderRegistryFactory {
    private final AuthorsDataLoader authorsDataLoader;
    private final CommentsDataLoader commentsDataLoader;

    @Override
    public DataLoaderRegistry createDataLoaderRegistry() {
        DataLoaderRegistry loaders = new DataLoaderRegistry();
        loaders.register("authorsDataLoader", DataLoaderFactory.newDataLoader(authorsDataLoader));
        loaders.register("commentsDataLoader", DataLoaderFactory.newMappedDataLoader(commentsDataLoader));

        return loaders;
    }
}
