import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.5.4"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("plugin.spring") version "1.5.30"
	kotlin("jvm") version "1.5.30"
	id("com.netflix.dgs.codegen") version "5.0.6" //https://plugins.gradle.org/plugin/com.netflix.dgs.codegen
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

dependencies {
	implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:4.7.6"))
	implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter") {
		exclude("org.yaml", "snakeyaml")
	}
	implementation("com.netflix.graphql.dgs:graphql-dgs-subscriptions-websockets-autoconfigure") {
		exclude("org.yaml", "snakeyaml")
	}
	implementation("org.yaml:snakeyaml:1.29")

	//Spring and kotlin
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	// test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
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
