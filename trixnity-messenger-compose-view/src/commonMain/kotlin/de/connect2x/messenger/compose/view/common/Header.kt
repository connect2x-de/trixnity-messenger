package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.root.IsSinglePane
import de.connect2x.messenger.compose.view.theme.MaxHeaderHeight


@Composable
fun Header(
    onBack: () -> Unit,
    title: String,
    backButtonType: HeaderBackButtonType = BACK,
    additionalButtons: @Composable (() -> Unit)? = null,
) {
    Header(
        onBack,
        { Text(title.capitalize(Locale.current), style = MaterialTheme.typography.titleMedium) },
        backButtonType,
        additionalButtons,
    )
}

@Composable
fun Header(
    onBack: () -> Unit,
    title: @Composable () -> Unit,
    backButtonType: HeaderBackButtonType = BACK,
    additionalButtons: @Composable (() -> Unit)? = null,
) {
    val i18n = DI.get<I18nView>()
    val headerHeightFlow = MaxHeaderHeight.current
    val headerHeight = headerHeightFlow.collectAsState().value
    val density = LocalDensity.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                    val newHeaderHeight = with(density) { coordinates.size.height.toDp() - 1.toDp() }
                    headerHeightFlow.value = maxOf(headerHeight, newHeaderHeight)
                }) {
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
                        title()
                        if (additionalButtons != null) {
                            Spacer(Modifier.weight(1.0f, false).fillMaxWidth())
                            additionalButtons()
                        }
                    }
                }

                // If we have a multi-pane view, we will display an invisible text that has the function of forcing the
                // three header elements to the same height.
                if (!IsSinglePane.current) {
                    Text(
                        text = " ",
                        style = MaterialTheme.typography.labelMedium
                            .copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.height(headerHeight)
                    )
                }
            }
            HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
        }
    }
}

enum class HeaderBackButtonType {
    CLOSE, BACK,
}
