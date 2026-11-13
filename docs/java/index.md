# GraphQL Java

[GraphQL Java](https://www.graphql-java.com/) is the JVM implementation of the
GraphQL specification, started in 2015. It is an execution engine, not a server
framework: it parses schemas, validates documents, and executes operations,
but it knows nothing about HTTP, WebSocket, or Spring. Every framework in this
repository — Spring GraphQL, Netflix DGS, and the legacy Kickstart and SPQR
examples — embeds it and delegates execution to it.

This page tours the core concepts. The code excerpts come from the
[legacy/graphql-java](https://github.com/hantsy/spring-graphql-sample/tree/master/legacy/graphql-java)
example, which wires the engine by hand; the framework examples do the same
wiring for you.

## Build the schema

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
custom scalars, directives, and type resolvers for interfaces and unions. The
legacy example builds it with a code registry that maps fields to fetchers:

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

## Fetch data

A `DataFetcher<T>` resolves one field. It receives a
`DataFetchingEnvironment` with the field arguments, the parent object
(`getSource()`), the execution context, and the data loader registry:

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
with a `title` accessor needs no wiring for its `title` field.

A fetcher can return a plain value, a `CompletionStage` for asynchronous
work, or a reactive `Publisher` for subscription fields.

## Execute operations

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
`ExecutionResult` maps directly to the response envelope described in
[Requests and responses](../intro/request-response.md).

The engine picks an execution strategy per operation type: query fields
resolve in parallel, mutation fields resolve serially in document order, and
subscription fields resolve to a stream that the transport layer forwards to
the client.

## Batch with DataLoader

Naive field resolution produces the N+1 problem: loading ten posts and then
each post's comments runs eleven queries. The
[java-dataloader](https://github.com/graphql-java/java-dataloader) library —
a separate project under the GraphQL Java organization — batches and caches
loads per request. The repository's
[DataLoaders notes](https://github.com/hantsy/spring-graphql-sample/blob/master/docs/adv.md)
show the effect on SQL. Before, one query per post:

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
and DGS both build on this same mechanism — see
[Spring GraphQL](../frameworks/spring-graphql.md) for its
`BatchLoaderRegistry` convenience.

## Extend execution

The engine exposes several extension points, all demonstrated in the
repository:

- **Instrumentation** — hooks around parsing, validation, and execution for
  metrics and tracing. GraphQL Java 22 reworked this API; older examples use
  the pre-22 form.
- **Error handling** — a `DataFetcherExceptionHandler` decides how a fetcher
  exception becomes a `GraphQLError` (see `CustomDataFetchingExceptionHandler`
  in the legacy example).
- **Custom scalars** — a `Coercing` implementation parses input and serializes
  output for scalars such as `LocalDateTime` and `UUID` (see `Scalars.java`).
- **Directives** — a `SchemaDirectiveWiring` wraps field fetchers to implement
  custom directives (see `UpperCaseDirectiveWiring`).

## Where to go next

The raw wiring above is what the frameworks automate. Read
[Integration history](./history.md) for how the JVM ecosystem arrived at
Spring GraphQL and DGS, then see
[Netflix DGS and Spring GraphQL](../frameworks/index.md) for the two
programming models.