package com.example.demo;

import com.example.demo.model.PostEntity;
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

@SpringBootTest(classes = PostRepositoryTests.Config.class)
@Import(TestcontainersConfiguration.class)
@Transactional
class PostRepositoryTests {

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
    void createAndFindById() {
        var authorId = authors.create("test user", "test@example.com");
        var postId = posts.create("test title", "test content", "DRAFT", authorId);

        PostEntity post = posts.findById(postId);

        assertThat(post).isNotNull();
        assertThat(post.id()).isEqualTo(postId);
        assertThat(post.title()).isEqualTo("test title");
        assertThat(post.content()).isEqualTo("test content");
        assertThat(post.status()).isEqualTo("DRAFT");
        assertThat(post.authorId()).isEqualTo(authorId);
    }

    @Test
    void findAll() {
        var authorId = authors.create("test user", "test@example.com");
        posts.create("post 1", "content 1", "DRAFT", authorId);
        posts.create("post 2", "content 2", "DRAFT", authorId);

        List<PostEntity> all = posts.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(PostEntity::title)
                .containsExactlyInAnyOrder("post 1", "post 2");
    }

    @Test
    void findByAuthorId() {
        var authorId = authors.create("test user", "test@example.com");
        posts.create("post 1", "content 1", "DRAFT", authorId);

        List<PostEntity> byAuthor = posts.findByAuthorId(authorId);

        assertThat(byAuthor).hasSize(1);
        assertThat(byAuthor.get(0).title()).isEqualTo("post 1");
    }
}
