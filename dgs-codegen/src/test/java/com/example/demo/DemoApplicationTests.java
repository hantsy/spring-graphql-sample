package com.example.demo;

import com.example.demo.gql.client.AllPostsGraphQLQuery;
import com.example.demo.gql.client.AllPostsProjectionRoot;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
//@SpringBootTest(classes = {DgsAutoConfiguration.class, PostsDataFetcher.class})
class DemoApplicationTests {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @Test
    void allPosts() {
        var queryRequest = new GraphQLQueryRequest(
                AllPostsGraphQLQuery.newRequest().build(),
                new AllPostsProjectionRoot().id()
                        .title()
                        .content()
                        .author().id().name().email().getParent()
                        .comments().content().getParent()
        );
        List<String> titles = dgsQueryExecutor.executeAndExtractJsonPath(
                queryRequest.serialize(),
                "data.allPosts[*].title");

        assertThat(titles).anyMatch(s -> s.startsWith("DGS POST"));
    }

}
