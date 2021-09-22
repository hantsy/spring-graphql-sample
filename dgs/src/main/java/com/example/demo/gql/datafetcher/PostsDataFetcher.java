package com.example.demo.gql.datafetcher;

import com.example.demo.gql.dataloaders.CommentsDataLoader;
import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@DgsComponent
@RequiredArgsConstructor
@Slf4j
public class PostsDataFetcher {
    private final PostService postService;

    @DgsQuery
    public List<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @DgsQuery
    public Post postById(@InputArgument String postId) {
        return this.postService.getPostById(postId);
    }

    /*
    @DgsData(
            parentType = DgsConstants.POST.TYPE_NAME,
            field = DgsConstants.POST.Author
    )
    public Author author(DgsDataFetchingEnvironment dfe) {
        Post post = dfe.getSource();
        String authorId = post.getAuthorId();
        return this.authorService.getAuthorById(authorId);
    }*/

/*
    @DgsDataLoader(name = "authorsLoader")
    public BatchLoader<String, Author> authorBatchLoader = keys ->
            CompletableFuture.supplyAsync(() ->
                    this.authorService.getAuthorByIdIn(keys)
            );*/

    @DgsData(
            parentType = "Post",
            field = "author"
    )
    public CompletableFuture<Author> author(DgsDataFetchingEnvironment dfe) {
        DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
        Post post = dfe.getSource();
        return dataLoader.load(post.getAuthorId());
    }

    /*
    @DgsData(
            parentType = DgsConstants.POST.TYPE_NAME,
            field = DgsConstants.POST.Comments
    )
    public List<Comment> comments(DgsDataFetchingEnvironment dfe) {
        Post post = dfe.getSource();
        return this.postService.getCommentsByPostId(post.getId());
    }
    */

    @DgsData(
            parentType = "Post",
            field = "comments"
    )
    public CompletableFuture<List<Comment>> comments(DgsDataFetchingEnvironment dfe) {
        DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader(CommentsDataLoader.class);
        Post post = dfe.getSource();
        return dataLoader.load(post.getId());
    }

    @DgsMutation
    public Post createPost(@InputArgument("createPostInput") CreatePostInput input) {
        var id = this.postService.createPost(input);
        return this.postService.getPostById(id.toString());
    }

}
