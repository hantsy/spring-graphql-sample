package com.example.demo;

import com.example.demo.gql.PostController;
import com.example.demo.gql.types.Post;
import com.example.demo.gql.types.PostStatus;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostNotFoundException;
import com.example.demo.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@GraphQlTest(PostController.class)
@Slf4j
public class QueryTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Test
    void allPosts() {
        when(postService.getAllPosts()).thenReturn(List.of(
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build(),
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title2")
                        .content("test content2")
                        .build()
        ));

        var allPosts = """
                query posts{
                   allPosts{
                     id
                     title
                     content
                   }
                 }""";
        graphQlTester.document(allPosts)
                .execute()
                .path("allPosts[*].title")
                .entityList(String.class)
                .satisfies(titles -> assertThat(titles).contains("test title", "test title2"));

        verify(postService, times(1)).getAllPosts();
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById() {
        var id = UUID.randomUUID().toString();
        var post = Post.builder()
                .id(id)
                .title("Post")
                .content("Content of post one")
                .createdAt(LocalDateTime.now())
                .status(PostStatus.DRAFT)
                .build();
        when(this.postService.getPostById(anyString())).thenReturn(post);
        var postById = """
                query post($postId:String!){
                   postById(postId:$postId) {
                     id
                     title
                     content
                   }
                 }""";

        graphQlTester.document(postById)
                .variable("postId", id)
                .execute()
                .path("postById.title")
                .entity(String.class).satisfies(s -> assertThat(s).startsWith("Post"));

        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById_notFoundException() {
        var id = UUID.randomUUID().toString();
        var post = Post.builder()
                .id(id)
                .title("Post")
                .content("Content of post one")
                .createdAt(LocalDateTime.now())
                .status(PostStatus.DRAFT)
                .build();
        var exception = new PostNotFoundException(id);
        doThrow(exception).when(this.postService).getPostById(anyString());
        var postById = """
                query post($postId:String!){
                   postById(postId:$postId) {
                     id
                     title
                     content
                   }
                 }""";

        graphQlTester.document(postById)
                .variable("postId", id)
                .execute()
                .errors()
                .expect(responseError -> responseError.getErrorType() == ErrorType.NOT_FOUND)
                .expect(responseError -> Objects.equals(responseError.getMessage(), exception.getMessage()));

        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

}
