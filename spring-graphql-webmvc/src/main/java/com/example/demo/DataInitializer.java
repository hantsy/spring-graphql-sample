package com.example.demo;

import com.example.demo.gql.types.PostStatus;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {
    final PostRepository posts;
    final CommentRepository comments;
    final AuthorRepository authors;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("start data initialization...");
        int commentsDel = this.comments.deleteAll();
        int postDel = this.posts.deleteAll();
        int authorsDel = this.authors.deleteAll();

        log.info("deleted rows: authors: {}, comments: {}, posts: {}", authorsDel, commentsDel, postDel);
        var authorId = this.authors.create("user", "user@example.com");
        IntStream.range(1, 5)
                .forEach(
                        i -> {
                            var postId = this.posts.create("Dgs post #" + i, "test content of #" + i, PostStatus.DRAFT.name(), authorId);

                            IntStream.range(1, new Random().nextInt(5) + 1)
                                    .forEach(c -> this.comments.create("comment #" + c, postId));
                        }
                );

        this.posts.findAll().forEach(p -> log.info("post: {}", p));
        this.comments.findAll().forEach(p -> log.info("comment: {}", p));
        this.authors.findAll().forEach(p -> log.info("author: {}", p));
        log.info("done data initialization...");
    }
}
