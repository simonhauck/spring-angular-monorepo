import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.google.cloud.tools.jib.gradle.JibTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"

    id("org.springdoc.openapi-gradle-plugin") version "1.3.3"
    id("com.github.johnrengelman.processes") version "0.5.0"
    id("com.google.cloud.tools.jib") version "3.2.0"

    id("com.github.johnrengelman.shadow") version "7.1.2"
    application // <- optional. Offers the runShadow gradle task and can create bash/batch scripts to start the jar
}

group = "com.github.simonhauck"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val feBuildConfiguration: Configuration by configurations.creating {} // <--- Create a new config

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    feBuildConfiguration(project(":fe")) // <---- this configuration can define dependencies

    // OpenApi / Swagger
    implementation("org.springdoc:springdoc-openapi-ui:1.6.6")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.6.5")
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

openApi {
    outputDir.set(file("$buildDir/docs"))
}

jib {
    to {
        val registry = System.getenv("REGISTRY_URL") ?: "local"
        image = "$registry/company/${project.name}"
        tags = setOf("${project.version}")
        auth {
            username = System.getenv("REGISTRY_USERNAME")
            password = System.getenv("REGISTRY_PASSWORD")
        }
    }
    container {
        ports = listOf("8080")
    }
}

// Add angular fe to be served from BE
val copyFeToBuildDirTask = tasks.register<ProcessResources>("copyFeToBuildDir") {
    dependsOn(feBuildConfiguration)
    val zipTree = zipTree(feBuildConfiguration.singleFile)
    from(zipTree)
    into("$buildDir/resources/main")
}

tasks.withType<JibTask> {
    dependsOn(copyFeToBuildDirTask)
}

tasks.withType<ShadowJar> {
    // Include spring stuff to get a fat jar
    mergeServiceFiles()
    append("META-INF/spring.handlers")
    append("META-INF/spring.schemas")
    append("META-INF/spring.tooling")
    transform(PropertiesFileTransformer::class.java) {
        paths = listOf("META-INF/spring.factories")
        mergeStrategy = "append"
    }
}

tasks.withType<ShadowJar> {
    dependsOn(copyFeToBuildDirTask)
}

application {
    mainClass.set("com.github.simonhauck.be.BeApplicationKt")
}
