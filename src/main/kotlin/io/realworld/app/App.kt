package io.realworld.app

import io.ktor.server.engine.EngineAPI
import io.ktor.util.KtorExperimentalAPI
import io.realworld.app.config.setup

@OptIn(EngineAPI::class, KtorExperimentalAPI::class)
fun main() {
    setup().start(wait = true)
}
