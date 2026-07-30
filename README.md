[![Travis](https://img.shields.io/travis/Rudge/kotlin-ktor-realworld-example-app.svg)](https://travis-ci.org/Rudge/kotlin-ktor-realworld-example-app/builds)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/5b6503dfa3024a0dbbf173e333f80bcf)](https://app.codacy.com/app/Rudge/kotlin-ktor-realworld-example-app?utm_source=github.com&utm_medium=referral&utm_content=Rudge/kotlin-ktor-realworld-example-app&utm_campaign=Badge_Grade_Dashboard)
[![BCH compliance](https://bettercodehub.com/edge/badge/Rudge/kotlin-ktor-realworld-example-app?branch=master)](https://bettercodehub.com/)

# ![RealWorld Example App](logo.png)

> ### Kotlin + Ktor codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API

### [RealWorld](https://github.com/gothinkster/realworld)

This codebase was created to demonstrate a fully fledged fullstack application built with **Kotlin + Ktor + Kodein + Exposed** including CRUD operations, authentication, routing, pagination, and more.

We've gone to great lengths to adhere to the **Kotlin + Ktor** community styleguides & best practices.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# How it works

The application was built with:

  - [Kotlin](https://github.com/JetBrains/kotlin) as programming language
  - [Ktor](https://github.com/ktorio/ktor) as web framework
  - [Kodein](https://github.com/Kodein-Framework/Kodein-DI) as dependency injection framework
  - [Jackson](https://github.com/FasterXML/jackson-module-kotlin) as data bind serialization/deserialization
  - [Java-jwt](https://github.com/auth0/java-jwt) for JWT spec implementation
  - [HikariCP](https://github.com/brettwooldridge/HikariCP) as datasource to abstract driver implementation
  - [H2](https://github.com/h2database/h2database) as database
  - [Exposed](https://github.com/JetBrains/Exposed) as Sql framework to persistence layer
  - [slugify](https://github.com/slugify/slugify)

Tests:

  - [junit](https://github.com/junit-team/junit4)
  - [Unirest](https://github.com/Kong/unirest-java) to call endpoints in tests

#### Structure
      + config/
          All app setups. Ktor, Kodein and Database
      + domain/
        + repository/
            Persistence layer and tables definition
        + service/
            Logic layer and transformation data
      + ext/
          Extension of String for email validation
      + utils/
          Jwt and Encrypt classes
      + web/
        + controllers
            Classes and methods to mapping actions of routes
        Router definition to features and exceptions
      - App.kt <- The main class

# Getting started

You need a JDK 17 or newer installed. The build targets JVM 17 bytecode and is verified in CI on JDK 17 and 21.

The server starts on [8080](http://localhost:8080) and all routes are served under `/api`, per the RealWorld spec.

Build:
> ./gradlew clean build

Start the server:
> ./gradlew run

Run the tests:
> ./gradlew test

In the project have the [spec-api](https://github.com/Rudge/kotlin-ktor-realworld-example-app/tree/master/spec-api) with the README and collections to execute backend tests specs [realworld](https://github.com/gothinkster/realworld).

Execute tests and start the server:

> ./gradlew run & APIURL=http://localhost:8080/api ./spec-api/run-api-tests.sh

## Popular articles feed

`GET /api/articles/feed/popular` returns articles ranked by favorite count, most favorited
first, with newest-first as the tie-break so paging over an equally-favorited set is stable.

Requires authentication (`Authorization: Token <jwt>`).

| Query param | Default | Rules |
|---|---|---|
| `limit` | 20 | positive integer, max 100 |
| `offset` | 0 | zero or a positive integer |

```
curl -H "Authorization: Token $TOKEN" \
  "http://localhost:8080/api/articles/feed/popular?limit=20&offset=0"
```

```json
{
  "articles": [
    {
      "slug": "alpha-post",
      "title": "Alpha post",
      "description": "d",
      "body": "b",
      "tagList": ["dragons", "training"],
      "createdAt": "2026-07-30T16:51:58.603+00:00",
      "updatedAt": "2026-07-30T16:51:58.603+00:00",
      "favorited": true,
      "favoritesCount": 2,
      "author": { "username": "author", "bio": null, "image": null, "following": false }
    }
  ],
  "articlesCount": 3
}
```

`favorited` and `author.following` are resolved for the authenticated caller.
`articlesCount` is the total number of articles available, not the size of the page.

Errors follow the RealWorld shape `{"errors":{"body":["..."]}}`: `401` without a valid token,
`422` for a malformed `limit`/`offset`.

## Supporting endpoints

Implemented alongside the feed, since a ranking is only meaningful once articles can be
created and favorited:

| Method | Route | Notes |
|---|---|---|
| `POST` | `/api/articles` | Slug derived from the title, `-N` suffix on collision. `422` on a blank title or body |
| `POST` | `/api/articles/{slug}/favorite` | Idempotent. `404` on an unknown slug |
| `DELETE` | `/api/articles/{slug}/favorite` | No-op when not favorited. `404` on an unknown slug |

All three require authentication. `GET /api/tags` returns tags collected from created articles.
