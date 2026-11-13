# An introduction to GraphQL and Spring

GraphQL is more and more popular in developer communities. What began as an
internal Facebook tool for mobile apps is now a cross-ecosystem standard, with
servers and tooling for nearly every language and platform. On the JVM, that
means one shared engine — GraphQL Java — and two Spring Boot frameworks built
on top of it.

This chapter is for Java developers who know Spring and want to understand
GraphQL and the frameworks that run it on the JVM. The examples use Java 25 and
Spring Boot 4.1.2 / Spring Framework 7, with some written in Kotlin (Kotlin
2.4.x). This chapter is not a Spring tutorial; it assumes you can already build
a Spring Boot application.

## What is GraphQL

GraphQL is a query language for APIs and a runtime for executing those queries
against your data. Facebook's mobile apps have been powered by GraphQL since
2012. The specification was open-sourced in 2015 and is now maintained by the
[GraphQL Foundation](https://graphql.org/foundation/), hosted by the Linux
Foundation since 2018.

A GraphQL server exposes a single endpoint and a strongly typed schema. The
client sends a document that describes exactly the data it needs, and the
server responds with JSON in the same shape. Compared with a REST API, this
moves the choice of response shape from the server to the client.

The specification defines three kinds of operations:

- **Query** — read data.
- **Mutation** — write data, executed serially in the order the document lists
  them.
- **Subscription** — a long-lived operation that pushes a stream of results to
  the client as events happen.

The specification deliberately says nothing about the transport. HTTP and
WebSocket are the common choices, and separate specifications cover them.

## Why GraphQL instead of REST

GraphQL is not a wholesale replacement for REST. It is a different trade-off
that fits the case where clients are many, diverse, and evolve independently
of the server.

- **No over-fetching.** A REST endpoint returns a fixed payload: a client that
  needs only a post's title still receives the whole post, its author, and its
  comments. A GraphQL query asks for exactly the fields it needs.
- **No under-fetching.** A screen that needs a post *and* its author calls two
  REST endpoints, or a bespoke composite endpoint, and assembles the result
  with several round trips. A GraphQL query nests the author under the post
  and resolves both in one request.
- **A typed, introspectable contract.** The schema is machine-readable, so
  tools generate clients, validate queries, and offer autocomplete (GraphiQL,
  Apollo Client) without a separate API-description layer such as OpenAPI.
- **One endpoint.** GraphQL has no per-resource URLs and no version in the
  URL. Adding a field is additive and does not break existing clients.

The costs sit on the server: GraphQL makes HTTP caching harder, moves query
cost and validation onto the server, and has no built-in file upload — that
needs a separate multipart specification. REST remains the simpler fit for
small, resource-oriented, cacheable APIs.

## Schema and SDL

A GraphQL schema is a contract written in the Schema Definition Language
(SDL). It declares the types a server exposes and the operations clients can
run. The following schema is the one used by the
[dgs](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs) example
(comments and formatting trimmed):

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

The building blocks are:

- **Object types** (`type Post`) — named collections of fields. Each field has
  a type and can take arguments, such as `postById(postId: ID!)`.
- **Scalar types** — the leaf values. The built-in scalars are `Int`, `Float`,
  `String`, `Boolean`, and `ID`. Servers can add custom scalars such as `Date`
  or `Upload`.
- **Input types** (`input CreatePostInput`) — structured arguments for
  mutations. Only input types, scalars, and enums can be used as input.
- **Enums** — a fixed set of named values, for example
  `enum PostStatus { DRAFT PUBLISHED }`.
- **Interfaces and unions** — abstract types that let a field return one of
  several object types. A type resolver picks the concrete type at runtime.
- **Non-null (`!`) and lists (`[]`)** — modifiers. `Post!` never returns null,
  `[Post!]!` is a non-null list of non-null posts, and `[Post]` can be null and
  can contain nulls.
- **Directives** — annotations such as `@deprecated(reason: "...")` that change
  how the schema or a document executes. Servers can define custom directives.

Three root types anchor every schema:

- `Query` — the entry point for reads.
- `Mutation` — the entry point for writes. Top-level mutation fields execute
  serially, in document order; query fields can execute in parallel.
- `Subscription` — the entry point for event streams. A subscription field
  returns a stream of results rather than a single value.

The names are conventions; a `schema` declaration can rename them, but most
servers — and every example in this repository — use the defaults.

A schema is also queryable. Clients ask the server for its own types through
the meta-fields `__schema` and `__type`, which is how tools such as GraphiQL
provide completion and documentation:

```graphql
{
  __schema {
    types {
      name
    }
  }
}
```

Introspection is part of the specification, so it works against any compliant
server. The specification is published in dated editions (June 2018, October
2021, September 2025), with a draft of the next edition always available.

## The request and response envelope

### The request

A GraphQL request carries three members:

- `query` — the document: one or more operations, each a `query`, `mutation`,
  or `subscription`.
- `variables` — a JSON object of values for the variables the document
  declares. Optional.
- `operationName` — which operation to run when the document contains more
  than one. Optional.

Over HTTP, the client POSTs this as a JSON body. The following request reads
every post with its title and author name:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ allPosts { id title author { name } } }"}'
```

Never build a document by concatenating user input into the query string.
Declare variables instead, and let the server coerce and validate them:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query PostById($postId: ID!) { postById(postId: $postId) { title content } }",
    "variables": { "postId": "1" },
    "operationName": "PostById"
  }'
```

