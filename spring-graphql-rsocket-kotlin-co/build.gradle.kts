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

extra["coroutinesVersion"]="1.10.2"
extra["mockkVersion"]="1.14.11"
extra["springmockkVersion"]="5.0.1"
extra["kotestVersion"]="6.2.6"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-rsocket")

	//r2dbc
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.postgresql:r2dbc-postgresql")

	//kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("tools.jackson.module:jackson-module-kotlin")

	//kotlin coroutines
	developmentOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${property("coroutinesVersion")}")

	//test
	testImplementation("org.springframework.boot:spring-boot-rsocket-test")
	testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testRuntimeOnly("org.postgresql:postgresql") // JDBC driver for Testcontainers init script
testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
	testImplementation("org.testcontainers:testcontainers-r2dbc:2.0.5")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
	}
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:${property("coroutinesVersion")}")
	testImplementation("io.mockk:mockk-jvm:${property("mockkVersion")}")
	testImplementation("com.ninja-squad:springmockk:${property("springmockkVersion")}"){
		exclude(module = "mockk")
	}

	testImplementation("io.kotest:kotest-runner-junit5-jvm:${property("kotestVersion")}")
	testImplementation("io.kotest:kotest-assertions-core-jvm:${property("kotestVersion")}")
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
