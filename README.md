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
project. Finally, open the ``settings.gradle`` of the parent project and include the backend project. It should look in
the end something like this

````shell
rootProject.name = "spring-angular-demo"
include("be")
````

Your repository should look something like this at the end.
[//] TODO Simon.Hauck 2022-02-20 insert image

Refresh your gradle project. If you use IntelliJ the gradle tab on the right should look something like this. Note: The
BE is part if the parent project. If the BE is still registered as a module of its own, right click it and unlink the BE

### Create the FE project