The document declares `$postId: ID!`; the `variables` object supplies the
value. The server validates the document against the schema before any data
fetcher runs. A mutation uses the same envelope:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation CreatePost($input: CreatePostInput!) { createPost(createPostInput: $input) { id title } }",
    "variables": { "input": { "title": "Hello GraphQL", "content": "First post" } }
  }'
```

### The response

A GraphQL response is a JSON object with up to three members:

- `data` — the result, in the same shape as the operation's selection set.
- `errors` — a list of errors raised while validating or executing the
  operation. Present only when something went wrong.
- `extensions` — an optional map for server-specific additions, such as
  tracing data.

A successful response to the `allPosts` query looks like this:

```json
{
  "data": {
    "allPosts": [
      { "id": "1", "title": "Hello GraphQL", "author": { "name": "Hantsy" } }
    ]
  }
}
```

GraphQL distinguishes request errors from field errors. A request error — a
syntax error, a validation failure, an unknown operation — rejects the whole
operation, and the response has `errors` and no `data`. A field error — a data
fetcher that throws — nulls out only that field, and the rest of the response
still returns:

```json
{
  "data": {
    "postById": {
      "title": "Hello GraphQL",
      "author": null
    }
  },
  "errors": [
    {
      "message": "Failed to load author",
      "locations": [{ "line": 1, "column": 40 }],
      "path": ["postById", "author"]
    }
  ]
}
```

Each error carries a `message`, the `locations` in the document that caused
it, and the `path` to the field that failed. If a non-null field (`Author!`)
errors, the null propagates up to the nearest nullable parent — the schema's
nullability rules decide how much of the response survives.

## Runtime protocols

The GraphQL specification does not define a transport. Two conventions cover
the real world: HTTP for queries and mutations, and WebSocket (or Server-Sent
Events) for subscriptions.

### HTTP

A server must support GraphQL requests via POST with a UTF-8 JSON body and the
`Content-Type: application/json` header. Every example in this repository
accepts POST on `/graphql`. A server may also support GET for read-only
queries, with the request members moved into the URL query string; GET must
not execute mutations (the recommended response is `405 Method Not Allowed`).
GET is useful for caching but optional — do not rely on it being enabled.

Two media types matter. `application/json` is the legacy request and response
type. `application/graphql-response+json` is the newer response type, sent in
the `Accept` header, typically as
`application/graphql-response+json, application/json;q=0.9` so older servers
can fall back. The media type decides how errors map to HTTP status codes:
with `application/graphql-response+json`, a request-level error produces a
`4xx` or `5xx`, while a well-formed operation that executes — even with field
errors — produces `2xx`. With legacy `application/json`, most servers return
`200 OK` for any well-formed response and signal problems only in the `errors`
member. A GraphQL client must always inspect the response body: a `200` status
does not mean every field succeeded.

### Subscriptions over SSE

Subscriptions need a channel that stays open. Over plain HTTP, the client
POSTs the subscription document with `Accept: text/event-stream`, and the
server keeps the response open and writes one `data:` event per result. The
`-http` modules — for example
[spring-graphql-webmvc-http](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-webmvc-http)
— serve queries, mutations, and SSE subscriptions on the same `/graphql`
endpoint.

### Subscriptions over WebSocket

A subscription over WebSocket uses a subprotocol, and two exist that are
**not** compatible with each other. The first,
[subscriptions-transport-ws](https://github.com/apollographql/subscriptions-transport-ws)
(Apollo, 2016), is no longer maintained. The actively maintained replacement
is [graphql-ws](https://github.com/enisdenjo/graphql-ws), whose
[GraphQL over WebSocket Protocol](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md)
uses the subprotocol `graphql-transport-ws` and lets any operation — query,
mutation, or subscription — run over the socket:

| Message | Direction | Purpose |
| ------- | --------- | ------- |
| `ConnectionInit` / `ConnectionAck` | client ↔ server | Open the connection; operations are allowed only after the ack. |
| `Subscribe` | client → server | Start an operation under a unique id. |
| `Next` | server → client | One execution result from the operation's stream. |
| `Error` | server → client | Execution errors for the operation; the stream finishes. |
| `Complete` | either | The stream finishes; the client can also send it to unsubscribe. |
| `Ping` / `Pong` | either | Bidirectional heartbeat. |

Because the protocols are distinct, migrating means upgrading the client and
the server together — a `graphql-ws` client cannot talk to a
`subscriptions-transport-ws` server. The WebSocket examples in this repository
all speak `graphql-transport-ws`.

### File upload

The specification does not define file uploads. The interoperable standard is
the
[GraphQL multipart request specification](https://github.com/jaydenseric/graphql-multipart-request-spec),
which wraps the usual `operations` JSON in a multipart form and maps file
fields to object paths. Both DGS and Spring GraphQL resolve the uploaded file
to a Spring `MultipartFile` argument; the
[dgs-fileupload](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-fileupload)
example demonstrates it.

## GraphQL Java

[GraphQL Java](https://www.graphql-java.com/) is the JVM implementation of the
GraphQL specification, started in 2015. It is an execution engine, not a
server framework: it parses schemas, validates documents, and executes
operations, but it knows nothing about HTTP, WebSocket, or Spring. Every
framework in this repository — Spring GraphQL, Netflix DGS, and the legacy
Kickstart and SPQR examples — embeds it and delegates execution to it.

### Build the schema

Execution starts with a `GraphQLSchema`. The schema-first path parses SDL into
a `TypeDefinitionRegistry`, then combines it with a `RuntimeWiring` to produce
an executable schema:

```java
TypeDefinitionRegistry typeRegistry =
        new SchemaParser().parse(new ClassPathResource("/schema/schema.graphql").getFile());

