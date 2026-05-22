package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView

@Composable
actual fun PlatformAppInfo() {
    val i18n = DI.current.get<I18nView>()
    val info = remember {
        var runtime = System.getProperty("java.vendor")
        runtime += " ${System.getProperty("java.vm.version")}"
        i18n.appInfoJvmPlatform(runtime, System.getProperty("os.name"))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        Text(text = info, fontSize = 12.sp)
    }
}
