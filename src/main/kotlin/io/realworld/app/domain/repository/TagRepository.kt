package io.realworld.app.domain.repository

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal object Tags : LongIdTable() {
    val name: Column<String> = varchar("name", 100).uniqueIndex()
}

class TagRepository {
    init {
        transaction {
            SchemaUtils.create(Tags)
        }
    }

    fun findAll(): List<String> = transaction {
        Tags.selectAll().map { it[Tags.name] }
    }
}
