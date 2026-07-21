package io.realworld.app.config

import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.TagRepository
import io.realworld.app.domain.repository.UserRepository
import io.realworld.app.domain.service.ArticleService
import io.realworld.app.domain.service.TagService
import io.realworld.app.domain.service.UserService
import io.realworld.app.utils.JwtProvider
import io.realworld.app.web.controllers.ArticleController
import io.realworld.app.web.controllers.CommentController
import io.realworld.app.web.controllers.ProfileController
import io.realworld.app.web.controllers.TagController
import io.realworld.app.web.controllers.UserController
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

object ModulesConfig {
    private val userModule = DI.Module("USER") {
        bindSingleton { UserController(instance()) }
        bindSingleton { UserService(JwtProvider, instance()) }
        bindSingleton { UserRepository() }
    }
    private val articleModule = DI.Module("ARTICLE") {
        bindSingleton { ArticleController(instance()) }
        bindSingleton { ArticleService(instance()) }
        bindSingleton { ArticleRepository() }
    }
    private val profileModule = DI.Module("PROFILE") {
        bindSingleton { ProfileController() }
    }
    private val commentModule = DI.Module("COMMENT") {
        bindSingleton { CommentController() }
    }
    private val tagModule = DI.Module("TAG") {
        bindSingleton { TagController(instance()) }
        bindSingleton { TagService(instance()) }
        bindSingleton { TagRepository() }
    }
    internal val kodein = DI {
        import(userModule)
        import(articleModule)
        import(profileModule)
        import(commentModule)
        import(tagModule)
    }
}
