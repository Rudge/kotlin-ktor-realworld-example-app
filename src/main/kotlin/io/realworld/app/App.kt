package io.realworld.app

import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.realworld.app.config.mainModule

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::mainModule).start(wait = true)
}
