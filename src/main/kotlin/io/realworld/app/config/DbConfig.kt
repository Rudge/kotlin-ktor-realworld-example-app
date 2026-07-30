package io.realworld.app.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.h2.tools.Server
import org.jetbrains.exposed.sql.Database

object DbConfig {
    private var pgServerStarted = false

    fun setup(jdbcUrl: String, username: String, password: String) {
        // The app is booted once per test class in the same JVM; starting the debug server
        // more than once would fail on the already-bound port.
        if (!pgServerStarted) {
            Server.createPgServer().start()
            pgServerStarted = true
        }
        val config = HikariConfig().also { config ->
            config.jdbcUrl = jdbcUrl
            config.username = username
            config.password = password
        }
        Database.connect(HikariDataSource(config))
    }
}
