package de.connect2x.trixnity.messenger.util

import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import de.connect2x.lognity.api.logger.Logger
import org.koin.core.module.Module
import org.koin.dsl.module

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.UriCallerKt")

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        val activityGetter = get<ActivityGetter>()
        UriCaller { uri, _ ->
            val safeUri = uri.toUri()
            log.info { "call uri: $safeUri" }
            val customTabsIntent = CustomTabsIntent.Builder().build()
            activityGetter().let { customTabsIntent.launchUrl(it, safeUri) }
        }
    }
}
