package com.example.demo.gql.dataloaders;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.web.WebInterceptor;

@Configuration
public class DataLoadersConfig {
    @Bean
    public WebInterceptor interceptor(AuthorsDataLoader loader, CommentsDataLoader commentsDataLoader) {
        return (input, next) -> {
            input.configureExecutionInput((executionInput, builder) -> {
                DataLoaderRegistry registry = new DataLoaderRegistry();
                registry.register("authorsLoader", DataLoader.newDataLoader(loader));
                registry.register("commentsLoader", DataLoader.newMappedDataLoader(commentsDataLoader));
                return builder.dataLoaderRegistry(registry).build();
            });
            return next.handle(input);
        };
    }
}
