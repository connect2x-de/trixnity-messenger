package de.connect2x.messenger.compose.view.settings

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
import platform.UIKit.UIDevice

@Composable
actual fun PlatformAppInfo() {
    val info = remember {
        val device = UIDevice.currentDevice
        "${device.systemName} ${device.systemVersion}"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(10.dp).fillMaxWidth()
    ) {
        Text(
            text = info,
            fontSize = 12.sp
        )
    }
}
