import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.config.ApiVersion.Companion.KOTLIN_2_1
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    // webflux
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Expediagroup GraphQL Kotlin
    implementation("com.expediagroup:graphql-kotlin-spring-server:11.0.0")
    // implementation("com.graphql-java:graphql-java:22.1")
    // r2dbc
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // kotlin
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // kotlin coroutines
    developmentOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-webclient-test") // WebClient auto-configuration lives in its own module since Boot 4
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
    }
    testImplementation("io.projectreactor:reactor-test")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm")
    testImplementation("io.mockk:mockk-jvm:1.14.11")
    testImplementation("com.ninja-squad:springmockk:5.0.1")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:6.2.6")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.2.6")

    // testcontainters
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    testImplementation("org.testcontainers:testcontainers-r2dbc:2.0.5")
    runtimeOnly("org.postgresql:postgresql")
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
        jvmTarget.set(JvmTarget.fromTarget("25"))
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    jvmArgs("-Djdk.attach.allowAttachSelf=true")
}
