# spring-graphql-sample

Spring GraphQL examples using the following frameworks and libraries:

* [Netflix DGS(Domain Graph Service) framework](https://netflix.github.io/dgs/) 
* [Spring GraphQL](https://github.com/spring-projects/spring-graphql)
* [GraphQL Java Kickstart](https://www.graphql-java-kickstart.com/)
* [GraphQL Java](https://www.graphql-java.com/)
* [GraphQL SPQR(GraphQL Schema Publisher & Query Resolver, pronounced like speaker)](https://github.com/leangen/graphql-spqr)
* [ExpediaGroup GraphQL Kotlin](https://opensource.expediagroup.com/graphql-kotlin/docs)

Other GraphQL Java integration examples with Java frameworks.

* [GraphQL with Quarkus](https://github.com/hantsy/quarkus-sandbox)
* [GraphQL with Vertx](https://github.com/hantsy/vertx-sandbox)

## Guide

The [documentation site](https://hantsy.github.io/spring-graphql-sample/) covers
GraphQL fundamentals (the specification, schema, requests and responses, file
uploads, HTTP and WebSocket transports), GraphQL Java core concepts and
integration history, and the Netflix DGS / Spring GraphQL frameworks.

To preview the site locally, install MkDocs Material and start the development
server from the repository root:

```bash
pip install mkdocs-material mkdocs-git-revision-date-plugin
mkdocs serve
```

## Example Codes
|  Example name       | Description     |
| ---- | ---- |
|[dgs](./dgs)  | Simple Netflix DGS example|
|[dgs-webflux](./dgs-webflux)| Simple Netflix DGS example with Spring WebFlux|
|[dgs-subscription-ws](./dgs-subscription-ws) | Simple Netflix DGS Subscription example using WebSocket protocol|
|[dgs-subscription-ui](./dgs-subscription-ui)  | Angular client (traditional RxJS) for dgs-subscription-ws|
|[dgs-subscription-ui-signals](./dgs-subscription-ui-signals)  | Angular client (signals + rxResource API) for dgs-subscription-ws|
|[dgs-subscription-ui-sse](./dgs-subscription-ui-sse)  | Angular client (traditional RxJS) for dgs-subscription-sse, consuming subscriptions over Server-Sent Events|
|[dgs-subscription-ui-sse-signals](./dgs-subscription-ui-sse-signals)  | Angular client (signals + rxResource API) for dgs-subscription-sse, consuming subscriptions over Server-Sent Events|
|[dgs-subscription-sse](./dgs-subscription-sse)  | Simple Netflix DGS Subscription example using Http/SSE protocol|
|[dgs-codegen](./dgs-codegen) | Netflix DGS example with Spring Jdbc and the DGS codegen plugin|
|[dgs-fileupload](./dgs-fileupload) | Netflix DGS file upload example|
|[dgs-client](./dgs-client) | Netflix DGS Typesafe Client example|
|[dgs-kotlin-co](./dgs-kotlin-co) | **A complete Netflix DGS example** with WebFlux, Kotlin Coroutines, Spring Data R2dbc and Spring Security|
|[dgs-kotlin](./dgs-kotlin) | **A complete Netflix DGS example** with WebMvc/Kotlin, Spring Data Jdbc, Spring Security and Spring Session/Spring Data Redis|
|[graphql-kotlin](./graphql-kotlin) |  ExpediaGroup Graphql Kotlin Spring Boot example|
|[spring-graphql](./spring-graphql) | Spring GraphQL example|
|[spring-graphql-webmvc-http](./spring-graphql-webmvc-http) | Spring GraphQL/WebMvc over HTTP: queries and mutations on `/graphql`, subscriptions on the same endpoint via SSE|
|[spring-graphql-webmvc-http-ws](./spring-graphql-webmvc-http-ws) | Spring GraphQL/WebMvc: queries and mutations over HTTP, subscriptions over WebSocket (`graphql-transport-ws`)|
|[spring-graphql-webmvc-ws](./spring-graphql-webmvc-ws) | Spring GraphQL/WebMvc: queries, mutations and subscriptions all over WebSocket|
|[spring-graphql-webflux-http](./spring-graphql-webflux-http) | Spring GraphQL/WebFlux over HTTP with SSE subscriptions|
|[spring-graphql-webflux-http-ws](./spring-graphql-webflux-http-ws) | Spring GraphQL/WebFlux: queries and mutations over HTTP, subscriptions over WebSocket|
|[spring-graphql-webflux-ws](./spring-graphql-webflux-ws) | Spring GraphQL/WebFlux: every operation type over WebSocket|
|[spring-graphql-querydsl](./spring-graphql-querydsl)| Spring GraphQL/JPA/QueryDSl Data Fetchers example|
|[spring-graphql-rsocket-kotlin-co](./spring-graphql-rsocket-kotlin-co) | Spring GraphQL/WebFlux/Kotlin Coroutines example with RSocket transport protocol |

Each `spring-graphql-web*` module carries the same three test tiers, and the tier is what tells you how close to the wire the assertion is:

- `QueryTests`, `MutationTests`, `SubscriptionTests` - `@GraphQlTest` slices on a protocol-neutral `GraphQlTester` bound to the in-JVM execution service, with the service layer (`PostService`, `AuthorService`) mocked. They are identical within each stack - the three WebMvc modules share one version, the three WebFlux modules another - and prove nothing about transport.
- `IntegrationTests.WithGraphQlTesters` - the `GraphQlTester` transport API (`HttpGraphQlTester`, `WebSocketGraphQlTester`) against a running application and a Testcontainers Postgres. In the `-http` modules this tier covers queries and mutations only: `HttpGraphQlTester` rides `WebTestClient`, whose transport throws `Subscriptions not supported over HTTP`, so no tester can assert SSE.
- `IntegrationTests.WithGraphQlClients` - the runtime `GraphQlClient` API (`HttpSyncGraphQlClient`/`RestClient` in the servlet modules, `HttpGraphQlClient`/`WebClient` in the reactive ones, `WebSocketGraphQlClient` for WebSocket). In the `-http` modules this is the tier that proves SSE subscriptions, and everywhere it is the code a real client would write.

The Angular clients demonstrate two state management approaches:

- **Traditional** (`dgs-subscription-ui`, `dgs-subscription-ui-sse`): RxJS Observables with `async` pipe and Apollo's `subscribeToMore` for real-time updates.
- **Signals** (`dgs-subscription-ui-signals`, `dgs-subscription-ui-sse-signals`): `rxResource()` from `@angular/core/rxjs-interop` for queries and streaming subscriptions, with computed signals for derived state.

All clients default to `http://localhost:8080/graphql`. Pair the WebSocket clients with the `dgs-subscription-ws` backend and the SSE clients with `dgs-subscription-sse`; running the wrong backend, or both at once, means the UI silently talks to the wrong transport.

### Legacy Codes

Some example codes are moved to legacy folder, because the upstream project is discontinued or under an inactive development status.

These examples stay on the Spring Boot, Java and Gradle versions they were last verified with (Spring Boot 2.7.x - 3.2.x, Java 17/21), and are deliberately excluded from Dependabot updates.

|  Example name       | Description     |
| ---- | ---- |
|[graphql-java](./legacy/graphql-java) | GraphQL Java vanilla Spring Boot example, upstream project is discontinuned, replaced by Spring GraphQL|
|[graphql-spqr](./legacy/graphql-spqr)| GraphQL SPQR Spring example, inactive|
|[graphql-java-kickstart](./legacy/graphql-java-kickstart)  | GraphQL Java Kickstart Spring Boot example|
|[graphql-java-kickstart-webclient](./legacy/graphql-java-kickstart-webclient) | GraphQL Java Kickstart Spring WebClient example|
|[graphql-java-kickstart-annotations](./legacy/graphql-java-kickstart-annotations) | GraphQL Java Kickstart Spring Boot example(Code first)|

## Prerequisites

Make sure you have installed the following software.

* Java 25
* Docker - required by the shared Postgres container and by the Testcontainers based tests

Every example is a standalone Maven project and ships its own Maven Wrapper (`mvnw`), so installing Maven separately is optional. The `dgs-subscription-ui*` Angular clients are the exception - they are built with npm.

All active examples target Spring Boot 4.1.2 / Spring Framework 7 on Java 25. Some sample codes are written in Kotlin (Kotlin 2.4.x). If you are new to Kotlin, start to learn it from the [the Kotlin homepage](https://kotlinlang.org/).

## Build 

Clone the source codes from Github.

```bash
git clone https://github.com/hantsy/spring-graphql-sample/
```

Open a terminal, and switch to the root folder of the project. Start the shared Postgres instance first, because some examples connect to it instead of provisioning their own container.

```bash
docker compose up -d postgres
```

The six `spring-graphql-web*` modules are the exception - their tests provision Postgres with Testcontainers, so building them needs a running Docker daemon but not this shared instance; the other examples still connect to it.

Then switch to the example folder you want to build, and run the Maven Wrapper.

```bash
cd examplename // change to the example folder
./mvnw -B -ntp verify
```

To drive a single tier of a `spring-graphql-web*` module by hand, name the `IntegrationTests` nested class with `$`. In the `-http` modules the SSE subscription is only proven by the runtime-client tier, because `WebTestClient` cannot carry a subscription:

```bash
cd spring-graphql-webmvc-http
./mvnw -B -ntp test -Dtest='IntegrationTests$WithGraphQlClients'  # the tier that proves SSE
./mvnw -B -ntp test -Dtest='IntegrationTests$WithGraphQlTesters'  # queries and mutations over HTTP
```

Run the application.

```bash
./mvnw spring-boot:run
// or from command line after building
java -jar target/xxx.jar
```

GraphiQL is enabled by property only in the two `-http` modules; in the other four `spring.graphql.graphiql.enabled` is set to `false`.

The `dgs-subscription-ui*` Angular clients are built with npm instead.

```bash
cd dgs-subscription-ui
npm install
npm start
```


## Contribution

Any suggestions are welcome, filing an issue or submitting a PR is also highly recommended.  

## References

* [Getting started with GraphQL Java and Spring Boot](https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/)
* [Getting Started with GraphQL and Spring Boot](https://www.baeldung.com/spring-graphql)
* [Open Sourcing the Netflix Domain Graph Service Framework: GraphQL for Spring Boot](https://netflixtechblog.com/open-sourcing-the-netflix-domain-graph-service-framework-graphql-for-spring-boot-92b9dcecda18)
* [Netflix Open Sources Their Domain Graph Service Framework: GraphQL for Spring Boot ](https://www.infoq.com/news/2021/02/netflix-graphql-spring-boot/)
* [Netflix Embraces GraphQL Microservices for Rapid Application Development ](https://www.infoq.com/news/2021/03/netflix-graphql-microservices/)
* [GraphQL Reference Guide: Building Flexible and Understandable APIs ](https://www.infoq.com/articles/GraphQL-ultimate-guide/)
