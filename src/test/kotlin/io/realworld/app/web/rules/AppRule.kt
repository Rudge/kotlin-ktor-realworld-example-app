package io.realworld.app.web.rules

import io.ktor.server.engine.ConnectorType
import io.realworld.app.config.SERVER_PORT
import io.realworld.app.config.setup
import io.realworld.app.domain.repository.ArticleTags
import io.realworld.app.domain.repository.Articles
import io.realworld.app.domain.repository.Comments
import io.realworld.app.domain.repository.Favorites
import io.realworld.app.domain.repository.Follows
import io.realworld.app.domain.repository.Tags
import io.realworld.app.domain.repository.Users
import io.realworld.app.web.util.HttpUtil
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

class AppRule : ExternalResource() {
    private val app = setup()
    lateinit var http: HttpUtil
    val port = app.environment.connectors.find { it.type == ConnectorType.HTTP }?.port ?: SERVER_PORT

    override fun before() {
        app.start()
        TimeUnit.MILLISECONDS.sleep(500)
        cleanupDb()
        http = HttpUtil(port)
    }

    override fun after() {
        app.stop(500, 500, TimeUnit.MILLISECONDS)
    }

    private fun cleanupDb() {
        try {
            transaction {
                exec("DELETE FROM ${Comments.tableName}")
                exec("DELETE FROM ${Favorites.tableName}")
                exec("DELETE FROM ${ArticleTags.tableName}")
                exec("DELETE FROM ${Articles.tableName}")
                exec("DELETE FROM ${Follows.tableName}")
                exec("DELETE FROM ${Users.tableName}")
                exec("DELETE FROM ${Tags.tableName}")
            }
        } catch (e: Exception) {
            // Tables may not exist yet on very first run
        }
    }
}
