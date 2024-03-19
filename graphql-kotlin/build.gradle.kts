import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["testcontainersVersion"] = "1.19.1"
extra["graphqlKotlinVersion"] = "7.0.2"
extra["coroutinesVersion"] = "1.7.1"
extra["mockkVersion"] = "1.13.5"
extra["springmockkVersion"] = "4.0.2"
extra["ktestVersion"] = "5.6.2"
extra["graphqlJavaVersion"] = "20.2"

dependencies {
    // webflux
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Expediagroup GraphQL Kotlin
    implementation("com.expediagroup:graphql-kotlin-spring-server:${property("graphqlKotlinVersion")}")
    implementation("com.graphql-java:graphql-java:${property("graphqlJavaVersion")}")
    // r2dbc
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // kotlin coroutines
    developmentOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${property("coroutinesVersion")}")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("io.projectreactor:reactor-test")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:${property("coroutinesVersion")}")
    testImplementation("io.mockk:mockk-jvm:${property("mockkVersion")}")
    testImplementation("com.ninja-squad:springmockk:${property("springmockkVersion")}")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:${property("ktestVersion")}")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${property("ktestVersion")}")
    testImplementation("io.kotest:kotest-framework-concurrency:${property("ktestVersion")}")

    // testcontainters
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:r2dbc")
    runtimeOnly("org.postgresql:postgresql")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
