# Netflix DGS and Spring GraphQL

Two Spring Boot frameworks dominate GraphQL on the JVM today, and since 2024
they are no longer competitors at the engine level: the Netflix DGS framework
is built on Spring GraphQL. This section introduces both, explains the
integration, and compares them so you can choose.

- **Netflix DGS** (Domain Graph Service) — Netflix's GraphQL server framework
  for Spring Boot, open-sourced in 2021. An annotation-based programming model
  (`@DgsComponent`, `@DgsQuery`), schema code generation, and federation
  support.
- **Spring GraphQL** — the Spring team's official GraphQL support, built on
  GraphQL Java, 1.0 in May 2022. A Spring MVC-style controller model
  (`@QueryMapping`, `@SchemaMapping`), `GraphQlTester`, and HTTP, WebSocket,
  and RSocket transports.

Both execute operations with [GraphQL Java](../java/index.md). Since DGS 9,
DGS also uses Spring GraphQL's execution and transport infrastructure — the
choice between them is a choice of programming model and extras, not of
engine.

## In this section

1. [Netflix DGS](./dgs.md) — the DGS programming model, codegen, federation,
   and testing.
2. [Spring GraphQL](./spring-graphql.md) — the controller model, transports,
   testing, and clients.
3. [The DGS and Spring GraphQL integration](./integration.md) — why and how
   the two frameworks merged their foundations.
4. [DGS or Spring GraphQL?](./compare.md) — the differences that remain, and
   how to choose.