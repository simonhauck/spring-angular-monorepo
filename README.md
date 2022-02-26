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
// build.gradle.kts
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

### Generate the fe open api client

The client will be generated with
the [gradle open api generator](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin)
. In the fe module add the following plugin and the configuration. This will generate and angular client with the
specified version. It will use the openapi.json from the backend and place the generated code in the build directory of
the frontend. Of course change this stuff to match your personal needs ;)

````kotlin
// fe build.gradle.kts
plugins {
    // Other plugins
    id("org.openapi.generator") version "5.4.0"
}

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
````

To have a fully working task we have to configure it correctly in the frontend ``build.gradle.kts``. The task should
delete the old api binding when being executed (that there are no leftovers), depend on the openapi.json. To achive
this, add the following configuration.

````kotlin
// fe build.gradle.kts
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

val generateGoScan4EsrApi = tasks.withType<GenerateTask>() {
    dependsOn(":be:generateOpenApiDocs")
    doFirst {
        delete(angularBindingPath)
    }
    inputs.file(openApiJsonFile)
    outputs.dir(angularBindingPath)
}
````

Now you should be able to generate the fe task with ``gradlew :fe:openApiGenerate``. Note: its importent to specify
the ``:fe`` else it could lead to compile errors.

If everything worked, you should have a typescript client in your ``fe/build/angular-binding/generated`` directory.

### Build the fe

Before we use the api client, lets build the fe with gradle. For local development, It's still possible to
use ``ng serve`` or any other command, but for production we will build the frontend, pack the generated sources in a
jar, which will be included and served in the backend.

The angular project will be build with the [gradle node plugin](https://github.com/node-gradle/gradle-node-plugin). This
gives us the option to run different npm/yarn tasks.

Add the plugin in the fe module and add the following configuration. This will download a node and place in the .cache
folder of the module. This node will be used for all our other tasks.

````kotlin
// fe build.gradle.kts
plugins {
    // --- Other plugins 
    id("com.github.node-gradle.node") version "3.2.0"
}

node {
    version.set("16.10.0")
    download.set(true)
    workDir.set(file("${project.projectDir}/.cache/nodejs"))
}
````

After that, we can create an ``buildAngularTask`` with this configuration in our frontend ``build.gradle.kts``. To get
proper gradle layer caching we define the src directory as input as well as the generatedApiCode. So if the backend api
changes, the frontend is also rebuild. Also we want the npm install task to wait for generatedApiCode. This will be
important later. But the essence is, our npm install will fail when our dependency is not here ;)

````kotlin
// fe build.gradle.kts

import com.github.gradle.node.npm.task.NpmInstallTask
import com.github.gradle.node.npm.task.NpmTask

val buildAngularTask = tasks.register<NpmTask>("buildAngular") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "build"))
    inputs.dir("src")
    inputs.dir(angularBindingPath)
    outputs.dir("${buildDir}/dist")
}

tasks.withType<NpmInstallTask>() {
    dependsOn(tasks.getByName("openApiGenerate"))
}
````

Now comes already the last step for the frontend. We will pack the generated sources as jar so that we can easily
include them in the backend. Add the following configuration. We define the folder with the generated resources as
sourceSet so that it is picked up by the jar task.

The jar task depends on the angular build and copies the output from this directory of the static folder of the jar.
Spring is so smart, that we can serve static resources later automatically.

````kotlin
// fe build.gradle.kts

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
````

### Use the api client in the frontend

Now comes the first tricky part. The generated typescript code is not compiled and (atleast for me) this leads to
compile errors. So in theory, you can locally compile and pack a npm package. This works in theory, but node caching
lead to some problems for me. The package was "updated" when the api changed, but some export statements where not
available at runtime.

One possible solution can be, to compile, pack and publish the dependency to a registry. But in that case, we would lose
our advantage to build everything locally with one command. So I decided to compile the library with the other angular
code. This even saves some build time ;)

So lets get started. First add the dependency in your ``package.json``

````shell
{
  // other package.json stuff
  "dependencies": {
    // other libs
    "@mycompany/myproject-api": "file:build/angular-binding/generated"
  }
}
````

To compile the library with your project open the ``tsconfig.json`` and add teh following ``path`` section under the
compile options.

````shell
To learn more about this file see: https://angular.io/config/tsconfig. */
{
  "compileOnSave": false,
  "compilerOptions": {
    // Other stuff
    "paths": {
      // This issue is the reason why we have to do this https://github.com/angular/angular/issues/25813
      "@angular/*": ["./node_modules/@angular/*"],
      "@mycompany/myproject-api": ["./build/angular-binding/generated"]
    }
  },
}
````

