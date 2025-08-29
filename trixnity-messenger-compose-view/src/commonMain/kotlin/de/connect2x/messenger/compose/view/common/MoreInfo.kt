package de.connect2x.messenger.compose.view.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView

@Composable
fun ColumnScope.MoreOptions(
    title: @Composable () -> Unit,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Settings,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    MoreInfo(title, enabled, content, icon, modifier)
}


@Composable
fun ColumnScope.MoreOptions(
    title: String,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Settings,
    modifier: Modifier = Modifier,
    expanded: MutableState<Boolean> = remember { mutableStateOf(false) },
    content: @Composable ColumnScope.() -> Unit,
) {
    MoreInfo({ Text(title, style = MaterialTheme.typography.titleSmall) }, enabled, content, icon, modifier, expanded)
}

@Composable
fun ColumnScope.MoreInfo(
    title: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    MoreInfo(
        { Text(title, style = MaterialTheme.typography.titleSmall) },
        enabled,
        content,
        Icons.Default.Info,
        modifier
    )
}

@Composable
private fun ColumnScope.MoreInfo(
    title: @Composable () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    expanded: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    val i18n = DI.get<I18nView>()
    var expanded by expanded
    val rotateState by animateFloatAsState(
        targetValue = if (expanded) 180F else 0F,
    )
    val interactionSource = remember { MutableInteractionSource() }

    // Make sure we are not expanded when disabled
    if (!enabled) expanded = false

    Card(
        modifier = modifier
            .clickable(interactionSource, indication = null, onClick = {
                if (enabled) expanded = expanded.not()
            })
            .buttonPointerModifier(),
        colors = if (enabled) CardDefaults.cardColors()
        else CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, i18n.commonInformation())
                Spacer(Modifier.size(10.dp))
                title()
                Spacer(Modifier.weight(1F).padding(end = 10.dp))
                Icon(
                    Icons.Default.ArrowDropDown, i18n.commonMoreInformation(),
                    modifier = Modifier.rotate(rotateState)
                )
            }
            AnimatedVisibility(
                visible = expanded,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    content()
                }
            }
        }
    }
}

