package com.github.simonhauck.be.config

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class RouteToAngular : ErrorController {

    @RequestMapping("/error")
    fun handleError(): String {
        return "/"
    }

    val errorPath: String
        get() = "/error"
}