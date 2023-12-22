package de.connect2x.trixnity.messenger

import android.content.Context
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.defaultComponentContext

suspend fun createMatrixMessenger(
    context: Context,
    componentContext: ComponentContext,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger = MatrixMessenger.create(componentContext, configuration) {
    single<Context> { context }
}

suspend fun <T> T.createMatrixMessenger(
    context: Context,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger where
        T : SavedStateRegistryOwner, T : OnBackPressedDispatcherOwner, T : ViewModelStoreOwner, T : LifecycleOwner =
    createMatrixMessenger(context, defaultComponentContext(), configuration)

suspend fun <T> T.createMatrixMessenger(
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger where
        T : SavedStateRegistryOwner, T : OnBackPressedDispatcherOwner, T : ViewModelStoreOwner, T : LifecycleOwner, T : Context =
    createMatrixMessenger(this, defaultComponentContext(), configuration)