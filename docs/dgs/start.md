# Getting started with Netflix DGS

Netflix DGS (Domain Graph Service) is a GraphQL framework that Netflix
open-sourced in 2021 and has run in production since. It gives Spring Boot
developers a schema-first, annotation-driven way to build GraphQL APIs on top
of GraphQL Java.

In this guide you build a small blog API — `Post`, `Author`, and `Comment` —
and, along the way, meet the pieces that make up a DGS application: the schema,
data fetchers, data loaders, error handling, and validation. From there you
extend it to a reactive WebFlux stack, generate types with the DGS codegen
plugin, call the API with the generated client, and accept file uploads.

Every section maps to a working module in the repository. The code excerpts
are trimmed to the parts that matter.

This tutorial follows the same order you would use to build the service:

1. Define the GraphQL schema and the Java data model.
2. Connect DGS fetchers to the schema and persist data.
3. Run the application and exercise its API in GraphiQL.
4. Extend the service with reactive execution, generated types, custom
   components, a typesafe client, and file uploads.

Repository: [spring-graphql-sample](https://github.com/hantsy/spring-graphql-sample).

## Prerequisites

- **Java 25** — the examples target Spring Boot 4.1.2 / Spring Framework 7.
- **Docker** — the blog API stores data in PostgreSQL, started with Docker
  Compose.
- **Maven** — optional; every module ships its own Maven Wrapper (`mvnw`).

## The blog domain and schema

**A GraphQL schema** is the contract that describes which data clients can
read or change and what each operation returns. DGS is schema-first: you write
that contract in the GraphQL Schema Definition Language (SDL), then connect
Java code to its fields.

Start with three object types: a post, its author, and its comments. An
**object type** describes a value clients can select fields from; an **input
type** describes structured data clients can pass into an operation. `Query`
is the root for reads, and `Mutation` is the root for writes. In SDL, `!`
means a value is required, while brackets describe a list.

Here is the schema for the blog domain — the types, their fields, and the operations clients can call:

```graphql
type Post {
    id: ID!
    title: String!
    content: String
    comments: [Comment]
    authorId: String
    author: Author
}

type Author {
    id: ID!
    name: String!
    email: String!
    posts: [Post]
}

type Comment {
    id: ID!
    content: String!
    postId: String!
}

input CreatePostInput {
    title: String!
    content: String!
}

type Query {
    allPosts: [Post!]!
    postById(postId: ID!): Post
}

type Mutation {
    createPost(createPostInput: CreatePostInput!): Post!
}
```

The three object types are the nodes of the graph, `CreatePostInput` is the
argument you pass to create a post, and the `Query` and `Mutation` root types
are the entry points clients call.

## Generate project skeleton

Start from a standard Spring Boot skeleton. Scaffold it with
[Spring Initializr](https://start.spring.io/), selecting Web (Spring MVC),
Validation, JDBC API, PostgreSQL Driver, and Lombok.

Then add the DGS dependencies: import the
`graphql-dgs-platform-dependencies` BOM in `dependencyManagement`, and add
`dgs-starter` to the dependencies. The relevant parts of `pom.xml` look like
this:

```xml
<properties>
    <java.version>25</java.version>
    <dgs-platform.version>12.1.0</dgs-platform.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.netflix.graphql.dgs</groupId>
            <artifactId>graphql-dgs-platform-dependencies</artifactId>
            <version>${dgs-platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.netflix.graphql.dgs</groupId>
        <artifactId>dgs-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

- `dgs-starter` is the DGS auto-configuration. Since DGS 9 it is built on
  Spring GraphQL, so it also pulls in Spring GraphQL's execution and transport
  support.
- `spring-boot-starter-webmvc` provides the HTTP endpoint.
- `spring-boot-starter-validation` enables Bean Validation on input types.
- `spring-boot-starter-data-jdbc` and the PostgreSQL driver give us a
  `JdbcClient` for persistence.

## Set up database  

Next, create the tables in `src/main/resources/schema.sql`.
`spring.sql.init.mode=always` tells Spring Boot to run the script when the
application starts:

```sql
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS posts (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255),
    content    VARCHAR(255),
    status     VARCHAR(255) DEFAULT 'DRAFT',
    author_id  BIGINT REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comments (
    id         BIGSERIAL PRIMARY KEY,
    content    VARCHAR(255),
    post_id    BIGINT REFERENCES posts(id) ON DELETE CASCADE
);
```

For local development, run the database with Docker Compose and use
Testcontainers to start a throwaway PostgreSQL for integration tests.

An example `docker-compose.yml`:

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:15.3-alpine
    container_name: blog-postgres
    environment:
      POSTGRES_DB: blogdb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "user"]
      interval: 10s 
```      


Next, create Java records that represent the rows in those tables:

```java
// PostEntity.java
public record PostEntity(
        Long id,
        String title,
        String content,
        String status,
        Long authorId
) {

}
// CommentEntity.java
public record CommentEntity(
        Long id,
        String content,
        Long postId
) {
}
// AuthorEntity.java
public record AuthorEntity(
        Long id,
        String name,
        String email
) {

}
```

Then create a repository that handles persistence for posts:

```java
@Component
@RequiredArgsConstructor
public class PostRepository {
    private final JdbcClient jdbcClient;

    public List<PostEntity> findAll() {
        String sql = "SELECT * FROM posts";
        return this.jdbcClient.sql(sql)
                .query(PostRepository::mapPost)
                .list();
    }

    public List<PostEntity> findByAuthorId(Long authorId) {
        String sql = """
                SELECT * FROM posts
                WHERE author_id = :author_id
                """;
        return this.jdbcClient.sql(sql)
                .param("author_id", authorId)
                .query(PostRepository::mapPost)
                .list();
    }

    public PostEntity findById(Long id) {
        String sql = """
                SELECT * FROM posts
                WHERE id = :id
                """;
        return this.jdbcClient.sql(sql)
                .param("id", id)
                .query(PostRepository::mapPost)
                .optional()
                .orElse(null);
    }

    public Long create(String title, String content, String status, Long authorId) {
        String sql = """
                INSERT INTO posts (title, content, status, author_id)
                VALUES (:title, :content, :status, :author_id)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(sql)
                .param("title", title)
                .param("content", content)
                .param("status", status)
                .param("author_id", authorId)
                .update(keyHolder, "id");
        return keyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        String sql = "DELETE FROM posts";
        return this.jdbcClient.sql(sql).update();
    }

    private static PostEntity mapPost(ResultSet rs, int rowNum) throws SQLException {
        return new PostEntity(
                rs.getObject("id", Long.class),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getObject("author_id", Long.class)
        );
    }
}
```

`PostRepository` uses `JdbcClient` to run SQL and map rows to `PostEntity`.
The `create` method inserts a post and returns the generated id, and `mapPost`
maps a `ResultSet` row to a `PostEntity`.

Create similar repositories for comments and authors:

```java
// CommentRepository.java
@Component
@RequiredArgsConstructor
public class CommentRepository {
    private final JdbcClient jdbcClient;

    public List<CommentEntity> findAll() {
        String sql = "SELECT * FROM comments";
        return this.jdbcClient.sql(sql)
                .query(CommentRepository::mapComment)
                .list();
    }

    public List<CommentEntity> findByPostId(Long postId) {
        String sql = """
                SELECT * FROM comments
                WHERE post_id = :post_id
                """;
        return this.jdbcClient.sql(sql)
                .param("post_id", postId)
                .query(CommentRepository::mapComment)
                .list();
    }

    public List<CommentEntity> findByPostIdIn(List<Long> postId) {
        String sql = """
                SELECT * FROM comments
                WHERE post_id IN (:post_id)
                """;
        return this.jdbcClient.sql(sql)
                .param("post_id", postId)
                .query(CommentRepository::mapComment)
                .list();
    }

    public Long create(String content, Long postId) {
        String sql = """
                INSERT INTO comments (content, post_id)
                VALUES (:content, :post_id)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(sql)
                .param("content", content)
                .param("post_id", postId)
                .update(keyHolder, "id");
        return keyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        String sql = "DELETE FROM comments";
        return this.jdbcClient.sql(sql).update();
    }

    private static CommentEntity mapComment(ResultSet rs, int rowNum) throws SQLException {
        return new CommentEntity(
                rs.getLong("id"),
                rs.getString("content"),
                rs.getLong("post_id")
        );
    }
}

// AuthorRepository.java
@Component
@RequiredArgsConstructor
public class AuthorRepository {
    private final JdbcClient jdbcClient;

    public List<AuthorEntity> findAll() {
        String sql = "SELECT * FROM users";
        return this.jdbcClient.sql(sql)
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public AuthorEntity findById(Long id) {
        String sql = """
                SELECT * FROM users
                WHERE id = :id
                """;
        return this.jdbcClient.sql(sql)
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .optional()
                .orElse(null);
    }

    public List<AuthorEntity> findByIdIn(List<Long> id) {
        String sql = """
                SELECT * FROM users
                WHERE id IN (:id)
                """;
        return this.jdbcClient.sql(sql)
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public Long create(String name, String email) {
        String sql = """
                INSERT INTO users (name, email)
                VALUES (:name, :email)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(sql)
                .param("name", name)
                .param("email", email)
                .update(keyHolder, "id");
        return keyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        String sql = "DELETE FROM users";
        return this.jdbcClient.sql(sql).update();
    }

    private static AuthorEntity mapAuthor(ResultSet rs, int rowNum) throws SQLException {
        return new AuthorEntity(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email")
        );
    }
}
```

A `DataInitializer` bean implements `ApplicationRunner` and seeds a few posts
and comments on startup, so you have data to query right away:

```java
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
```

The database side is done. Next you map the schema to Java: first the GraphQL
type models, then the data fetchers that connect them to the repositories.

## GraphQL type models

The schema is the contract between client and server. Now create Java classes
that mirror its types. Here is the `Post` model:

```java
@Data
@Builder
@ToString
@Slf4j
public class Post {
    Long id;
    String title;
    String content;
    @Builder.Default
    PostStatus status = PostStatus.DRAFT;
    @Builder.Default
    List<Comment> comments = new ArrayList<>();
    Long authorId;
    Author author;

    public void addComment(Comment comment) {
        comment.setPostId(this.id);
        this.comments.add(comment);
        log.info("post comments: {}", this.comments);
    }
}
```

Create the remaining types — `Author`, `Comment`, and the two input types —
the same way:

```java

// Author.java
@Data
@Builder
@ToString
public class Author {
    Long id;
    String name;
    String email;
}

// Comment.java
@Data
@Builder
@ToString
public class Comment {
    Long id;
    Long postId;
    String content;
}

// CreatePostInput.java
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CreatePostInput {
    @NotEmpty
    @Length(min = 5, max = 100)
    String title;

    @NotEmpty
    String content;
}

// CommentInput.java
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CommentInput {

    @NotEmpty
    @Length(min = 10, max = 50)
    String content;

    @NotEmpty
    String postId;
}

// PostStatus.java
public enum PostStatus {
    DRAFT, PENDING_MODERATION, PUBLISHED
}
```

Input types can also declare validation constraints. Bean Validation checks
them before your service runs: `@NotEmpty` requires a value, and `@Length`
bounds the title's length.

## Data fetchers

**A data fetcher** is the resolver that supplies the value for a GraphQL
schema field. A request enters through a `Query` or `Mutation` fetcher; nested
fields such as `Post.author` are resolved separately when a client selects
them. Mark the containing class with `@DgsComponent`, then bind methods to
schema fields with DGS annotations.

The following implementation covers the request flow: list posts, find one
post, resolve its nested author and comments, and create a post.

When a query selects nested fields such as `Post.author`, each field is
resolved for every returned post. **The N+1 problem** occurs when this causes
one extra database or service call per post. A **DataLoader** collects those
lookup keys, batches them, and caches results for the current request. The
fetchers below use DataLoaders for authors and comments.

```java
@DgsComponent
@RequiredArgsConstructor
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

    @DgsData(parentType = "Post", field = "comments")
    public CompletableFuture<List<Comment>> comments(DgsDataFetchingEnvironment dfe) {
        DataLoader<Long, List<Comment>> dataLoader = dfe.getDataLoader(CommentsDataLoader.class);
        Post post = dfe.getSource();
        return dataLoader.load(post.getId());
    }

    @DgsMutation
    public Post createPost(@InputArgument("createPostInput") CreatePostInput input) {
        var id = this.postService.createPost(input);
        return this.postService.getPostById(id);
    }
}
```

The annotations, in order of appearance:

- `@DgsComponent` — a Spring stereotype that marks the class as a container of
  data fetchers.
- `@DgsQuery` / `@DgsMutation` — bind a method to a field on the `Query` /
  `Mutation` root type, matched by method name. `allPosts()` serves the
  `allPosts` field.
- `@DgsData(parentType = "Post", field = "author")` — binds a method to a
  nested field on any type. `dfe.getSource()` returns the parent `Post`.
- `@InputArgument` — binds a GraphQL argument to a parameter, coercing it to
  the parameter type. `@InputArgument("createPostInput")` names the argument
  explicitly because the parameter is `input`, not `createPostInput`.

`PostService` is the service layer: it delegates to the repositories and
converts database entities into GraphQL types.

```java
@Service
@RequiredArgsConstructor
public class PostService {
    private static final Function<PostEntity, Post> POST_MAPPER = p -> Post.builder()
            .id(p.id())
            .title(p.title())
            .content(p.content())
            .status(PostStatus.valueOf(p.status()))
            .authorId(p.authorId())
            .build();
    public static final Function<CommentEntity, Comment> COMMENT_MAPPER = c -> Comment.builder()
            .id(c.id())
            .content(c.content())
            .postId(c.postId())
            .build();
    final PostRepository posts;
    final CommentRepository comments;
    final AuthorRepository authors;

    public List<Post> getAllPosts() {
        return this.posts.findAll()
                .stream()
                .map(POST_MAPPER)
                .toList();
    }

    public Post getPostById(Long id) {
        var postEntity = this.posts.findById(id);
        return Optional.ofNullable(postEntity)
                .map(POST_MAPPER)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    List<Post> getPostsByAuthorId(Long id) {
        return this.posts.findByAuthorId(id)
                .stream()
                .map(POST_MAPPER)
                .toList();
    }

    public Long createPost(CreatePostInput postInput) {
        var authorId = this.authors.findAll().get(0).id();
        Long id = this.posts.create(postInput.getTitle(), postInput.getContent(), "DRAFT", authorId);
        return id;
    }

    public List<Comment> getCommentsByPostId(Long id) {
        return this.comments.findByPostId(id)
                .stream()
                .map(COMMENT_MAPPER)
                .toList();
    }

    public List<Comment> getCommentsByPostIdIn(Set<Long> ids) {
        return this.comments.findByPostIdIn(new ArrayList<>(ids))
                .stream()
                .map(COMMENT_MAPPER)
                .toList();
    }
}
```

The `authorsLoader` data loader implements the batched lookup. Register one
with `@DgsDataLoader`, and DGS gathers the keys the field fetchers request and
passes them to `load` in one call. Ten posts load their authors with a single
`WHERE id IN (...)` query instead of ten:

```java
@DgsDataLoader(name = "authorsLoader")
@RequiredArgsConstructor
public class AuthorsDataLoader implements BatchLoader<Long, Author> {
    private final AuthorService authorService;

    @Override
    public CompletionStage<List<Author>> load(List<Long> keys) {
        return CompletableFuture.supplyAsync(() -> this.authorService.getAuthorByIdIn(keys));
    }
}
```

In the `author` fetcher above, `getDataLoader("authorsLoader")` retrieves this
loader and `load(post.getAuthorId())` queues the lookup. Use the same pattern
for nested fields backed by another table or a remote call.

The `comments` fetcher is different because each key maps to a *list* of
comments. DGS provides `MappedBatchLoader` for exactly this case; it returns a
map from key to value. `CommentsDataLoader` loads all comments for the
requested posts and groups them by post id:

```java
@DgsDataLoader(name = "comments")
@RequiredArgsConstructor
@Slf4j
public class CommentsDataLoader implements MappedBatchLoader<Long, List<Comment>> {
    final PostService postService;

    @Override
    public CompletionStage<Map<Long, List<Comment>>> load(Set<Long> keys) {
        List<Comment> comments = this.postService.getCommentsByPostIdIn(keys);

        Map<Long, List<Comment>> mappedComments = new HashMap<>();
        keys.forEach(
                k -> mappedComments.put(k, comments
                        .stream()
                        .filter(c -> c.getPostId().equals(k)).toList())
        );
        log.info("mapped comments: {}", mappedComments);
        return CompletableFuture.supplyAsync(() -> mappedComments);
    }
}
```


## Error handling

**Error handling** converts exceptions thrown during field resolution into
errors in the GraphQL response. Implement `DataFetcherExceptionHandler` to
choose how known exceptions map to errors, while delegating unexpected ones to
the default handler. DGS's `TypedGraphQLError` can attach a well-known error
type such as `NOT_FOUND`:

```java
@Component
public class CustomDataFetchingExceptionHandler implements DataFetcherExceptionHandler {
    private final DefaultDataFetcherExceptionHandler defaultHandler =
            new DefaultDataFetcherExceptionHandler();

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        if (exception instanceof AuthorNotFoundException || exception instanceof PostNotFoundException) {
            GraphQLError error = TypedGraphQLError.newNotFoundBuilder()
                    .message(exception.getMessage())
                    .path(handlerParameters.getPath())
                    .build();
            return CompletableFuture.completedFuture(
                    DataFetcherExceptionHandlerResult.newResult().error(error).build());
        }
        return defaultHandler.handleException(handlerParameters);
    }
}
```

Now a `postById` for a missing id returns a typed `NOT_FOUND` error with a
clear message instead of a generic stack trace. The `dgs-webflux` module goes
one step further and also maps `ConstraintViolationException` to a typed
`BAD_REQUEST`, so validation failures are reported consistently.

Example code: [the `dgs` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs), [the `dgs-codegen` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-codegen), and [the `dgs-webflux` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-webflux).

## Build and run

Follow these steps to start the database and the application:

1. From the repository root, start the shared PostgreSQL container:

   ```bash
   docker compose up -d postgres
   ```

   The `docker-compose.yml` creates a `blogdb` database with user `user` and
   password `password`, matching `application.properties`.

2. Start the application from its module directory:

   ```bash
   cd dgs
   ./mvnw spring-boot:run
   ```

   On Windows, use `mvnw.cmd spring-boot:run`.

When the `Started DemoApplication` log line appears, the server is ready. Open
`http://localhost:8080/graphiql` in your browser.

## GraphiQL

**GraphiQL** is an in-browser GraphQL IDE. It reads the schema through
introspection and provides field completion, schema documentation, and a query
runner. Open the IDE at `http://localhost:8080/graphiql`; the schema explorer
is on the left and the operation editor and response are on the right.

Start by creating a post. A **mutation** changes server-side data; its
selection set specifies which fields the server should return after the
change.

Create a post with the `createPost` mutation:

```graphql
mutation {
  createPost(createPostInput: { title: "Hello DGS", content: "My first post" }) {
    id
    title
    content
  }
}
```

![Creating a post in GraphiQL](ui-createPost.png)

Next, read the data back. A **query** reads data without changing it.
`allPosts` returns every post, and the nested selections request each post's
author and comments:

```graphql
{
  allPosts {
    id
    title
    content
    author { name email }
    comments { id content }
  }
}
```

![Listing posts in GraphiQL](ui-allPosts.png)

Finally, use `postById` to request one post by id. GraphQL returns only the
fields in the selection set:

```graphql
{
  postById(postId: "1") {
    id
    title
    content
    author { name email }
    comments { id content }
  }
}
```

![Fetching a single post in GraphiQL](ui-postById.png)

Because the author and comments live in separate tables, `allPosts` is exactly
where the `DataLoader`s earn their keep — a plain nested fetch would run one
query per post.

Example code: [the `dgs` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs).

## Switching to WebFlux

**WebFlux** is Spring's reactive web stack. **R2DBC** provides reactive
relational database access, unlike the blocking JDBC API used above. The
`dgs-webflux` module keeps the same domain and schema but replaces the
servlet/JDBC dependencies with WebFlux and R2DBC:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Make the same change in your application: replace `webmvc` with `webflux` and
JDBC with R2DBC. Fetchers then return Reactor's `Mono` (zero or one value) or
`Flux` (a stream of values); the DGS annotations stay the same.

```java
@DgsComponent
@Validated
public class PostsDataFetcher {
    private final PostService postService;
    private final AuthorService authorService;

    @DgsQuery
    public Flux<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @DgsQuery
    public Mono<Post> postById(@InputArgument Long postId) {
        return this.postService.getPostById(postId);
    }

    @DgsMutation
    public Mono<Post> createPost(@InputArgument("createPostInput") @Valid CreatePostInput input) {
        return this.postService.createPost(input).flatMap(id -> this.postService.getPostById(id));
    }

    @DgsMutation
    public Mono<Comment> addComment(@InputArgument("commentInput") @Valid CommentInput input) {
        return this.postService.addComment(input);
    }

    @DgsSubscription
    Publisher<Comment> commentAdded() {
        return postService.commentAdded();
    }
}
```

There are two additions to notice:

- `@Valid` on the argument turns on Bean Validation for `CreatePostInput`.
- `@DgsSubscription` turns a method that returns a reactive `Publisher` into a
  live subscription — a stream of results pushed to the client, here over
  Server-Sent Events or WebSocket.

This module also adds a second mutation (`addComment`) and demonstrates the
subscription transport, so it is the one to copy when you need real-time
updates.

Example code: [the `dgs-webflux` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-webflux).

## Codegen for type safety

**Code generation** creates Java types and constants from the GraphQL schema,
reducing the chance that Java code and schema names drift apart. The
`dgs-codegen` module uses the DGS codegen Maven plugin to generate
`Post`, `Author`, `CreatePostInput`, and a `DgsConstants` class.

To try it, configure the plugin in `pom.xml` and add the generated sources to
the build:

```xml
<plugin>
    <groupId>io.github.deweyjose</groupId>
    <artifactId>graphqlcodegen-maven-plugin</artifactId>
    <version>${dgs-codegen-plugin.version}</version>
    <executions>
        <execution>
            <id>dgs-codegen</id>
            <goals><goal>generate</goal></goals>
            <configuration>
                <schemaPaths>
                    <param>${project.basedir}/src/main/resources/schema</param>
                </schemaPaths>
                <packageName>com.example.demo.gql</packageName>
                <generateClientApi>true</generateClientApi>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Because this is the Maven plugin rather than the Gradle one, you also add
`graphql-dgs-codegen-shared-core`, which supplies the base classes of the
generated client API.

The generated types follow the schema. This version adds an enum, a timestamp,
and two concepts used later in the tutorial:

```graphql
directive @uppercase on FIELD_DEFINITION
scalar LocalDateTime
scalar UUID

type Post {
    id: ID!
    title: String! @uppercase
    content: String
    comments: [Comment]
    status: PostStatus
    createdAt: LocalDateTime
    authorId: String
    author: Author
}

enum PostStatus {
    DRAFT, PENDING_MODERATION, PUBLISHED
}
```

An **enum** restricts a field to a known set of values. A **scalar** is a
single value such as a string or timestamp. A **directive** is metadata on a
schema element that can alter behavior; here, `@uppercase` decorates the
`title` field.

The fetchers now reference the generated constants instead of hard-coded
strings, and the generated types instead of hand-written ones:

```java
@DgsData(
    parentType = DgsConstants.POST.TYPE_NAME,
    field = DgsConstants.POST.Author
)
public CompletableFuture<Author> author(DgsDataFetchingEnvironment dfe) {
    DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
    Post post = dfe.getSource();
    return dataLoader.load(post.getAuthorId());
}
```

`DgsConstants.POST.TYPE_NAME` and `DgsConstants.POST.Author` are generated, so
a schema rename becomes a compile error rather than a silent typo.

Example code: [the `dgs-codegen` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-codegen).

## Custom scalars

A **scalar** is a leaf value in a GraphQL response: clients select it directly
but cannot request subfields from it. GraphQL has built-in scalars such as
`String` and `Int`; a **custom scalar** adds a domain-specific value type.
Declare `LocalDateTime` in the schema, then register a coercer with
`@DgsScalar` to convert between Java values and the string representation sent
over GraphQL:

```java
@DgsScalar(name = "LocalDateTime")
public class LocalDateTimeScalar implements Coercing<LocalDateTime, String> {

    @Override
    public String serialize(Object dataFetcherResult, GraphQLContext ctx, Locale locale) {
        if (dataFetcherResult instanceof LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
        }
        throw new CoercingSerializeException("Not a valid DateTime");
    }

    @Override
    public LocalDateTime parseLiteral(Value<?> input, CoercedVariables variables,
                                      GraphQLContext ctx, Locale locale) {
        if (input instanceof StringValue value) {
            return LocalDateTime.parse(value.getValue(), DateTimeFormatter.ISO_DATE_TIME);
        }
        throw new CoercingParseLiteralException("Value is not a valid ISO date time");
    }
}
```

`serialize` turns a `LocalDateTime` into the ISO string sent to the client;
`parseValue`/`parseLiteral` do the reverse for input. A `UUIDScalar` does the
same for `java.util.UUID`. The `graphql-dgs-extended-scalars` dependency ships
ready-made scalars for common types if you would rather not write your own.

Example code: [the `dgs-codegen` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-codegen).

## Custom directives

A **directive** is an annotation in the schema that adds behavior to a type,
field, or argument. Define its location in SDL, then implement
`SchemaDirectiveWiring` to wrap or replace the field's data fetcher. The
`@uppercase` directive changes the value returned for the decorated field:

```java
@DgsDirective(name = "uppercase")
public class UppercaseDirectiveWiring implements SchemaDirectiveWiring {

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
        var field = env.getElement();
        var parentType = env.getFieldsContainer();
        var coordinates = FieldCoordinates.coordinates(parentType, field.getName());
        var original = env.getCodeRegistry().getDataFetcher(coordinates, field);

        var dataFetcher = DataFetcherFactories.wrapDataFetcher(original, (dfe, value) ->
                value instanceof String s ? s.toUpperCase() : value);

        env.getCodeRegistry().dataFetcher(coordinates, dataFetcher);
        return field;
    }
}
```

Because the schema marks `title: String! @uppercase`, every `title` that comes
back from the server is upper-cased — the directive changed the field's
behavior without touching the fetcher.

Example code: [the `dgs-codegen` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-codegen).

## Typesafe client

**A typesafe GraphQL client** builds operations and field selections using
generated Java classes rather than hand-written query strings. The
`dgs-client` module is a separate command-line application: it declares the
same schema, enables `generateClientApi`, and depends on
`graphql-dgs-client` and `spring-boot-starter-restclient`.

The generated client API has two parts. A query object identifies the
operation, while a projection describes the fields requested in the selection
set. Build a request like this:

```java
GraphQLQueryRequest request = new GraphQLQueryRequest(
        new AllPostsGraphQLQuery(),
        new AllPostsProjectionRoot<AuthorProjection<?, ?>, PostProjection<?, ?>>()
                .id().title().content().status().parent()
                .author().id().name().email().parent()
                .comments().id().content().parent()
                .createdAt());

String query = request.serialize();
```

`request.serialize()` produces the GraphQL document. You then execute it with
the DGS client, which rides Spring's `RestClient`:

```java
DgsGraphQLClient client =
        new DgsRestClientGraphQLClient(RestClient.builder().baseUrl(url).build());

DgsGraphQLResponse response = client.executeQuery(query, Map.of());
List<Post> posts = response.extractValueAsObject("allPosts", new TypeRef<List<Post>>() {});
```

The projections are typesafe: you can only select fields that exist on the
type, and `extractValueAsObject` deserializes into the generated `Post`. The
module's test stubs the `/graphql` endpoint with WireMock and verifies the
request body semantically, so the client is exercised without a running server.

Example code: [the `dgs-client` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-client).

## File upload

**File upload** is not defined by the GraphQL specification, so applications
need a transport convention to send files alongside GraphQL operations. The
`dgs-fileupload` module uses the GraphQL multipart request specification,
implemented for Spring GraphQL by the `multipart-spring-graphql` library.
First, add the library dependency:

```xml
<dependency>
    <groupId>name.nkonev.multipart-spring-graphql</groupId>
    <artifactId>multipart-spring-graphql</artifactId>
    <version>${multipart-spring-graphql.version}</version>
</dependency>
```

Next, declare an `Upload` scalar and use it in a mutation. The scalar represents
the uploaded file in the GraphQL schema:

```graphql
scalar Upload

type Mutation {
    upload(file: Upload!): Boolean
    uploadWithDesc(desc: String, file: Upload!): Boolean
    uploads(files: [Upload!]!): Boolean
}
```

Finally, read the upload from the data-fetching environment rather than
`@InputArgument`. A `MultipartFile` is supplied by the multipart transport
and cannot be deserialized from a normal GraphQL input object by Jackson:

```java
@DgsComponent
public class FileUploadMutation {

    @DgsMutation
    public Boolean upload(DgsDataFetchingEnvironment env) {
        var file = (MultipartFile) env.getArgument("file");
        // read file.getInputStream(), file.getOriginalFilename(), file.getContentType()
        return true;
    }
}
```

The multipart parsing happens in the transport layer; your fetcher only sees
the resolved `MultipartFile`.

References: [GraphQL multipart request specification](https://github.com/jaydenseric/graphql-multipart-request-spec) and [multipart-spring-graphql](https://github.com/nkonev/multiple-spring-graphql).

Example code: [the `dgs-fileupload` module](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-fileupload).

## Conclusion

You now have a working DGS application and a tour of its extension points. The
base `dgs` module shows the core programming model; `dgs-webflux` the reactive
stack and subscriptions; `dgs-codegen` generated types, custom scalars, and
directives; `dgs-client` the generated client; and `dgs-fileupload` multipart
uploads.

From here:

- Read the [DGS documentation](https://netflix.github.io/dgs/) for federation,
  testing, and the full annotation reference.
- See [DGS or Spring GraphQL?](../intro.md#how-dgs-adopted-spring-graphql) for
  how DGS relates to the Spring team's framework.

Related examples: [dgs-kotlin](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-kotlin) and [dgs-kotlin-co](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-kotlin-co) show the same domain in Kotlin and coroutines.
