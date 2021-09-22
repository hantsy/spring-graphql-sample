package com.example.demo;

import com.example.demo.gql.client.AllPostsGraphQLQuery;
import com.example.demo.gql.client.AllPostsProjectionRoot;
import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


//@SpringBootTest(classes = {DgsAutoConfiguration.class, PostsDataFetcher.class})
@SpringBootTest()
class QueryTests {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @MockBean
    PostService postService;

    @Test
    void allPosts() {
        when(postService.getAllPosts())
                .thenReturn(
                        List.of(Post.newBuilder().id(UUID.randomUUID().toString()).title("dgs post test title").content("test content").build(),
                                Post.newBuilder().id(UUID.randomUUID().toString()).title("dgs post test title2").content("test content2").build()
                        )
                );
        var queryRequest = new GraphQLQueryRequest(
                AllPostsGraphQLQuery.newRequest().build(),
                new AllPostsProjectionRoot().id()
                        .title()
                        .content()
        );
        List<String> titles = dgsQueryExecutor.executeAndExtractJsonPath(
                queryRequest.serialize(),
                "data.allPosts[*].title");

        assertThat(titles).anyMatch(s -> s.startsWith("DGS POST"));
        verify(postService, times(1)).getAllPosts();
        verifyNoMoreInteractions(postService);
    }


    @Test
    void allPosts_literalQueryString() {
        when(postService.getAllPosts())
                .thenReturn(
                        List.of(Post.newBuilder().id(UUID.randomUUID().toString()).title("dgs post test title").content("test content").build(),
                                Post.newBuilder().id(UUID.randomUUID().toString()).title("dgs post test title2").content("test content2").build()
                        )
                );
        var query= """
                query allPosts{
                    allPosts{
                        title
                        content
                    }
                }
                """.trim();

        List<String> titles = dgsQueryExecutor.executeAndExtractJsonPath(
                query,
                "data.allPosts[*].title");

        assertThat(titles).anyMatch(s -> s.startsWith("DGS POST"));
        verify(postService, times(1)).getAllPosts();
        verifyNoMoreInteractions(postService);
    }
}
