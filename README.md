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

TBD

## Example Codes
|  Example name       | Description     |
| ---- | ---- |
|[dgs](./dgs)  | Simple Netflix DGS example|
|[dgs-webflux](./dgs-webflux)| Simple Netflix DGS example with Spring WebFlux|
|[dgs-subscription-ws](./dgs-subscription-ws) | Simple Netflix DGS Subscription example using WebSocket protocol|
|[dgs-subscription-ui](./dgs-subscription-ui)  | Angular Client app for dgs-subscription-ws|
|[dgs-subscription-sse](./dgs-subscription-sse)  | Simple Netflix DGS Subscription example using Http/SSE protocol|
|[dgs-codegen](./dgs-codegen) | Netflix DGS example with Spring Jdbc and Gradle codegen plugin|
|[dgs-fileupload](./dgs-fileupload) | Netflix DGS file upload example|
|[dgs-client](./dgs-client) | Netflix DGS Typesafe Client example|
|[dgs-kotlin-co](./dgs-kotlin-co) | **A complete Netflix DGS example** with WebFlux, Kotlin Coroutines, Spring Data R2dbc and Spring Security|
|[dgs-kotlin](./dgs-kotlin) | **A complete Netflix DGS example** with WebMvc/Kotlin, Spring Data Jdbc, Spring Security and Spring Session/Spring Data Redis|
|[graphql-kotlin](./graphql-kotlin) |  ExpediaGroup Graphql Kotlin Spring Boot example|
|[graphql-java-kickstart](./graphql-java-kickstart)  | GraphQL Java Kickstart Spring Boot example|
|[graphql-java-kickstart-webclient](./graphql-java-kickstart-webclient) | GraphQL Java Kickstart Spring WebClient example|
|[graphql-java-kickstart-annotations](./graphql-java-kickstart-annotations) | GraphQL Java Kickstart Spring Boot example(Code first)|
|[spring-graphql](./spring-graphql) | Spring GraphQL example|
|[spring-graphql-webmvc](./spring-graphql-webmvc) | Spring GraphQL with WebMvc Controller annotation example|
|[spring-graphql-querydsl](./spring-graphql-querydsl)| Spring GraphQL/JPA/QueryDSl Data Fetchers example|
|[spring-graphql-webflux](./spring-graphql-webflux) | Spring GraphQL/WebFlux example with WebSocket transport protocol |
|[spring-graphql-rsocket-kotlin-co](./spring-graphql-rsocket-kotlin-co) | Spring GraphQL/WebFlux/Kotlin Coroutines example with RSocket transport protocol |


### Legacy Codes

Some example codes are moved to legacy folder, because the upstream project is discontinued or under an inactive development status.

|  Example name       | Description     |
| ---- | ---- |
|[graphql-java](./legacy/graphql-java) | GraphQL Java vanilla Spring Boot example, upstream project is discontinuned, replaced by Spring GraphQL|
|[graphql-spqr](./legacy/graphql-spqr)| GraphQL SPQR Spring example, inactive|

## Prerequisites

Make sure you have installed the following software.

* Java 21
* Apache Maven 3.8.x / Gradle 7.x
* Docker

Some sample codes are written in Kotlin. If you are new to Kotlin, start to learn it from the [the Kotlin homepage](https://kotlinlang.org/).

## Build 

Clone the source codes from Github.

```bash
git clone https://github.com/hantsy/spring-graphql-sample/
```

Open a terminal, and switch to the root folder of the project, and run the following command to build the whole project.

```bash
docker-compose up postgres // start up a postgres it is required
cd examplename // change to the example folder
mvn clean install // build the project
//or
./gradlew build
```

Run the application.

```bash
mvn spring-boot:run 
//or 
./gradlew bootRun
// or from command line after building
java -jar target/xxx.jar
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