Run a npm install and it should already work. You can test it by creating a simple service that calls our api. To
prevent CORS issues, add this configuration to your BE (TODO Add Config).

Create a simple angular service and make sure the constructor is actually called. In the appModule ``app.module.ts`` you
have to also add the HttpClientModule to make those reqeuest.

````typescript
// Example service
import { Injectable }               from '@angular/core';
import { ExampleControllerService } from "@mycompany/myproject-api";

@Injectable({
    providedIn: 'root'
})
export class ExampleService {

    constructor(private api: ExampleControllerService) {
        console.log("Test")
        api.getHelloWorld().subscribe(value => console.log("Received: " + value.response));
    }
}
````

````typescript
// App module with HttpClient

@NgModule({
    declarations: [
        AppComponent
    ],
    imports: [
        BrowserModule,
        HttpClientModule, //<---- This is important
    ],
    providers: [ExampleService],
    bootstrap: [AppComponent]
})
export class AppModule {
}

````

Start the BE and fe and give it a quick test :) I received only a ``blob`` as response which is quite strange. In theory
this should not matter, but if you specify in the backend that the default return type is json, everything works. Modify
your application.yml / application.property file accordingly. I hope this will be fixed in the future. If you find a
solution, please open a pull request.

````yaml
springdoc:
  default-produces-media-type: application/json
````

With all those obstacles tackled, i and hopefully you too, have a proper api response...hurrayyy :D

Note: For better code completion sense you can (at least in IntelliJ) mark the ``angular-binding`` folder as
generatedSources by right click > mark directory as > generated sources.

### Include frontend in docker

Now to the grand finale. Lets bundle it all together and deploy it as docker container. I will
use [Jib](https://cloud.google.com/blog/products/application-development/introducing-jib-build-java-docker-images-better)
for that. Add the gradle plugin and configure your container. The registry credentials are of course optional and can be
used with a CI for example.

````kotlin
// In be build.gradle.kts

plugins {
    // Other plugins ...
    id("com.google.cloud.tools.jib") version "3.2.0"
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
````

Last, we have to add the FE project as dependency and make jib depend on it. As stated at the beginning, we can't just
include the frontend as a normal implementation dependency because we would have gradle build cycles. So the fe will not
be available with the normal bootJar task! (Atle ast for this does not matter. For development i can start the FE
manually and for production, in jib, its packed correctly.)

Open your build.gradle.kts in the backend porject and create a new configuration like the following snippet below.

````kotlin
val feBuildConfiguration: Configuration by configurations.creating {} // <--- Create a new config

dependencies {
    // ... Other dependencies

    // Web frontend will be wired after compilation
    feBuildConfiguration(project(":fe")) // <---- this configuration can define dependencies
}
````

Now we can define a new task, that depends on the ``feBuildConfiguration`` and copies the static files in our build
directory. Additionally, we configure our jib task to depend on the ``copyFeToBuildDirTask`` task.

````kotlin
// be build.gradle.kts
import com.google.cloud.tools.jib.gradle.JibTask

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
````

Build the container with ``gradlew be:jibDockerBuild``. Afterwards you should be able to start it
with ``docker run -p 8080:8080 local/company/be:0.0.1-SNAPSHOT``

If you have come so far, you should see we are nearly done. The frontend is served via the backend. THe api generation
works (nearly perfect). For other host you should overwrite the api base-path.

### Override base api path

Now let's override the base api path in production, so that the projects works regardless of the host and port. In the
angular ``environmen.ts`` introduce a new ``serverBase`` value and fill it with the server url. This will probably
be ``http://localhost:8080``.

````typescript
export const environment = {
    production: false,
    serverBase: 'http://localhost:8080',
};
````

In the production config, we can override this value to use the host url, like shown in the following snippet.

````typescript
// environment.prod.ts
export const environment = {
    production: true,
    serverBase: window.location.origin,
};
````

In your ``app.module.ts`` add a new ``BASE_PATH`` provider like in the following example

````typescript
import { BASE_PATH }   from "@mycompany/myproject-api";
import { environment } from "../environments/environment";

@NgModule({
    declarations: [
        AppComponent,
    ],
    // ... other imports ...
    providers: [{provide: BASE_PATH, useValue: environment.serverBase}],
})
export class AppModule {
}
````



