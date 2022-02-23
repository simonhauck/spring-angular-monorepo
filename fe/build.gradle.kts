import com.github.gradle.node.npm.task.NpmInstallTask
import com.github.gradle.node.npm.task.NpmTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  java
  id("org.openapi.generator") version "5.4.0"

  id("com.github.node-gradle.node") version "3.2.0"
}

group = "com.github.simonhauck"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val angularBindingPath = "$buildDir/angular-binding/generated"
val beBuildDir = project(":be").buildDir
val openApiJsonFile = "$beBuildDir/docs/openapi.json"

openApiGenerate {
  generatorName.set("typescript-angular")
  inputSpec.set(openApiJsonFile)
  outputDir.set(angularBindingPath)

  configOptions.set(
    mapOf(
      "npmName" to "@mycompany/myproject-api",
      "snapshot" to "false",
      "npmVersion" to "1.0.0",
      "ngVersion" to "13.2.0",
    )
  )
}

val generateGoScan4EsrApi = tasks.withType<GenerateTask>() {
  dependsOn(":be:generateOpenApiDocs")
  doFirst {
    delete(angularBindingPath)
  }
  inputs.file(openApiJsonFile)
  outputs.dir(angularBindingPath)
}

repositories {
  mavenCentral()
}

tasks.withType<NpmInstallTask>() {
  dependsOn(tasks.getByName("openApiGenerate"))
}

val buildAngularTask = tasks.register<NpmTask>("buildAngular") {
  dependsOn(tasks.npmInstall)
  npmCommand.set(listOf("run", "build"))
  inputs.dir("src")
  inputs.dir(angularBindingPath)
  outputs.dir("${buildDir}/dist")
}

val angularBuildDir = "dist/fe"

java.sourceSets.create("angular") {
  resources.srcDir(angularBuildDir)
}

// Pack compiled angular in jar
tasks.withType<Jar> {
  dependsOn(buildAngularTask)
  from(angularBuildDir)
  into("static")
}

