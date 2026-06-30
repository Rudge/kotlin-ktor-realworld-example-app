package io.realworld.app.ext

const val MAIL_REGEX = ("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
    + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
    + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
    + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
    + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
    + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$")

fun String.isEmailValid(): Boolean = !this.isNullOrBlank() && Regex(MAIL_REGEX).matches(this)

/**
 * Converts a title to a URL slug (RealWorld convention): lowercase, non-alphanumeric → hyphens.
 * Example: "Many favorites" → "many-favorites". Used when persisting articles and in tests.
 */
fun String.toSlug(): String = lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
