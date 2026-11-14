package com.example.demo;

import com.example.demo.model.CommentEntity;
import com.example.demo.model.PostEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RepositoryTests {

    @Autowired
    PostRepository posts;

    @Autowired
    CommentRepository comments;

    @Autowired
    AuthorRepository authors;

    private Long newAuthor() {
        return authors.create("test user", "test@example.com");
    }

    private Long newPost(String title) {
        return newPost(newAuthor(), title);
    }

    private Long newPost(Long authorId, String title) {
        return posts.create(title, title + " content", "DRAFT", authorId);
    }

    private Long newComment(Long postId, String content) {
        return comments.create(content, postId);
    }

    @Test
    void createAndFindPostById() {
        var authorId = newAuthor();
        var postId = newPost(authorId, "test title");

        PostEntity post = posts.findById(postId);

        assertThat(post).isNotNull();
        assertThat(post.id()).isEqualTo(postId);
        assertThat(post.title()).isEqualTo("test title");
        assertThat(post.content()).isEqualTo("test title content");
        assertThat(post.status()).isEqualTo("DRAFT");
        assertThat(post.authorId()).isEqualTo(authorId);
    }

    @Test
    void findAllPosts() {
        newPost("post 1");
        newPost("post 2");

        List<PostEntity> all = posts.findAll();

        assertThat(all).extracting(PostEntity::title)
                .contains("post 1", "post 2");
    }

    @Test
    void findPostsByAuthorId() {
        var authorId = newAuthor();
        newPost(authorId, "post 1");

        List<PostEntity> byAuthor = posts.findByAuthorId(authorId);

        assertThat(byAuthor).hasSize(1);
        assertThat(byAuthor.get(0).title()).isEqualTo("post 1");
    }

    @Test
    void createAndFindCommentsByPostId() {
        var postId = newPost("test title");
        newComment(postId, "comment 1");
        newComment(postId, "comment 2");

        List<CommentEntity> result = comments.findByPostId(postId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CommentEntity::content)
                .containsExactlyInAnyOrder("comment 1", "comment 2");
    }

    @Test
    void findCommentsByPostIdIn() {
        var postId1 = newPost("post 1");
        var postId2 = newPost("post 2");
        newComment(postId1, "comment 1");
        newComment(postId2, "comment 2");

        List<CommentEntity> result = comments.findByPostIdIn(List.of(postId1, postId2));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CommentEntity::postId)
                .containsExactlyInAnyOrder(postId1, postId2);
    }
}
