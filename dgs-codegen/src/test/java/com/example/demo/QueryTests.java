package com.example.demo;

import com.example.demo.gql.client.AllPostsGraphQLQuery;
import com.example.demo.gql.client.AllPostsProjectionRoot;
import com.example.demo.gql.types.Post;
import com.example.demo.service.PostNotFoundException;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;
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
        var query = """
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

    @Test
    void postById() {
        when(postService.getPostById(anyString())).thenReturn(
                Post.newBuilder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build()
        );
        String title = dgsQueryExecutor.executeAndExtractJsonPath(
                "query postById($postId:String!){ postById(postId:$postId) { title content }}",
                "data.postById.title",
                Map.of("postId", "test")
        );

        assertThat(title).isEqualTo("TEST TITLE");

        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById_notFound() {
        when(postService.getPostById(anyString())).thenThrow(new PostNotFoundException("test"));
        var executeResult = dgsQueryExecutor.execute(
                "query postById($postId:String!){ postById(postId:$postId) { title content }}",
                Map.of("postId", "test")
        );

        assertThat(executeResult.getErrors()).isNotEmpty();
        assertThat(executeResult.getErrors().get(0).getMessage()).isEqualTo("Post: test was not found.");

        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }
}
