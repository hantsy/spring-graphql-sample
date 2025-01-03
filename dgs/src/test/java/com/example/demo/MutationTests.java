package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcher;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.test.EnableDgsTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@EnableDgsTest
@SpringBootTest(classes = {MutationTests.MutationTestsConfig.class})
@Slf4j
class MutationTests {

    @Configuration
    @Import(PostsDataFetcher.class)
    static class MutationTestsConfig {

    }

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @MockitoBean
    PostService mockedPostService;

    @MockitoBean
    AuthorService authorService;

    @Test
    void createPosts() {
        when(mockedPostService.createPost(any(CreatePostInput.class))).thenReturn(1L);
        when(mockedPostService.getPostById(anyLong())).thenReturn(
                Post.builder().id(1L)
                        .title("test title")
                        .content("test content")
                        .build()
        );

        String title = dgsQueryExecutor.executeAndExtractJsonPath(
                """
                        mutation createPost($input: CreatePostInput!){
                            createPost(createPostInput:$input){
                                id
                                title
                                content
                            }
                        }""",
                "data.createPost.title",
                Map.of("input", Map.of(
                                "title", "test title",
                                "content", "test content"
                        )
                )
        );

        assertThat(title).isEqualTo("test title");

        verify(mockedPostService, times(1)).createPost(any(CreatePostInput.class));
        verify(mockedPostService, times(1)).getPostById(anyLong());
        verifyNoMoreInteractions(mockedPostService);
    }

}
