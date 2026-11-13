# Spring GraphQL

[Spring GraphQL](https://spring.io/projects/spring-graphql) is the Spring
team's official GraphQL support, built on [GraphQL Java](../java/index.md).
Version 1.0 arrived in May 2022, and Spring Boot 2.7 introduced
`spring-boot-starter-graphql`. It provides the transport, execution, testing,
and client infrastructure that the other frameworks in this repository either
use directly (DGS) or preceded (Kickstart).

## The controller model

Spring GraphQL is schema-first: SDL files live under
`src/main/resources/graphql/` by default. Annotated controllers bind methods
to schema fields, in the same style as Spring MVC. The following excerpt is
from
[PostController.java](https://github.com/hantsy/spring-graphql-sample/blob/master/spring-graphql-webmvc-http/src/main/java/com/example/demo/gql/PostController.java)
in the
[spring-graphql-webmvc-http](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-webmvc-http)
example:

```java
@Controller
public class PostController {
    private final PostService postService;

    @QueryMapping
    public List<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @QueryMapping
    public Post postById(@Argument("postId") String id) {
        return this.postService.getPostById(id);
    }

    @MutationMapping
    public Post createPost(@Argument("createPostInput") @Valid CreatePostInput input) {
        return postService.createPost(input);
    }

    @SubscriptionMapping
    public Publisher<Comment> commentAdded() {
        return this.postService.commentAdded();
    }

    @SchemaMapping(field = "author")
    public CompletionStage<Author> authorOfPost(
            Post post, DataLoader<String, Author> authorDataLoader) {
        return authorDataLoader.load(post.getAuthorId());
    }
}
```

The annotations mirror Spring MVC's mapping style:

- `@QueryMapping`, `@MutationMapping`, `@SubscriptionMapping` — bind a method
  to a root-type field, matched by method name.
- `@SchemaMapping` — bind a method to a field on any type; the parent object
  arrives as a parameter.
- `@Argument` — bind a GraphQL argument, with coercion and Bean Validation
  (`@Valid`) support.

Controller methods can return plain values, `CompletionStage`, or reactive
types (`Mono`, `Flux`, `Publisher`).

## Data loading

Spring GraphQL wraps GraphQL Java's DataLoader mechanism in a
`BatchLoaderRegistry`. Register loaders by name or by key/value type pair,
and inject them into controller methods:

```java
registry.forTypePair(String.class, Author.class)
        .registerBatchLoader((keys, env) ->
                Flux.fromIterable(authorService.getAuthorByIdIn(keys)));
```

The `authorOfPost` method above receives the loader as a parameter — no
string lookup required. See [GraphQL Java](../java/index.md) for the N+1
problem this solves.

## Transports

Spring GraphQL separates execution from transport, and the same application
code runs on every supported transport:

| Transport | Operations | Repository example |
| --------- | ---------- | ------------------ |
| WebMvc or WebFlux over HTTP | Queries, mutations; subscriptions over SSE | `spring-graphql-webmvc-http`, `spring-graphql-webflux-http` |
| HTTP + WebSocket | Queries and mutations over HTTP; subscriptions over `graphql-transport-ws` | `spring-graphql-webmvc-http-ws`, `spring-graphql-webflux-http-ws` |
| WebSocket only | Every operation over `graphql-transport-ws` | `spring-graphql-webmvc-ws`, `spring-graphql-webflux-ws` |
| RSocket | Every operation over RSocket | `spring-graphql-rsocket-kotlin-co` |

Configuration lives under `spring.graphql.*`, for example the WebSocket
endpoint:

```properties
spring.graphql.websocket.path=/subscriptions
```

## Testing

Spring GraphQL ships a dedicated testing API, demonstrated in three tiers by
every `spring-graphql-web*` module:

- `@GraphQlTest` slices with a protocol-neutral `GraphQlTester` bound to the
  in-JVM execution service — fast tests with the service layer mocked.
- `HttpGraphQlTester` and `WebSocketGraphQlTester` — the same tester API over
  real transports against a running application.
- The runtime client API — `HttpSyncGraphQlClient` (on `RestClient`),
  `HttpGraphQlClient` (on `WebClient`), and `WebSocketGraphQlClient` — the
  code a real client writes, and the tier that proves SSE subscriptions.

## Clients

The same `GraphQlClient` API used in tests is the production client:
`HttpSyncGraphQlClient` for blocking HTTP, `HttpGraphQlClient` for reactive
HTTP and SSE, and `WebSocketGraphQlClient` for WebSocket. The
[dgs-client](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-client)
example shows the DGS alternative, a typesafe client generated from the
schema.

## Security and observability

Spring GraphQL integrates with Spring Security for authentication and
authorization at the transport and data-fetcher level, and with Micrometer
for metrics and tracing of execution. Both are standard Spring Boot
auto-configurations.