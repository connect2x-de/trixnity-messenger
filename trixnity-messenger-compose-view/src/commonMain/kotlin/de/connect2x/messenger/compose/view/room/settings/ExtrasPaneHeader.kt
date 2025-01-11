package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.settings.ExtrasPaneHeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.room.settings.ExtrasPaneHeaderBackButtonType.CLOSE

// TODO: fuse with Header.kt
@Composable
fun ExtrasPaneHeader(
    title: String,
    error: String?,
    onBack: () -> Unit,
    backButtonType: ExtrasPaneHeaderBackButtonType,
    content: @Composable ColumnScope.() -> Unit,
) {
    val i18n = DI.get<I18nView>()
    Box(
        Modifier.fillMaxSize(),
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                Column(
                    Modifier.fillMaxWidth()
//                        .background(Color.Red)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.buttonPointerModifier(),
                        ) {
                            when (backButtonType) {
                                BACK -> Icon(
                                    Icons.AutoMirrored.Default.ArrowBack,
                                    i18n.commonBack(),
                                )

                                CLOSE -> Icon(
                                    Icons.Default.Close,
                                    i18n.commonClose(),
                                )
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = title.capitalize(Locale.current),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                    if (error != null) ErrorView(error)
                }
            }
            content()
        }
    }
}

enum class ExtrasPaneHeaderBackButtonType {
    CLOSE, BACK,
}
