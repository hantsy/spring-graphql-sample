package com.example.demo;

import com.example.demo.gql.DgsConstants;
import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.netflix.graphql.dgs.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@DgsComponent
@RequiredArgsConstructor
public class PostsDataFetcher {
    private final PostService postService;
    private final AuthorService authorService;

    @DgsQuery
    public List<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @DgsQuery
    public Post postById(@InputArgument String postId) {
        return this.postService.getPostById(postId).orElseThrow(() -> new PostNotFoundException(postId));
    }

    @DgsData(
            parentType = DgsConstants.POST.TYPE_NAME,
            field = DgsConstants.POST.Author
    )
    public Author author(DgsDataFetchingEnvironment dfe) {
        Post post = dfe.getSource();
        String authorId = post.getAuthorId();
        return this.authorService.getAuthorById(authorId)
                .orElseThrow(() -> new AuthorNotFoundException(authorId));
    }

    @DgsMutation
    public Post createPost(@InputArgument("createPostInput") CreatePostInput input) {
        return this.postService.createPost(input);
    }
}
