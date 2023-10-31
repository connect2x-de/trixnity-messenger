package de.connect2x.trixnity.messenger

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import org.koin.core.context.GlobalContext

fun getContext() = GlobalContext.get().get<Context>()

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}