import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.spring") version "1.8.21"
    id("com.netflix.dgs.codegen") version "5.11.1" //https://plugins.gradle.org/plugin/com.netflix.dgs.codegen
}

// extra["graphql-java.version"] = "19.2"

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // dgs
    implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:7.1.0"))
    implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter")
    implementation("com.netflix.graphql.dgs:graphql-dgs-subscriptions-websockets-autoconfigure")
    implementation("com.netflix.graphql.dgs:graphql-dgs-extended-scalars")

    //jdbc
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.postgresql:postgresql")

    // spring boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // spring security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // spring data mongo
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // data redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")

    //kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    //test
    testImplementation("org.springframework:spring-websocket")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<GenerateJavaTask> {
    schemaPaths =
        mutableListOf("${projectDir}/src/main/resources/schema") // List of directories containing schema files
    packageName = "com.example.demo.gql" // The package name to use to generate sources
    generateClient = true // Enable generating the type safe query API
    shortProjectionNames = false
    maxProjectionDepth = 2
    snakeCaseConstantNames = true
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
