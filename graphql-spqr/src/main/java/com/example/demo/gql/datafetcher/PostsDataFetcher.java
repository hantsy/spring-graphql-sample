package com.example.demo.gql.datafetcher;

import com.example.demo.gql.types.*;
import com.example.demo.service.PostService;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@GraphQLApi
@Service
@RequiredArgsConstructor
@Slf4j
public class PostsDataFetcher {
    private final PostService postService;

    @GraphQLQuery
    public List<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @GraphQLQuery
    public Post postById(@GraphQLArgument(name = "postId") @GraphQLNonNull String postId) {
        return this.postService.getPostById(postId);
    }

    @GraphQLQuery
    public CompletableFuture<Author> author(@GraphQLContext Post post, @GraphQLEnvironment ResolutionEnvironment re) {
        DataLoader<String, Author> dataLoader = re.dataFetchingEnvironment.getDataLoader("authorsDataLoader");
        return dataLoader.load(post.getAuthorId());
    }

    @GraphQLQuery
    public CompletableFuture<List<Comment>> comments(@GraphQLContext Post post, @GraphQLEnvironment ResolutionEnvironment re) {
        DataLoader<String, List<Comment>> dataLoader = re.dataFetchingEnvironment.getDataLoader("commentsDataLoader");
        return dataLoader.load(post.getId());
    }

    @GraphQLMutation(name = "createPost", description = "create a new post")
    public Post createPost(@GraphQLArgument(name = "createPostInput") @GraphQLNonNull CreatePostInput input) {
        return this.postService.createPost(input);
    }

//    @GraphQLMutation(name = "createPost", description = "create a new post")
//    public Post createPost(@GraphQLNonNull String title, String content) {
//        return this.postService.createPost(CreatePostInput.of(title, content));
//    }

    @GraphQLMutation
    public Comment addComment(@GraphQLNonNull String postId, @GraphQLNonNull String content) {
        return this.postService.addComment(CommentInput.of(postId, content));
    }

    @GraphQLSubscription
    public Publisher<Comment> commentAdded() {
        return this.postService.commentAdded();
    }

}
