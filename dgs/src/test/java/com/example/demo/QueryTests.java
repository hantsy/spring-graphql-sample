package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcher;
import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostNotFoundException;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {QueryTests.QueryTestsConfig.class})
@Slf4j
class QueryTests {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Configuration
    @Import(PostsDataFetcher.class)
    @ImportAutoConfiguration(classes = {
            DgsAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    static class QueryTestsConfig {

    }

    @Test
    void allPosts() {
        when(postService.getAllPosts())
                .thenReturn(
                        List.of(
                                Post.builder().id(1L)
                                        .title("test title")
                                        .content("test content")
                                        .build(),
                                Post.builder().id(2L)
                                        .title("test title2")
                                        .content("test content2")
                                        .build()
                        )
                );
        List<String> titles = dgsQueryExecutor.executeAndExtractJsonPath(
                " { allPosts { title content }}",
                "data.allPosts[*].title");

        assertThat(titles).containsAll(List.of("test title", "test title2"));

        verify(postService, times(1)).getAllPosts();
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById() {
        when(postService.getPostById(anyLong())).thenReturn(
                Post.builder().id(1L)
                        .title("test title")
                        .content("test content")
                        .build()
        );
        String title = dgsQueryExecutor.executeAndExtractJsonPath(
                "query postById($postId:ID!){ postById(postId:$postId) { title content }}",
                "data.postById.title",
                Map.of("postId", 1L)
        );

        assertThat(title).isEqualTo("test title");

        verify(postService, times(1)).getPostById(anyLong());
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById_notFound() {
        when(postService.getPostById(anyLong())).thenThrow(new PostNotFoundException(1L));
        var executeResult = dgsQueryExecutor.execute(
                "query postById($postId: ID!){ postById(postId:$postId) { title content }}",
                Map.of("postId", 1L)
        );


        assertThat(executeResult.getErrors()).isNotEmpty();
        assertThat(executeResult.getErrors().get(0).getMessage()).contains("Post: #1 was not found.");

        verify(postService, times(1)).getPostById(anyLong());
        verifyNoMoreInteractions(postService);
    }

}
