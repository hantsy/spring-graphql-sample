import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.config.ApiVersion.Companion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("com.netflix.dgs.codegen") version "7.0.3" //https://plugins.gradle.org/plugin/com.netflix.dgs.codegen
}

// extra["graphql-java.version"] = "19.2"

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencyManagement {
    imports {
        mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:9.2.2")
    }
}

dependencies {
    // implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:8.1.1"))
    implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter") {
        exclude("org.yaml", "snakeyaml")
    }
    implementation("com.netflix.graphql.dgs:graphql-dgs-subscriptions-sse-autoconfigure") {
        exclude("org.yaml", "snakeyaml")
    }
    implementation("org.yaml:snakeyaml:2.3")

    //Spring and kotlin
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    // test
    testImplementation("com.netflix.graphql.dgs:graphql-dgs-client")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("io.projectreactor.netty:reactor-netty")
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

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        jvmTarget.set(JvmTarget.fromTarget("21"))
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
