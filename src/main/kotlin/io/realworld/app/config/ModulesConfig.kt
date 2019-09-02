package io.realworld.app.config

import io.realworld.app.domain.repository.TagRepository
import io.realworld.app.domain.repository.UserRepository
import io.realworld.app.domain.service.TagService
import io.realworld.app.domain.service.UserService
import io.realworld.app.utils.JwtProvider
import io.realworld.app.web.controllers.ArticleController
import io.realworld.app.web.controllers.CommentController
import io.realworld.app.web.controllers.ProfileController
import io.realworld.app.web.controllers.TagController
import io.realworld.app.web.controllers.UserController
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object ModulesConfig {
    private val userModule = Kodein.Module("USER") {
        bind() from singleton { UserController(instance()) }
        bind() from singleton { UserService(JwtProvider, instance()) }
        bind() from singleton { UserRepository() }
    }
    private val articleModule = Kodein.Module("ARTICLE") {
        bind() from singleton { ArticleController() }
    }
    private val profileModule = Kodein.Module("PROFILE") {
        bind() from singleton { ProfileController() }
    }
    private val commentModule = Kodein.Module("COMMENT") {
        bind() from singleton { CommentController() }
    }
    private val tagModule = Kodein.Module("TAG") {
        bind() from singleton { TagController(instance()) }
        bind() from singleton { TagService(instance()) }
        bind() from singleton { TagRepository() }
    }
    internal val kodein = Kodein {
        import(userModule)
        import(articleModule)
        import(profileModule)
        import(commentModule)
        import(tagModule)
    }
}
