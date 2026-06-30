package io.realworld.app.web.rules

import io.ktor.server.engine.BaseApplicationEngine
import io.ktor.server.engine.ConnectorType
import io.realworld.app.config.SERVER_PORT
import io.realworld.app.config.setup
import io.realworld.app.web.util.HttpUtil
import org.junit.rules.ExternalResource
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * JUnit rule: starts a Ktor app before each test and stops it after.
 *
 * Dynamic port: the original rule reused port 8080 and a single app instance. When tests run
 * back-to-back, the previous server could still hold the port (BindException) or share DB state.
 * We pick a free port via [ServerSocket(0)] and start a new app per test so each test is isolated.
 *
 * Sleeps: Ktor 1.x [BaseApplicationEngine.start]/[stop] are not fully synchronous from the caller's
 * perspective. The brief pauses avoid flaky failures (requests before listen, or port not released).
 * Not ideal, but the pragmatic fix without a richer test lifecycle API.
 */
class AppRule : ExternalResource() {
    private lateinit var app: BaseApplicationEngine
    lateinit var http: HttpUtil
    val port: Int
        get() = app.environment.connectors.find { it.type == ConnectorType.HTTP }?.port ?: SERVER_PORT

    override fun before() {
        val freePort = ServerSocket(0).use { it.localPort }
        app = setup(port = freePort)
        app.start()
        TimeUnit.MILLISECONDS.sleep(500) // wait for connector to accept connections
        http = HttpUtil(port)
    }

    override fun after() {
        app.stop(500, 500)
        TimeUnit.MILLISECONDS.sleep(200) // allow socket + pool teardown before next test's port bind
    }
}
