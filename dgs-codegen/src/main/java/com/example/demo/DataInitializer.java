package com.example.demo;

import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {
    final PostService postService;
    final AuthorService authorService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var author = Author.newBuilder()
                .id(UUID.randomUUID().toString())
                .name("user")
                .email("user@example.com")
                .build();

        authorService.init(List.of(author));

        var initData = IntStream.range(1, 5)
                .mapToObj(
                        i -> {
                            var comments = IntStream.range(1, new Random().nextInt(5) + 1)
                                    .mapToObj(c -> Comment.newBuilder()
                                            .id(UUID.randomUUID().toString())
                                            .content("comment #" + c)
                                            .build()
                                    )
                                    .toList();
                            var data = Post.newBuilder()
                                    .id(UUID.randomUUID().toString())
                                    .title("Dgs post #" + i)
                                    .content("test content of #" + i)
                                    .comments(comments)
                                    .authorId(author.getId())
                                    .build();
                            return data;
                        }
                )
                .toList();

        this.postService.init(initData);

        this.postService.getAllPosts().forEach(p -> log.info("post data : {}", p));

    }
}
