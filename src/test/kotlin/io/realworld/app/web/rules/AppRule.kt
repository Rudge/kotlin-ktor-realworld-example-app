package io.realworld.app.web.rules

import io.ktor.server.engine.ConnectorType
import io.realworld.app.config.SERVER_PORT
import io.realworld.app.config.setup
import io.realworld.app.web.util.HttpUtil
import org.junit.rules.ExternalResource
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

class AppRule : ExternalResource() {
    private val app = setup()
    lateinit var http: HttpUtil
    val port = app.environment.connectors.find { it.type == ConnectorType.HTTP }?.port ?: SERVER_PORT

    override fun before() {
        requirePortIsFree()
        app.start()
        TimeUnit.MILLISECONDS.sleep(500)
        http = HttpUtil(port)
    }

    override fun after() {
        app.stop(500, 500, TimeUnit.MILLISECONDS)
    }

    /**
     * Ktor logs the bind failure on a worker thread and carries on, so an already-running server
     * (a stray `./gradlew run`, say) would quietly serve the tests its own data and surface as
     * baffling assertion failures. Fail here instead, with the reason.
     */
    private fun requirePortIsFree() {
        try {
            ServerSocket(port).close()
        } catch (e: Exception) {
            throw IllegalStateException(
                "Port $port is already in use — stop the process bound to it before running the tests.", e
            )
        }
    }
}
