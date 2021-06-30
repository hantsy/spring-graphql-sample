package com.example.demo.gql;

import com.example.demo.gql.types.*;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
@Slf4j
@Component
public class DataFetchers {
    final PostService postService;
    final ObjectMapper objectMapper;

    public DataFetcher<Post> postById() {
        return (DataFetchingEnvironment dfe) -> {
            String postId = dfe.getArgument("postId");
            return postService.getPostById(postId);
        };
    }

    public DataFetcher<List<Post>> allPosts() {
        return dfe -> postService.getAllPosts();
    }

    public DataFetcher<UUID> createPost() {
        return (DataFetchingEnvironment dfe) -> {
            var input = dfe.getArgument("createPostInput");
            var createPostInput = objectMapper.convertValue(input, CreatePostInput.class);
            return postService.createPost(createPostInput);
        };
    }

    public DataFetcher<CompletionStage<Author>> authorOfPost() {
        return (DataFetchingEnvironment dfe) -> {
            DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
            Post post = dfe.getSource();
            return dataLoader.load(post.getAuthorId());
        };
    }

    public DataFetcher<CompletionStage<List<Comment>>> commentsOfPost() {
        return (DataFetchingEnvironment dfe) -> {
            DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader("commentsLoader");
            Post post = dfe.getSource();
            return dataLoader.load(post.getId());
        };
    }

    public DataFetcher<UUID> addComment() {
        return (DataFetchingEnvironment dfe) -> {
            var commentInputArg = dfe.getArgument("commentInput");
            var input = objectMapper.convertValue(commentInputArg, CommentInput.class);
            return this.postService.addComment(input);
        };
    }

    public DataFetcher<Publisher<Comment>> commentAdded() {
        return (DataFetchingEnvironment dfe) -> {
            log.info("connect to `commentAdded`");
            return this.postService.commentAdded();
        };
    }

    public DataFetcher<Boolean> upload() {
        return (DataFetchingEnvironment dfe) -> {
            MultipartFile file = dfe.getArgument("file");
            log.info("file name: {}", file.getName());
            log.info("content type: {}", file.getContentType());
            log.info("original file name: {}", file.getOriginalFilename());
            log.info("file content size: {}", file.getSize());
            String content = StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8);
            log.info("file content : {}", content);
            return true;
        };
    }
}
