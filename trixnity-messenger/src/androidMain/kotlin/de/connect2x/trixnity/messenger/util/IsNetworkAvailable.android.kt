package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformIsNetworkAvailableModule(): Module = module {
    single<IsNetworkAvailable> {
        val context = get<Context>()
        IsNetworkAvailable {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            networkCapabilities != null &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}