GraphQLSchema graphQLSchema =
        new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);
```

`RuntimeWiring` is where behavior attaches to the type system: data fetchers,
custom scalars, directives, and type resolvers for interfaces and unions:

```java
RuntimeWiring.newRuntimeWiring()
        .codeRegistry(codeRegistry)
        .scalar(Scalars.localDateTimeType())
        .scalar(Scalars.uuidType())
        .directive("uppercase", new UpperCaseDirectiveWiring())
        .build();
```

A code-first path also exists — building `GraphQLSchema` programmatically with
builders instead of SDL — but every example in this repository is schema-first.

### Fetch data

A `DataFetcher<T>` resolves one field. It receives a `DataFetchingEnvironment`
with the field arguments, the parent object, the execution context, and the
data loader registry:

```java
public DataFetcher<Post> postById() {
    return (DataFetchingEnvironment dfe) -> {
        String postId = dfe.getArgument("postId");
        return postService.getPostById(postId);
    };
}
```

Fields without an explicit fetcher fall back to `PropertyDataFetcher`, which
reads a matching property from the parent object — this is why a `Post` record
with a `title` accessor needs no wiring for its `title` field. A fetcher can
return a plain value, a `CompletionStage` for asynchronous work, or a reactive
`Publisher` for subscription fields.

### Execute operations

The `GraphQL` object is the engine entry point. Build it once from the schema,
then execute an `ExecutionInput` per request:

```java
GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

ExecutionInput input = ExecutionInput.newExecutionInput()
        .query("{ allPosts { id title } }")
        .build();

