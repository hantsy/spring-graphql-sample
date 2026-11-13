# DGS or Spring GraphQL?

Since the [integration](./integration.md), DGS and Spring GraphQL share the
same foundation: both run on Spring Boot, both execute operations with
[GraphQL Java](../java/index.md), and DGS additionally uses Spring GraphQL's
execution service and transports. The differences that remain are the
programming model and the tooling around it.

## Differences

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

Both frameworks support Kotlin and coroutines, Spring Security, file uploads
through `MultipartFile`, and DataLoader-based batching. The
[dgs-kotlin-co](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-kotlin-co)
and
[spring-graphql-rsocket-kotlin-co](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-rsocket-kotlin-co)
examples show the two styles side by side.

## Choose

- **Starting a new Spring Boot application** — choose Spring GraphQL. It is
  the official Spring baseline, its versions move with Spring Boot, and its
  controller model matches the rest of a Spring application.
- **Running DGS today** — stay on DGS and keep upgrading. Since DGS 9 you are
  already on Spring GraphQL underneath; a rewrite buys nothing. You can mix
  Spring GraphQL APIs into a DGS application where they help.
- **Needing codegen or federation** — DGS's generated types and built-in
  federation support are the strongest reasons to pick it for a new project.
- **Preferring code-first Kotlin** — look at
  [GraphQL Kotlin](https://opensource.expediagroup.com/graphql-kotlin/docs)
  instead; it generates the schema from Kotlin code rather than requiring SDL.

## In this repository

The `dgs*` modules demonstrate the DGS programming model; the
`spring-graphql*` modules demonstrate Spring GraphQL across every transport.
Both sets run the same blog domain (posts, authors, comments) against the same
Postgres schema, so you can compare equivalent implementations directly —
for example
[dgs](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs)
versus
[spring-graphql](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql),
or
[dgs-subscription-ws](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ws)
versus
[spring-graphql-webmvc-ws](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-webmvc-ws).