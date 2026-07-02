package io.realworld.app.web.rules

import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.EngineAPI
import io.ktor.util.KtorExperimentalAPI
import io.realworld.app.config.SERVER_PORT
import io.realworld.app.config.setup
import io.realworld.app.web.util.HttpUtil
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

@OptIn(EngineAPI::class, KtorExperimentalAPI::class)
class AppRule : ExternalResource() {
    private val app = setup()
    lateinit var http: HttpUtil
    val port = app.environment.connectors.find { it.type == ConnectorType.HTTP }?.port ?: SERVER_PORT

    override fun before() {
        app.start()
        TimeUnit.MILLISECONDS.sleep(500)
        http = HttpUtil(port)
    }

    override fun after() {
        app.stop(500L, 500L)
    }
}
