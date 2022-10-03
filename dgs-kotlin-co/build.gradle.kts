import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"

    kotlin("jvm") version "1.7.10"
    kotlin("plugin.spring") version "1.7.20"

    id("com.netflix.dgs.codegen") version "5.3.1" //https://plugins.gradle.org/plugin/com.netflix.dgs.codegen
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:5.2.4"))
    implementation("com.netflix.graphql.dgs:graphql-dgs-webflux-starter") {
        exclude("org.yaml", "snakeyaml")
    }
    implementation("org.yaml:snakeyaml:1.33")

    //Spring
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-security")

    //kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    //kotlin coroutines extensions
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("io.mockk:mockk-jvm:1.13.2")
    testImplementation("com.ninja-squad:springmockk:3.1.1"){
        exclude(module = "mockk")
    }
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.5.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.4.2")
    testImplementation("io.kotest:kotest-framework-concurrency:5.4.2")
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
        freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
