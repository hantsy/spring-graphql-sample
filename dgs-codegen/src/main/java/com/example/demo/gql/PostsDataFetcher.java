package com.example.demo.gql;

import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

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
        return this.postService.getPostById(postId);
    }

    @DgsData(
            parentType = DgsConstants.POST.TYPE_NAME,
            field = DgsConstants.POST.Author
    )
    public Author author(DgsDataFetchingEnvironment dfe) {
        Post post = dfe.getSource();
        String authorId = post.getAuthorId();
        return this.authorService.getAuthorById(authorId);
    }

    @DgsData(
            parentType = DgsConstants.POST.TYPE_NAME,
            field = DgsConstants.POST.Comments
    )
    public List<Comment> comments(DgsDataFetchingEnvironment dfe) {
        Post post = dfe.getSource();
        return this.postService.getCommentsByPostId(post.getId());
    }

    @DgsMutation
    public UUID createPost(@InputArgument("createPostInput") CreatePostInput input) {
        return this.postService.createPost(input);
    }
}
