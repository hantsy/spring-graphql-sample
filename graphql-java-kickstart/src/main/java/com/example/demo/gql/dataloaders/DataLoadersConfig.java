package com.example.demo.gql.dataloaders;

import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContextBuilder;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.Collections;

@Configuration
public class DataLoadersConfig {

    @Bean
    GraphQLServletContextBuilder graphQLServletContextBuilder(AuthorService authorService, PostService postService) {
        return new DefaultGraphQLServletContextBuilder() {
            @Override
            public GraphQLKickstartContext build(HttpServletRequest request, HttpServletResponse response) {
                return GraphQLKickstartContext.of(buildDataLoaderRegistry(), Collections.emptyMap());
            }

            @Override
            public GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest) {
                return GraphQLKickstartContext.of(buildDataLoaderRegistry(), Collections.emptyMap());
            }

            @Override
            public GraphQLKickstartContext build() {
                return GraphQLKickstartContext.of(buildDataLoaderRegistry(), Collections.emptyMap());
            }

            private DataLoaderRegistry buildDataLoaderRegistry() {
                DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
                dataLoaderRegistry.register("authorsLoader", DataLoaderFactory.newDataLoader(new AuthorsDataLoader(authorService)));
                dataLoaderRegistry.register("commentsLoader", DataLoaderFactory.newMappedDataLoader(new CommentsDataLoader(postService)));
                return dataLoaderRegistry;
            }

        };
    }
}
