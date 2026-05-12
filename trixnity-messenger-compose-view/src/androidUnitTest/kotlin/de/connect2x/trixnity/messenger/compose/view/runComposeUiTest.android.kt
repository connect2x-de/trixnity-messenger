package de.connect2x.trixnity.messenger.compose.view

actual fun runComposeUiTest(block: suspend ComposeUiTestWithBackgroundScope.() -> Unit) {
    throw Exception("tests requiring a MatrixMultiMessenger should be run as instrumented tests")
}
