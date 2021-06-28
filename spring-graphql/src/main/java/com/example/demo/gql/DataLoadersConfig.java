package com.example.demo.gql;

import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.web.WebInterceptor;

@Configuration
public class DataLoadersConfig {
    @Bean
    public WebInterceptor interceptor(DataLoaders loader) {
        return (input, next) -> {
            input.configureExecutionInput((executionInput, builder) -> {
                DataLoaderRegistry registry = new DataLoaderRegistry();
                registry.register("authorsLoader", loader.authorsLoader());
                registry.register("commentsLoader", loader.commentsLoader());
                return builder.dataLoaderRegistry(registry).build();
            });
            return next.handle(input);
        };
    }
}
