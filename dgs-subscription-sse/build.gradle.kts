import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    id("com.netflix.dgs.codegen") version "8.7.0" //https://plugins.gradle.org/plugin/com.netflix.dgs.codegen
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencyManagement {
    imports {
        mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:12.1.0")
    }
}

dependencies {
    // implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:8.1.1"))
    implementation("com.netflix.graphql.dgs:dgs-starter")
    implementation("org.yaml:snakeyaml")

    //Spring and kotlin
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("io.projectreactor:reactor-core")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    // test
    testImplementation("com.netflix.graphql.dgs:graphql-dgs-client")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.22.4") // DGS client still uses Jackson 2 internally
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.4")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.4")
    testImplementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.22.4")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.22.4")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-webflux") // provides WebClient
    testImplementation("io.projectreactor.netty:reactor-netty")
    testImplementation("com.netflix.graphql.dgs:dgs-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<GenerateJavaTask> {
    schemaPaths =
        mutableListOf("${projectDir}/src/main/resources/schema") // List of directories containing schema files
    packageName = "com.example.demo.gql" // The package name to use to generate sources
    generateClient = true // Enable generating the type safe query API
    shortProjectionNames = false
    snakeCaseConstantNames = true
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
