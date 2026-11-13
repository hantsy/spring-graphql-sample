# Integration history

GraphQL on the JVM has gone through several generations of integration
libraries. This page traces that history — it explains why this repository
keeps a frozen `legacy/` folder, and why the two actively developed frameworks
today, Spring GraphQL and Netflix DGS, share the same engine. The framework
links come from the
[README](https://github.com/hantsy/spring-graphql-sample#readme).

## 2015: GraphQL Java

Facebook open-sourced the GraphQL specification in 2015, and
[GraphQL Java](https://github.com/graphql-java/graphql-java) started the same
year as the JVM implementation. It provided schema parsing, validation, and
execution — but no way to serve requests. Integrating it into an application
meant writing your own endpoint.

## 2016: graphql-java-servlet

[graphql-java-servlet](https://github.com/graphql-java-kickstart/graphql-java-servlet)
(2016) filled the gap with a servlet endpoint: it accepted GraphQL requests
over HTTP and handed them to a `GraphQL` instance. It also carried an early
WebSocket subscription implementation from the
`subscriptions-transport-ws` era (see
[Subscriptions over WebSocket](../intro/websocket.md)).

## The Kickstart era

The [GraphQL Java Kickstart](https://www.graphql-java-kickstart.com/) project
built the most popular Spring Boot integration of that period on two pieces:

- [GraphQL Java Tools](https://github.com/graphql-java-kickstart/graphql-java-tools)
  — a schema-first tool inspired by JavaScript's graphql-tools: you write SDL
  and implement resolver interfaces, and the library wires them into a
  `RuntimeWiring`.
- [GraphQL Spring Boot Starters](https://github.com/graphql-java-kickstart/graphql-spring-boot)
  — auto-configuration for the servlet, GraphiQL, and subscriptions.

This repository's `legacy/graphql-java-kickstart*` examples use that stack.
The starters are now archived; the repository's notice reads:

> Since Spring now officially supports GraphQL we have decided to archive
> this project. We encourage you to start using Spring for GraphQL instead.

## The code-first alternative: SPQR

[GraphQL SPQR](https://github.com/leangen/graphql-spqr) (Schema Publisher &
Query Resolver, pronounced "speaker") took the opposite approach: annotate
plain Java methods and let the library generate the schema from the code.
Development is no longer active; the `legacy/graphql-spqr` example is frozen.

## 2021: Netflix DGS

Netflix open-sourced the
[DGS (Domain Graph Service) framework](https://netflix.github.io/dgs/) in
February 2021, after years of internal use. It offered an annotation-based
Spring Boot programming model, schema code generation, federation support,
and a query-test framework. See the announcement posts:
[Open Sourcing the Netflix Domain Graph Service Framework](https://netflixtechblog.com/open-sourcing-the-netflix-domain-graph-service-framework-graphql-for-spring-boot-92b9dcecda18)
and InfoQ's coverage of
[the release](https://www.infoq.com/news/2021/02/netflix-graphql-spring-boot/)
and
[Netflix's GraphQL microservices architecture](https://www.infoq.com/news/2021/03/netflix-graphql-microservices/).

## 2021–2022: Spring GraphQL

Soon after DGS appeared, the Spring team started a parallel effort:
[Spring GraphQL](https://github.com/spring-projects/spring-graphql), built
directly on GraphQL Java. Version 1.0 arrived in May 2022, and Spring Boot 2.7
shipped `spring-boot-starter-graphql` — GraphQL became an official part of the
Spring portfolio, with first-class WebMvc, WebFlux, WebSocket, and RSocket
transports and the `GraphQlTester` testing API.

## 2023–2024: convergence

Two frameworks solving the same problem split the community. Instead of
end-of-lifing either one, the teams integrated them: DGS adopted Spring
GraphQL as its execution foundation, first as an opt-in starter flavor, then
by default in DGS 9.0 (May 2024, on Spring GraphQL 1.3). The
[DGS and Spring GraphQL integration](../frameworks/integration.md) page tells
that story in detail.

## The ecosystem today

| Project | Approach | Status |
| ------- | -------- | ------ |
| [GraphQL Java](https://www.graphql-java.com/) | Execution engine | Active; the foundation of everything below. |
| [Spring GraphQL](https://github.com/spring-projects/spring-graphql) | Official Spring integration, schema-first controllers | Active; the Spring baseline. |
| [Netflix DGS](https://netflix.github.io/dgs/) | Annotation model, codegen, federation, on Spring GraphQL | Active. |
| [GraphQL Kotlin](https://opensource.expediagroup.com/graphql-kotlin/docs) | Code-first Kotlin, coroutines (ExpediaGroup) | Active. |
| [GraphQL Java Kickstart](https://www.graphql-java-kickstart.com/) | Schema-first tools + Spring Boot starters | Starters archived; use Spring GraphQL. |
| [GraphQL SPQR](https://github.com/leangen/graphql-spqr) | Code-first schema generation | Inactive. |

Other JVM frameworks integrate GraphQL Java outside Spring as well — see
[GraphQL with Quarkus](https://github.com/hantsy/quarkus-sandbox) and
[GraphQL with Vert.x](https://github.com/hantsy/vertx-sandbox).

The `legacy/` folder in this repository preserves the discontinued stacks
(Kickstart, SPQR, vanilla GraphQL Java) on the Spring Boot and Java versions
they were last verified with, excluded from dependency updates. The active
modules track current Spring Boot and either Spring GraphQL or DGS.

## References

- [Getting started with GraphQL Java and Spring Boot](https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/) — the original GraphQL Java tutorial.
- [Getting Started with GraphQL and Spring Boot](https://www.baeldung.com/spring-graphql) — Baeldung's Spring GraphQL introduction.
- [GraphQL Reference Guide: Building Flexible and Understandable APIs](https://www.infoq.com/articles/GraphQL-ultimate-guide/) — InfoQ's GraphQL overview.