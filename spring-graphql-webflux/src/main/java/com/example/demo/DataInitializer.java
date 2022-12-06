package com.example.demo;

import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Random;

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
        var commentsDel = this.comments.deleteAll().block();
        var postDel = this.posts.deleteAll().block();
        var authorsDel = this.authors.deleteAll().block();

        log.info("deleted rows: authors: {}, comments: {}, posts: {}", authorsDel, commentsDel, postDel);
        this.authors.create("user", "user@example.com")
                .flatMapMany(authorId -> Flux.range(1, 5)
                        .flatMap(i -> this.posts.create("Dgs post #" + i, "test content of #" + i, "DRAFT", authorId)
                                .flatMapMany(
                                        postId -> Flux.range(1, new Random().nextInt(5) + 1)
                                                .flatMap(c -> this.comments.create("comment #" + c, postId))
                                )
                        )
                ).subscribe();

        this.posts.findAll().subscribe(p -> log.info("post: {}", p));
        this.comments.findAll().subscribe(p -> log.info("comment: {}", p));
        this.authors.findAll().subscribe(p -> log.info("author: {}", p));
        log.info("done data initialization...");
    }
}
