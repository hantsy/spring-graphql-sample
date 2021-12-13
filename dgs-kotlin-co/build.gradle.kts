import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    kotlin("jvm") version "1.6.0"
    kotlin("plugin.spring") version "1.6.0"
	
    id("com.netflix.dgs.codegen") version "5.1.11" //https://plugins.gradle.org/plugin/com.netflix.dgs.codegen
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:4.9.11"))
    implementation("com.netflix.graphql.dgs:graphql-dgs-webflux-starter") {
        exclude("org.yaml", "snakeyaml")
    }
    implementation("org.yaml:snakeyaml:1.29")

    //Spring
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.r2dbc:r2dbc-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-security")

    //kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    //kotlin coroutines extensions
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
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
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

}
