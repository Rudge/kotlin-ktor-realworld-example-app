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
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object ModulesConfig {
    private val userModule = DI.Module("USER") {
        bind<UserController>() with singleton { UserController(instance()) }
        bind<UserService>() with singleton { UserService(JwtProvider, instance()) }
        bind<UserRepository>() with singleton { UserRepository() }
    }
    private val articleModule = DI.Module("ARTICLE") {
        bind<ArticleController>() with singleton { ArticleController() }
    }
    private val profileModule = DI.Module("PROFILE") {
        bind<ProfileController>() with singleton { ProfileController() }
    }
    private val commentModule = DI.Module("COMMENT") {
        bind<CommentController>() with singleton { CommentController() }
    }
    private val tagModule = DI.Module("TAG") {
        bind<TagController>() with singleton { TagController(instance()) }
        bind<TagService>() with singleton { TagService(instance()) }
        bind<TagRepository>() with singleton { TagRepository() }
    }
    internal val kodein = DI {
        import(userModule)
        import(articleModule)
        import(profileModule)
        import(commentModule)
        import(tagModule)
    }
}
