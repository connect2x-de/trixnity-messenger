package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformUriCallerModule(): Module = module {
    single<UriCaller> {
        val context = get<Context>()
        UriCaller { uri -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
    }
}
