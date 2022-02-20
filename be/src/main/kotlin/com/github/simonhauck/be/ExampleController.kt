package com.github.simonhauck.be

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
