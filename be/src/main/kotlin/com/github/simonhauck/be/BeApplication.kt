package com.github.simonhauck.be

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BeApplication

fun main(args: Array<String>) {
    runApplication<BeApplication>(*args)
}