ExecutionResult result = graphQL.execute(input);
Map<String, Object> data = result.getData();
List<GraphQLError> errors = result.getErrors();
```

`executeAsync` returns a `CompletableFuture<ExecutionResult>` instead. The
`ExecutionResult` maps directly to the response envelope. The engine picks an
execution strategy per operation type: query fields resolve in parallel,
mutation fields resolve serially in document order, and subscription fields
resolve to a stream that the transport layer forwards to the client.

### Batch with DataLoader

Naive field resolution produces the N+1 problem: loading ten posts and then
each post's comments runs eleven queries. The
[java-dataloader](https://github.com/graphql-java/java-dataloader) library — a
separate project under the GraphQL Java organization — batches and caches
loads per request. Before, one query per post:

```sql
SELECT * FROM comments WHERE post_id = ?
SELECT * FROM comments WHERE post_id = ?
SELECT * FROM comments WHERE post_id = ?
```

After, one batched query:

```sql
SELECT * FROM comments WHERE post_id in (?, ?, ?, ?)
```

A fetcher asks the environment for a registered loader and loads a key; the
loader collects keys during the execution tick and dispatches them as one
batch:

```java
DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
return dataLoader.load(post.getAuthorId());
```

Loaders are registered in a `DataLoaderRegistry` keyed by name. Spring GraphQL
and DGS both build on this same mechanism.

### GraphQL in the Java ecosystem

GraphQL on the JVM has gone through several generations of integration
libraries. GraphQL Java is the low-level engine; Netflix DGS and Spring
GraphQL are the two actively maintained Spring Boot frameworks built on top
of it. The framework links come from the
[README](https://github.com/hantsy/spring-graphql-sample#readme).

- **2015: GraphQL Java.** Facebook open-sourced the specification, and GraphQL
  Java started the same year as the JVM implementation — schema parsing,
  validation, and execution, but no way to serve requests.
- **2016: graphql-java-servlet.** A servlet endpoint that accepted GraphQL
  requests over HTTP and handed them to a `GraphQL` instance, with an early
  WebSocket subscription implementation.
- **The Kickstart era.**
  [GraphQL Java Kickstart](https://www.graphql-java-kickstart.com/) built the
  most popular Spring Boot integration of that period on
  [GraphQL Java Tools](https://github.com/graphql-java-kickstart/graphql-java-tools)
  (schema-first resolver wiring) and the
  [GraphQL Spring Boot Starters](https://github.com/graphql-java-kickstart/graphql-spring-boot)
  (auto-configuration for the servlet, GraphiQL, and subscriptions). The
  starters are now archived: "Since Spring now officially supports GraphQL we
  have decided to archive this project."
- **The code-first alternative: SPQR.**
  [GraphQL SPQR](https://github.com/leangen/graphql-spqr) annotated plain Java
  methods and generated the schema from the code. Development is no longer
  active, but the code-first approach lives on in
  [MicroProfile GraphQL](https://github.com/eclipse/microprofile-graphql),
  implemented by
  [SmallRye GraphQL](https://github.com/smallrye/smallrye-graphql) and
  supported in Quarkus.
- **2021: Netflix DGS.** Netflix open-sourced the
  [DGS (Domain Graph Service) framework](https://netflix.github.io/dgs/) in
  February 2021 after years of internal use — an annotation-based Spring Boot
  programming model, schema code generation, federation support, and a
  query-test framework.
- **2021–2022: Spring GraphQL.** The Spring team built
  [Spring GraphQL](https://github.com/spring-projects/spring-graphql) directly
  on GraphQL Java. Version 1.0 arrived in May 2022, and Spring Boot 2.7
  shipped `spring-boot-starter-graphql` — GraphQL became an official part of
  the Spring portfolio.

The ecosystem today:

| Project | Approach | Status |
| ------- | -------- | ------ |
| [GraphQL Java](https://www.graphql-java.com/) | Execution engine | Active; the foundation of everything below. |
| [Spring GraphQL](https://github.com/spring-projects/spring-graphql) | Official Spring integration, schema-first controllers | Active; the Spring baseline. |
| [Netflix DGS](https://netflix.github.io/dgs/) | Annotation model, codegen, federation, on Spring GraphQL | Active. |
| [GraphQL Kotlin](https://opensource.expediagroup.com/graphql-kotlin/docs) | Code-first Kotlin, coroutines (ExpediaGroup) | Active. |
| [GraphQL Java Kickstart](https://www.graphql-java-kickstart.com/) | Schema-first tools + Spring Boot starters | Starters archived; use Spring GraphQL. |
| [GraphQL SPQR](https://github.com/leangen/graphql-spqr) | Code-first schema generation | Inactive. |

Outside Spring, the author's companion repositories show GraphQL Java with
[Quarkus](https://github.com/hantsy/quarkus-sandbox) and
[Vert.x](https://github.com/hantsy/vertx-sandbox). The `legacy/`
folder in this repository preserves the discontinued stacks (Kickstart, SPQR,
vanilla GraphQL Java) on the Spring Boot and Java versions they were last
verified with; the active modules track current Spring Boot and either Spring
GraphQL or DGS.

## Netflix DGS and Spring GraphQL

Two Spring Boot frameworks dominate GraphQL on the JVM today, and since 2024
they are no longer competitors at the engine level: the Netflix DGS framework
is built on Spring GraphQL.

### Netflix DGS

The [DGS (Domain Graph Service) framework](https://netflix.github.io/dgs/) is
Netflix's GraphQL server framework for Spring Boot, open-sourced in 2021. It
is schema-first: you write SDL under `src/main/resources/schema/`, then
implement data fetchers as annotated Spring components:

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

The annotations map directly to the schema: `@DgsComponent` marks the class as
a data fetcher container, `@DgsQuery` / `@DgsMutation` / `@DgsSubscription`
bind a method to a root-type field, `@DgsData(parentType = "...",
field = "...")` binds a method to any field on any type, and `@InputArgument`
binds a GraphQL argument to a method parameter. Subscriptions return a
reactive `Publisher`.

!!! note
    The DGS framework itself is written mainly in Kotlin. This is an
    implementation detail — DGS works equally well from Java, and this
    repository demonstrates both the Java (`dgs`) and Kotlin (`dgs-kotlin`,
    `dgs-kotlin-co`) styles.

DGS also ships a codegen plugin that generates Java or Kotlin types from the
schema (the [dgs-codegen](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-codegen)
example), first-class Apollo Federation support for federated subgraphs, and a
test framework (`DgsQueryExecutor`, `@DgsTest`).

### Spring GraphQL

[Spring GraphQL](https://spring.io/projects/spring-graphql) is the Spring
team's official GraphQL support, built on GraphQL Java. Version 1.0 arrived in
May 2022, and Spring Boot 2.7 introduced `spring-boot-starter-graphql`. It is
also schema-first, with SDL files under `src/main/resources/graphql/` and
annotated controllers in the style of Spring MVC:

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

`@QueryMapping`, `@MutationMapping`, and `@SubscriptionMapping` bind a method
to a root-type field; `@SchemaMapping` binds a method to a field on any type;
`@Argument` binds a GraphQL argument with coercion and Bean Validation
(`@Valid`) support. Methods can return plain values, `CompletionStage`, or
reactive types.

Spring GraphQL separates execution from transport, so the same application
code runs on WebMvc, WebFlux, WebSocket, and RSocket. It wraps GraphQL Java's
DataLoader mechanism in a `BatchLoaderRegistry`, ships a dedicated testing API
(`@GraphQlTest` slices with a protocol-neutral `GraphQlTester`, plus
`HttpGraphQlTester` and `WebSocketGraphQlTester`), and provides the production
client API (`HttpSyncGraphQlClient`, `HttpGraphQlClient`,
`WebSocketGraphQlClient`).

### The integration

Netflix open-sourced DGS in 2021 and it became the most widely adopted GraphQL
Java framework. Soon after, the DGS team learned of a parallel effort by the
Spring team — Spring for GraphQL — and over the following year Spring GraphQL
matured to rough feature parity with DGS. The result was two frameworks
solving the same problem: new users had to choose, the maintainers duplicated
work, and the community's feedback split across two projects.

Neither could simply disappear. DGS is deeply embedded in many companies'
architectures (including Netflix's), so forcing a migration would be costly
with no benefit; at the same time, Spring deserves an out-of-the-box GraphQL
offering. The teams chose integration over competition. DGS keeps its
programming model and tooling but drops the parts that duplicated Spring
GraphQL's low-level WebMvc/WebFlux integration. The integration shipped first
as an opt-in `spring-graphql` flavor of the DGS starter, then became the
default in DGS 9.0 (May 2024), which builds against Spring GraphQL 1.3, Spring
Boot 3.3, and GraphQL Java 22.

After the integration:

- You write data fetchers with the DGS programming model (`@DgsComponent`,
  `@DgsQuery`), the Spring GraphQL programming model (`@QueryMapping`), or
  both in the same application.
- DGS builds the `GraphQLSchema` from its programming model and schema files.
- Spring GraphQL handles the transports and executes every operation through
  its `ExecutionGraphQlService`.
- `DgsQueryExecutor` becomes a proxy over `ExecutionGraphQlService`, so
  existing DGS test code keeps working unchanged.

One engine, one transport layer, two programming models. For your code the
integration is mostly a drop-in replacement — the areas to review when
upgrading are the ones where the frameworks overlapped: configuration
properties, file uploads, WebSocket subscriptions, schema inspection, and test
setup. See Netflix's
[Spring GraphQL Integration](https://netflix.github.io/dgs/spring-graphql-integration/)
document for the full list.

### The difference

Since the integration, DGS and Spring GraphQL share the same foundation: both
run on Spring Boot, both execute with GraphQL Java, and DGS additionally uses
Spring GraphQL's execution service and transports. The differences that remain
are the programming model and the tooling around it:

| Aspect | Netflix DGS | Spring GraphQL |
| ------ | ----------- | -------------- |
| Programming model | `@DgsComponent` classes with `@DgsQuery`, `@DgsData`, `@InputArgument` | `@Controller` classes with `@QueryMapping`, `@SchemaMapping`, `@Argument` |
| Schema location | `src/main/resources/schema/` | `src/main/resources/graphql/` |
| Code generation | Official Gradle/Maven plugin generates types from the schema | No built-in codegen; use a third-party plugin if wanted |
| Federation | Built-in subgraph support | Not built in; add a federation library |
| Testing | `DgsQueryExecutor`, `@DgsTest` | `GraphQlTester`, `@GraphQlTest`, transport testers |
| Client | Typesafe client generated from the schema | `HttpSyncGraphQlClient`, `HttpGraphQlClient`, `WebSocketGraphQlClient` |
| Transports | HTTP, SSE, WebSocket (via Spring GraphQL) | HTTP, SSE, WebSocket, RSocket |
| Stewardship | Netflix | Spring team |
| Dependency management | DGS platform BOM aligned to a Boot version | Spring Boot starter, version managed by Boot |

Both support Kotlin and coroutines, Spring Security, file uploads through
`MultipartFile`, and DataLoader-based batching.

### Choosing a framework

- **Starting a new Spring Boot application** — choose Spring GraphQL. It is
  the official Spring baseline, its versions move with Spring Boot, and its
  controller model matches the rest of a Spring application.
- **Running DGS today** — stay on DGS and keep upgrading. Since DGS 9 you are
  already on Spring GraphQL underneath; a rewrite buys nothing, and you can
  mix Spring GraphQL APIs into a DGS application where they help.
- **Needing codegen or federation** — DGS's generated types and built-in
  federation support are the strongest reasons to pick it for a new project.
- **Preferring code-first Kotlin** — look at
  [GraphQL Kotlin](https://opensource.expediagroup.com/graphql-kotlin/docs)
  instead; it generates the schema from Kotlin code rather than requiring SDL.

The `dgs*` modules demonstrate the DGS programming model and the
`spring-graphql*` modules demonstrate Spring GraphQL across every transport;
both run the same blog domain (posts, authors, comments) against the same
Postgres schema, so you can compare equivalent implementations directly.

## Summary

You have seen what GraphQL is and why you would choose it over REST, the schema
language (SDL) and the request/response envelope, the HTTP and WebSocket
transports, and the GraphQL Java execution engine. You have also met the two
Spring Boot frameworks — Netflix DGS and Spring GraphQL — how DGS adopted
Spring GraphQL as its foundation, and how to choose between them.

To go further, clone the
[spring-graphql-sample](https://github.com/hantsy/spring-graphql-sample)
repository and run one of the `dgs*` or `spring-graphql*` modules.
