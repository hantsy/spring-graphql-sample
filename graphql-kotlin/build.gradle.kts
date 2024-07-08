import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.spring") version "2.0.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["testcontainersVersion"] = "1.19.8"
extra["graphqlKotlinVersion"] = "8.0.0-alpha.1"
extra["coroutinesVersion"] = "1.8.1"
extra["mockkVersion"] = "1.13.11"
extra["springmockkVersion"] = "4.0.2"
extra["kotestVersion"] = "5.9.1"
extra["graphqlJavaVersion"] = "22.1"

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
    testImplementation("io.kotest:kotest-runner-junit5-jvm:${property("kotestVersion")}")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${property("kotestVersion")}")
    testImplementation("io.kotest:kotest-framework-concurrency:${property("kotestVersion")}")

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
