package com.example.demo.gql;

import com.example.demo.gql.directives.UpperCaseDirectiveWiring;
import com.example.demo.gql.scalars.Scalars;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.RuntimeWiring;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostsRuntimeWiring implements RuntimeWiringConfigurer {
    private final DataFetchers dataFetchers;

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        builder
                .codeRegistry(buildCodeRegistry())
                /*  .type("Query",
                          typeWiring -> typeWiring
                                  .dataFetcher("allPosts", postsDataFetchers.allPosts())
                                  .dataFetcher("postById", postsDataFetchers.postById())
                  )
                  .type("Mutation",
                          typeWiring -> typeWiring
                                  .dataFetcher("createPost", postsDataFetchers.createPost())
                                  .dataFetcher("addComment", postsDataFetchers.addComment())
                  )
                  .type("Post", typeWiring -> typeWiring
                          .dataFetcher("author", postsDataFetchers.authorOfPost())
                          .dataFetcher("comments", postsDataFetchers.commentsOfPost())

                  )
                  .type("Subscription", typeWiring -> typeWiring
                          .dataFetcher("commentAddded", postsDataFetchers.commentAdded())
                  )*/
                .scalar(Scalars.uuidType())
                .scalar(Scalars.localDateTimeType())
                .directive("uppercase", new UpperCaseDirectiveWiring())
                .build();
    }

    private GraphQLCodeRegistry buildCodeRegistry() {
        return GraphQLCodeRegistry.newCodeRegistry()
                .dataFetchers("Query", Map.of(
                        "postById", dataFetchers.postById(),
                        "allPosts", dataFetchers.allPosts()
                ))
                .dataFetchers("Mutation", Map.of(
                        "createPost", dataFetchers.createPost(),
                        // "upload", dataFetchers.upload(),
                        "addComment", dataFetchers.addComment()
                ))
                .dataFetchers("Subscription", Map.of(
                        "commentAdded", dataFetchers.commentAdded()
                ))
                .dataFetchers("Post", Map.of(
                        "author", dataFetchers.authorOfPost(),
                        "comments", dataFetchers.commentsOfPost()
                ))
                //.typeResolver()
                //.fieldVisibility()
                // Spring has set the default property data fetching.
                //.defaultDataFetcher(environment -> PropertyDataFetcher.fetching(environment.getFieldDefinition().getName()))
                .build();
    }

}
