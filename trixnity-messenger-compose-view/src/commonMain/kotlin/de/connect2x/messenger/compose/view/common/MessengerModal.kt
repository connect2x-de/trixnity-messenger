package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration

@Composable
fun MessengerModal(
    onDismiss: (() -> Unit)? = null,
    title: String,
    width: Dp = 800.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .blockPointerInput()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp))
                .width(width)
                .border(Dp.Hairline, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            Column {
                MessengerModalHeader(onDismiss, title)
                Column(Modifier.padding(20.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ColumnScope.MessengerModalContent(content: @Composable ColumnScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Column(Modifier.verticalScroll(scrollState).weight(1.0f, fill = true)) {
        content()
    }
    // do not display scroll bar as it sets the height to max and is not used on mobile (where scrolling might be needed)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.MessengerModalButtonRow(
    button1: @Composable RowScope.() -> Unit,
    button2: (@Composable RowScope.() -> Unit)? = null,
    button3: (@Composable RowScope.() -> Unit)? = null,
) {
    Spacer(Modifier.size(20.dp))
    Column(
        Modifier.fillMaxWidth().weight(1.0f, fill = false),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        FlowRow(horizontalArrangement = Arrangement.SpaceEvenly) {
            button1()
            if (button2 != null) {
                Spacer(Modifier.size(10.dp))
                button2()
            }
            if (button3 != null) {
                Spacer(Modifier.size(10.dp))
                button3()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
        /**
         * This composable with layout buttons at the end of the modal (to the right).
         * The miscellaneous action is separated from the others and the next and back action
         * are grouped, so that they always appear on the same row:
         * | _______ misc __ back _ next | or on smaller devices
         * | _________ misc |
         * | __ back _ next |
         * @param next a button to go to the next action
         * @param back a button to go to the previous action
         * @param misc a miscellaneous third action which does not fit into the typical next or back action
         */
fun ColumnScope.MessengerModalThreeButtonRow(
    next: @Composable RowScope.() -> Unit,
    back: (@Composable RowScope.() -> Unit)? = null,
    misc: (@Composable RowScope.() -> Unit)? = null,
) {
    MiddleSpacer()
    Column(
        Modifier.fillMaxWidth().weight(1.0f, fill = false),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        FlowRow(horizontalArrangement = Arrangement.End) {
            if (misc != null) {
                misc()
            }
            Row {
                if (misc != null) {
                    SmallSpacer()
                }
                if (back != null) {
                    back()
                    SmallSpacer()
                }
                next()
            }
        }
    }
}

@Composable
fun RowScope.NextButton(enabled: Boolean = true, text: String? = null, nextAction: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Button(
        onClick = nextAction,
        modifier = Modifier.buttonPointerModifier().weight(1.0f, fill = false)
            .width(IntrinsicSize.Max), // avoid wrapping button text if possibles
        enabled = enabled,
    ) {
        Text(text ?: i18n.commonNext().capitalize(Locale.current))
    }
}

@Composable
fun RowScope.CloseModalButton(closeModalAction: () -> Unit, caption: String? = null) {
    val i18n = DI.get<I18nView>()
    Button(
        onClick = { closeModalAction() },
        modifier = Modifier.buttonPointerModifier().weight(1.0f, fill = false)
            .width(IntrinsicSize.Max), // avoid wrapping button text if possible
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = Color.White
        )
    ) {
        Text(caption ?: i18n.commonClose())
    }
}

@Composable
fun RowScope.CloseMessengerButton(closeMessengerAction: () -> Unit) {
    val i18n = DI.get<I18nView>()
    val appName = DI.get<MatrixMessengerConfiguration>().appName
    Button(
        onClick = { closeMessengerAction() },
        modifier = Modifier.buttonPointerModifier().weight(1.0f, fill = false),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = Color.White
        )
    ) {
        Text(i18n.closeApp(appName))
    }
}

@Composable
fun RowScope.BackButton(onBack: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Button(
        onClick = onBack,
        modifier = Modifier.buttonPointerModifier().weight(1.0f, fill = false),
    ) {
        Text(i18n.commonBack())
    }
}


@Composable
private fun MessengerModalHeader(onDismiss: (() -> Unit)?, title: String) {
    val i18n = DI.get<I18nView>()
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(10.dp)
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            Modifier.weight(1.0f, fill = true),
            style = MaterialTheme.typography.titleLarge,
        )
        if (onDismiss != null)
            IconButton(
                onDismiss,
                Modifier.buttonPointerModifier()
            ) {
                Icon(Icons.Default.Close, i18n.commonCancel().capitalize(Locale.current))
            }
    }
}
