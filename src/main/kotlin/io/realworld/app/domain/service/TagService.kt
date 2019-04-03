package io.realworld.app.domain.service

import io.realworld.app.domain.TagDTO
import io.realworld.app.domain.repository.TagRepository

class TagService(private val tagRepository: TagRepository) {
    fun findAll() = tagRepository.findAll().let { tags ->
        TagDTO(tags)
    }
}
