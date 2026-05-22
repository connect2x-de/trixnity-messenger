@file:OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class, Connect2xComposeUiApi::class)

package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Connect2xComposeUiApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.window.ComposeViewportConfiguration
import org.jetbrains.skiko.wasm.onWasmReady as skikoOnWasmReady
import web.html.HTMLDivElement
import web.html.HTMLElement

internal actual fun onWasmReady(onReady: () -> Unit) = skikoOnWasmReady(onReady)

@Suppress("FunctionName")
internal actual fun ComposeViewport(
    viewportContainer: HTMLElement,
    semanticsListener: (a11yContainer: HTMLDivElement) -> PlatformContext.SemanticsOwnerListener,
    configure: ComposeViewportConfiguration.() -> Unit,
    content: @Composable (() -> Unit),
) =
    androidx.compose.ui.window.ComposeViewport(
        viewportContainer = viewportContainer.unsafeCast<org.w3c.dom.Element>(),
        semanticsListener = { semanticsListener(it.unsafeCast<HTMLDivElement>()) },
        configure = configure,
        content = content,
    )
