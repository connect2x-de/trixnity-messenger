package de.connect2x.trixnity.messenger.compose.view

import androidx.test.platform.app.InstrumentationRegistry
import de.connect2x.lognity.api.config.ConfigBuilder
import de.connect2x.lognity.config.fileAppender
import java.io.File

actual fun ConfigBuilder.modifyLogging() {
    val basePath = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
    checkNotNull(basePath) { "additionalTestOutputDir not specified" }

    fileAppender(
        File(basePath).resolve("logs.txt").toString(),
        "{{levelColor}}>> {{levelSymbol}} {{hh}}:{{mm}}:{{ss}}.{{SSS}} [{{threadId}}/{{coroutineName}}][{{name}}] {{message}}{{r}}",
    )
}
