package com.example.demo;

import com.example.demo.model.CommentEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CommentRepositoryTests.Config.class)
@Import(TestcontainersConfiguration.class)
@Transactional
class CommentRepositoryTests {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({PostRepository.class, CommentRepository.class, AuthorRepository.class})
    static class Config {
    }

    @Autowired
    PostRepository posts;

    @Autowired
    CommentRepository comments;

    @Autowired
    AuthorRepository authors;

    @Test
    void createAndFindByPostId() {
        var authorId = authors.create("test user", "test@example.com");
        var postId = posts.create("test title", "test content", "DRAFT", authorId);
        comments.create("comment 1", postId);
        comments.create("comment 2", postId);

        List<CommentEntity> result = comments.findByPostId(postId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CommentEntity::content)
                .containsExactlyInAnyOrder("comment 1", "comment 2");
    }

    @Test
    void findByPostIdIn() {
        var authorId = authors.create("test user", "test@example.com");
        var postId1 = posts.create("post 1", "content 1", "DRAFT", authorId);
        var postId2 = posts.create("post 2", "content 2", "DRAFT", authorId);
        comments.create("comment 1", postId1);
        comments.create("comment 2", postId2);

        List<CommentEntity> result = comments.findByPostIdIn(List.of(postId1, postId2));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CommentEntity::postId)
                .containsExactlyInAnyOrder(postId1, postId2);
    }
}
