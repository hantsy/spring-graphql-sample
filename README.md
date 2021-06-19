# spring-graphql-sample

Spring GraphQL examples using the following frameworks and libraries:

* [Netflix DGS(Domain Graph Service) framework](https://netflix.github.io/dgs/) 
* [Spring GraphQL](https://github.com/spring-projects/spring-graphql)
* [GraphQL Java](https://www.graphql-java.com/)

## Guide

TBD

## Example Codes
|  Example name    |Lang/Build tools      | Description     |
| ---- | ---- | ---- |
|[dgs](./dgs) | Java/Maven | Simple Netflix DGS example|
|[dgs-webflux](./dgs-webflux) | Java/Maven| Simple Netflix DGS example with Spring WebFlux|
|[dgs-subscription-ws](./dgs-subscription-ws) | Kotlin/Gradle | Simple Netflix DGS Subscription example using WebSocket protocol|
|[dgs-subscription-ui](./dgs-subscription-ui) | TypeScript/Angular CLI | Angular Client app for dgs-subscription-ws|
|[dgs-subscription-sse](./dgs-subscription-sse) | Kotlin/Gradle | Simple Netflix DGS Subscription example using Http/SSE protocol|
|[dgs-codegen](./dgs-codegen) | Java/Gradle | Netflix DGS example with Spring Jdbc and Gradle codegen plugin|
|[dgs-client](./dgs-client) | Java/Gradle | Netflix DGS Typesafe Client example|
|[dgs-kotlin](./dgs-kotlin) | Kotlin/Gradle | Netflix DGS example written in Kotlin, Spring Data Jdbc, Spring Security and Spring Session/Spring Data Redis|
|[graphql-java](./graphql-java) | Java/Gradle | GraphQL Java Spring Boot example|
|[graphql-java-webclient](./graphql-java-webclient) | Java/Gradle | GraphQL Java Spring WebClient example|
|[graphql-java-annotations](./graphql-java-annotations) | Java/Gradle | GraphQL Java Spring Boot example(Code first)|
|[spring-graphql](./spring-graphql) | Java/Gradle | Spring GraphQL example|

## Prerequisites

Make sure you have installed the following software.

* Java 16 and Kotlin 1.4
* Apache Maven 3.8.x and Gradle 7.x
* Docker

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
