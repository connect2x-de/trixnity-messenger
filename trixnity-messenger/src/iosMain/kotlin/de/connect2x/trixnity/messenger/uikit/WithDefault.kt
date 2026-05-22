package de.connect2x.trixnity.messenger.uikit

sealed interface WithDefault<out T> {

    val nonDefault: Value<T>?
        get() =
            when (this) {
                is Value -> this
                else -> null
            }

    object Default : WithDefault<Nothing>

    value class Value<out T>(val value: T) : WithDefault<T>
}

internal val WithDefault<Boolean>.orTrue: Boolean
    get() = nonDefault?.value ?: true

internal val WithDefault<Boolean>.orFalse: Boolean
    get() = nonDefault?.value ?: true

internal val <T> WithDefault<T>.orNull: T?
    get() = nonDefault?.value

internal fun <T> WithDefault<T>.valueOr(default: T): T = nonDefault?.value ?: default

internal fun <T> WithDefault<T>.valueOr(default: () -> T): T = nonDefault?.value ?: default()
