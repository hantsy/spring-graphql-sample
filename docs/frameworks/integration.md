# The DGS and Spring GraphQL integration

Since DGS 9.0 (May 2024), the Netflix DGS framework is built on Spring
GraphQL. This page explains why the two teams merged their foundations and
what the integration changes for your code. It follows Netflix's own
[Spring GraphQL Integration](https://netflix.github.io/dgs/spring-graphql-integration/)
document.

## Two competing frameworks

Netflix open-sourced DGS in 2021, and it became the most widely adopted
GraphQL Java framework. Soon after, the DGS team learned of a parallel effort
by the Spring team: Spring for GraphQL, then in its early stages as a
low-level integration with GraphQL Java. Over the following year Spring
GraphQL matured to rough feature parity with DGS.

The result was two frameworks solving the same problem. New users had to
choose between them and missed out on features available only in the other;
the maintainers duplicated work instead of collaborating; and the community's
feedback was split across two projects.

## Why not end-of-life one of them?

DGS is deeply embedded in the architecture of many companies, including
Netflix. Forcing those users onto Spring GraphQL would be a costly migration
with no real benefit. At the same time, Spring deserves an out-of-the-box
GraphQL offering, just as it has for REST. Neither framework could simply
disappear.

## The way forward

The teams chose integration over competition. DGS keeps its programming model
and tooling, but drops the parts that duplicated Spring GraphQL's low-level
WebMVC/WebFlux integration. The integration shipped first as an opt-in
`spring-graphql` flavor of the DGS starter, then became the default in DGS 9.0
(May 2024), which builds against Spring GraphQL 1.3, Spring Boot 3.3, and
GraphQL Java 22.

## Technical implementation

Both frameworks were designed for modularity, which made the integration
feasible. Before the integration, a request flowed through DGS's own
transport integration and its `DgsQueryExecutor`, which called GraphQL Java
directly. After the integration:

- You write data fetchers with the DGS programming model (`@DgsComponent`,
  `@DgsQuery`), the Spring GraphQL programming model (`@QueryMapping`), or
  both in the same application.
- DGS builds the `GraphQLSchema` from its programming model and schema files.
- Spring GraphQL handles the transports: WebMvc, WebFlux, and RSocket.
- Spring GraphQL's `ExecutionGraphQlService` executes every operation.
- `DgsQueryExecutor` becomes a proxy over `ExecutionGraphQlService`, so
  existing DGS test code continues to work unchanged.

One engine, one transport layer, two programming models.

## Performance

Netflix tested the integration on some of its largest services. The tests
uncovered a few performance issues in Spring WebMVC and Spring GraphQL; the
Spring team addressed them quickly, and the integrated stack now performs
better than the baseline of Netflix applications running the standalone DGS
framework.

## What changes for your code

The integration is mostly a drop-in replacement — no breaking changes in the
DGS programming model itself. The areas to review when upgrading are the ones
where the frameworks overlapped: configuration properties, file uploads,
WebSocket subscriptions, schema inspection, and test setup. Netflix's
[integration document](https://netflix.github.io/dgs/spring-graphql-integration/)
lists the notable changes and known gaps per area.

For a new application, the practical consequence is that the choice between
DGS and Spring GraphQL is no longer a choice of engine — see
[DGS or Spring GraphQL?](./compare.md).