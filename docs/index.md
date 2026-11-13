# Preface

This guide helps Java developers familiar with Spring understand GraphQL and
build GraphQL APIs with Spring GraphQL and the Netflix DGS framework. It starts
with the GraphQL specification and its transport protocols, then covers
GraphQL Java — the execution engine that every JVM framework builds on — and
finishes with the two Spring-based frameworks, their history, and their
relationship.

The examples use Java 25 and Spring Boot 4.1.2 / Spring Framework 7. Some
examples are written in Kotlin (Kotlin 2.4.x). This guide is not a Spring
tutorial; it assumes you can already build a Spring Boot application.

## Get the examples

The [spring-graphql-sample repository](https://github.com/hantsy/spring-graphql-sample)
contains the example projects. Clone it with Git:

```bash
git clone https://github.com/hantsy/spring-graphql-sample.git
```

Each example is a standalone Maven project with its own Maven Wrapper. See the
[README](https://github.com/hantsy/spring-graphql-sample#readme) for the full
example index and build instructions.

## Read this guide

Read the sections in order if you are new to GraphQL on the JVM:

1. [Introduction to GraphQL](./intro/index.md) — the specification, the schema
   language, and the HTTP and WebSocket transports.
2. [GraphQL Java](./java/index.md) — the core concepts of the execution engine,
   and the [integration history](./java/history.md) of GraphQL in the Java
   ecosystem.
3. [Netflix DGS and Spring GraphQL](./frameworks/index.md) — the two frameworks,
   their [integration](./frameworks/integration.md), and
   [how to choose](./frameworks/compare.md).

## Contribute

Report an issue or propose a change through a pull request in the
[GitHub repository](https://github.com/hantsy/spring-graphql-sample).

## About the author

Hantsy is an independent software engineer and consultant with more than 20
years of experience. For consulting inquiries, contact
[hantsy@gmail.com](mailto:hantsy@gmail.com). You can also
[buy Hantsy a coffee](https://www.buymeacoffee.com/hantsy).