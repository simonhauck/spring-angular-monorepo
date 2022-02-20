# Gradle Spring Angular Mono-Repo

This is an example project, to showcase how a gradle multi-module projects can be used to have a single code base for
spring-angular projects.

## Technology stack

This project utilizes the following tools and frameworks

- Spring as server/backend-framework
- Angular as frontend-framework
- (Kotlin) Gradle as build tool for spring
- Node as Fe build tool
- Jib as Docker build tool

## Desired Workflow

This project is designed to fill the following requirements:

- Everything is packaged together as a single docker container with a single gradle command (`gradlew jibDocker`)
- The BE generates an OpenApi specification
- The OpenApi Specification is used to generate an angular client
- The angular application consumes this client to make request to the BE
- The angular application is served via the BE (this relates to the first point)

## Setup of this project

### Setup empty multi module project

Use your favorite IDE for that. I personally recommend IntelliJ. The project should contain at this point

- the gradle wrapper
- build.gradle.kts
- gradlew
- gradlew.bat for windows
- settings.gradle.kts

### Create the BE project

Create your spring project with the spring initializer. I used again IntelliJ for that, but you can also use
the [Spring Initializer](https://start.spring.io/).

Delete the gradle wrapper, gradlew and gradlew.bat files from the spring project. We only need them in the top level
project. Finally, open the ``settings.gradle.kts`` of the parent project and include the backend project. It should look
in the end something like this

````shell
rootProject.name = "spring-angular-demo"
include("be")
````

Your repository should look something like this at the end.
[//] TODO Simon.Hauck 2022-02-20 insert image

Refresh your gradle project. If you use IntelliJ the gradle tab on the right should look something like this. Note: The
BE is part if the parent project. If the BE is still registered as a module of its own, right click it and unlink the BE

[//]: ( TODO Simon.Hauck 2022-02-20 Add image

### Create the FE project

Now comes the angular project. Again use your favorite tool to create an angular project. My is named ``fe`` and it must
be placed in a folder like the backend project. Your project should look something like this now.

[//]: ( TODO Simon.Hauck 2022-02-20 Add image

Create two new files named ``settings.gradle.kts`` and ``build.gradle.kts`` in the fe directory with the following
content

````kotlin
# build.gradle.kts
plugins {
    java
}

group = "com.github.simonhauck"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}
````

````shell
# settings.gradle.kts
rootProject.name = "fe"
````

Finally, add io the root ``settings.gradle.kts`` the line ``include("fe")``. It should look now like this

````shell
rootProject.name = "spring-angular-demo"
include("be")
include("fe")
````

### Generate OpenApi Json from BE code

It would be easier to set this project up with an api-first approach. There you would have the openApi json first and
would create client and server stubs directly.

In this project we want to use the code-first approach and document our api with
the [springdoc-openApi](https://springdoc.org/) project. This requires a bit more setup because we want to

````shell
build be --> generate api json --> build fe client --> build fe --> include fe in be build
````

But since the backend is already built it is a bit more complicated to include. I will demonstrate my proposed solution
in the next steps.

Let's start by adding the following dependencies in our be project. Since this is a kotlin project, i will use the
Kotlin dependencies additionally but for java it should be quite similar.

```` kotlin
// in be module : build.gradle.kts

dependencies {
    // other dependencies

    // OpenApi / Swagger
    implementation("org.springdoc:springdoc-openapi-ui:1.6.6")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.6.5")
}
````

Now create a RestController and a response object. Below is my example

````kotlin
@RestController
@RequestMapping("example")
class ExampleController {

    @GetMapping("hello")
    fun getHelloWorld(): HelloWorldDto {
        return HelloWorldDto("Word: ${System.currentTimeMillis()}")
    }
}

data class HelloWorldDto(
    val response: String,
)
````

Start the backend project, and you should see

- on http://localhost:8080/swagger-ui/index.html the swagger ui
- on http://localhost:8080/v3/api-docs the open api json

Now lets start generating the openApi json as part of our build. We will need
the [springdoc-openapi-gradle-plugin](https://github.com/springdoc/springdoc-openapi-gradle-plugin) plugin. Add it as
dependency in the backend module in the ``build.gradle.kts``

````kotlin
// be build.gradle.kts

plugins {
    // other plugins

    id("org.springdoc.openapi-gradle-plugin") version "1.3.3"
    id("com.github.johnrengelman.processes") version "0.5.0" // This one is also required for me to compile the be. If it works without it, remoe it
}

// Configure the path of the json
openApi {
    outputDir.set(file("$buildDir/docs"))
}
````

Now you can generate the openApi json with the command ``gradlew generateOpenApiDocs``

### Generate the frontend api client

