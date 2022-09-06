import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.3"
	id("io.spring.dependency-management") version "1.0.13.RELEASE"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

extra["testcontainersVersion"] = "1.17.3"
extra["graphqlKotlinVersion"] = "6.2.2"
extra["coroutinesVersion"] = "1.6.4"
extra["mockkVersion"] = "1.12.5"
extra["springmockkVersion"] = "3.1.1"
extra["ktestVersion"] = "5.4.2"

dependencies {
	// webflux
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Expediagroup GraphQL Kotlin
	implementation("com.expediagroup:graphql-kotlin-spring-server:${property("graphqlKotlinVersion")}")
	implementation("com.graphql-java:graphql-java:19.1")
	// r2dbc
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	runtimeOnly("org.postgresql:r2dbc-postgresql")

	// kotlin
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	// kotlin coroutines
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${property("coroutinesVersion")}")

	// test
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(module = "mockito-core")
	}
	testImplementation("io.projectreactor:reactor-test")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${property("coroutinesVersion")}")
	testImplementation("io.mockk:mockk:${property("mockkVersion")}")
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
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
