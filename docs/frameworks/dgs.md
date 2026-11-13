# Netflix DGS

The [DGS (Domain Graph Service) framework](https://netflix.github.io/dgs/) is
Netflix's GraphQL server framework for Spring Boot, open-sourced in February
2021 after years of internal use. Since DGS 9 it is built on
[Spring GraphQL](./spring-graphql.md) — see
[the integration](./integration.md) — so this page focuses on what DGS adds on
top: its programming model and tooling.

## The programming model

DGS is schema-first: you write SDL under `src/main/resources/schema/`, then
implement data fetchers as annotated Spring components. The following excerpt
is from
[PostsDataFetcher.java](https://github.com/hantsy/spring-graphql-sample/blob/master/dgs/src/main/java/com/example/demo/gql/datafetchers/PostsDataFetcher.java)
in the [dgs](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs)
example:

```java
@DgsComponent
public class PostsDataFetcher {
    private final PostService postService;

    @DgsQuery
    public List<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @DgsQuery
    public Post postById(@InputArgument Long postId) {
        return this.postService.getPostById(postId);
    }

    @DgsData(parentType = "Post", field = "author")
    public CompletableFuture<Author> author(DgsDataFetchingEnvironment dfe) {
        DataLoader<Long, Author> dataLoader = dfe.getDataLoader("authorsLoader");
        Post post = dfe.getSource();
        return dataLoader.load(post.getAuthorId());
    }

    @DgsMutation
    public Post createPost(@InputArgument("createPostInput") CreatePostInput input) {
        var id = this.postService.createPost(input);
        return this.postService.getPostById(id);
    }
}
```

The annotations map directly to the schema:

- `@DgsComponent` — marks the class as a data fetcher container (a Spring
  stereotype).
- `@DgsQuery`, `@DgsMutation`, `@DgsSubscription` — bind a method to a field
  on the corresponding root type, matched by method name.
- `@DgsData(parentType = "...", field = "...")` — binds a method to any field
  on any type, for nested resolution.
- `@InputArgument` — binds a GraphQL argument to a method parameter, with
  automatic coercion to the parameter type.

Subscriptions return a reactive `Publisher`; the
[dgs-subscription-ws](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ws)
example shows a `@DgsSubscription` method backed by a Reactor `Sinks.many()`
stream, and
[dgs-subscription-sse](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-sse)
serves the same model over SSE.

## Code generation

The DGS codegen plugin (Gradle and Maven) generates Java or Kotlin types from
the schema, so data fetchers work against generated classes instead of
hand-written DTOs. The
[dgs-codegen](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-codegen)
example demonstrates it with Spring Data JDBC.

## Federation

DGS ships first-class support for Apollo Federation: a DGS service can act as
a federated subgraph, and the framework handles entity resolution and the
federation schema additions. This is one of the features DGS adds beyond
Spring GraphQL.

## Testing

The `dgs-starter-test` dependency provides `DgsQueryExecutor`, which executes
documents against the application context without an HTTP round trip, and
`@DgsTest` slices for focused query tests. Since the Spring GraphQL
integration, `DgsQueryExecutor` delegates to Spring GraphQL's execution
service, so existing tests keep working.

## File uploads

DGS resolves multipart uploads to Spring `MultipartFile` arguments — see
[File upload](../intro/file-upload.md) and the
[dgs-fileupload](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-fileupload)
example.

## Versions and compatibility

DGS releases a platform BOM, `graphql-dgs-platform-dependencies`, that aligns
the DGS artifacts with a Spring Boot version. The examples in this repository
use DGS platform 12.1.0 on Spring Boot 4.1.2. The upstream compatibility
matrix:

| DGS version | Spring Boot | Status |
| ----------- | ----------- | ------ |
| 11+ | 4 | Actively maintained. |
| 10.x | 3 | Most features backported until the second half of 2026. |
| 5.x | 2 | No longer maintained. |

Every DGS release since 9.0 builds on Spring GraphQL, so a DGS application
can also use Spring GraphQL APIs directly — see
[the integration](./integration.md).