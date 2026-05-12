package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogScrollContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemRadioButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.export.CSVFileBasedExportRoomProperties
import de.connect2x.trixnity.messenger.export.Destination
import de.connect2x.trixnity.messenger.export.FileBasedExportRoomProperties
import de.connect2x.trixnity.messenger.export.PlainTextFileBasedExportRoomProperties
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModel

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.room.settings.ExportRoomKt")

@Composable
fun ExportRoomContainer(exportRoomViewModel: ExportRoomViewModel) {
    Box(Modifier.fillMaxWidth().clickable(enabled = false) {}) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            ExportRoom(exportRoomViewModel)
        }
    }
}


interface ExportRoomView {
    @Composable
    fun create(exportRoomViewModel: ExportRoomViewModel)
}

@Composable
fun ExportRoom(exportRoomViewModel: ExportRoomViewModel) {
    DI.get<ExportRoomView>().create(exportRoomViewModel)
}

class ExportRoomViewImpl : ExportRoomView {
    @Composable
    override fun create(exportRoomViewModel: ExportRoomViewModel) {
        val i18n = DI.get<I18nView>()

        val roomName = exportRoomViewModel.roomName.collectAsState().value
        val isDirect = exportRoomViewModel.isDirect.collectAsState().value
        val state = exportRoomViewModel.state.collectAsState().value

        val canExport by exportRoomViewModel.canExport.collectAsState()
        val isExporting by exportRoomViewModel.isExporting.collectAsState()

        Column {
            Header(
                onBack = exportRoomViewModel::back,
                title = i18n.exportRoom(
                    if (isDirect) i18n.commonChat().capitalize(Locale.current)
                    else i18n.commonGroup().capitalize(Locale.current)
                )
            )

            // TODO Range(exportRoomViewModel)
            AdaptiveDialogScrollContent {
                Text(i18n.exportRoomBodyLabel(roomName))

                Spacer(Modifier.height(16.dp))

                ExportRoomProperties(exportRoomViewModel)

                Spacer(Modifier.height(16.dp))

                when (state) {
                    ExportRoomViewModel.State.None -> {}
                    is ExportRoomViewModel.State.Running -> {
                        val (processed, total) = state.progress.collectAsState().value
                        if (processed == null || total == null)
                            ThemedProgressIndicator(
                                Modifier.fillMaxWidth(),
                                MaterialTheme.components.linearProgressIndicator
                            )
                        else
                            ThemedProgressIndicator(
                                progress = { processed.toFloat() / total },
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.components.linearProgressIndicator
                            )
                        Spacer(Modifier.size(10.dp))
                        Text(state.progressString.collectAsState().value)
                    }

                    is ExportRoomViewModel.State.Success -> {
                        ThemedProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.components.linearProgressIndicator
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(state.progressString)
                    }

                    is ExportRoomViewModel.State.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        // TODO list missing media
                    }
                }
            }

            AdaptiveDialogFooter {
                ThemedButton(
                    style = MaterialTheme.components.destructiveButton,
                    onClick = { exportRoomViewModel.abort() },
                    enabled = isExporting,
                ) { Text(i18n.exportRoomAbort()) }
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = { exportRoomViewModel.start() },
                    enabled = canExport && !isExporting,
                ) { Text(i18n.exportRoomButton()) }
            }
        }
    }
}

@Composable
fun ExportRoomProperties(exportRoomViewModel: ExportRoomViewModel) {
    val i18n = DI.get<I18nView>()
    val properties = exportRoomViewModel.properties.collectAsState().value

    val targets = listOf(
        "txt" to i18n.exportRoomTargetPlainText(),
        "csv" to i18n.exportRoomTargetCsv(),
    )
    var selectedIndex by remember { mutableStateOf(0) }
    val selectedTarget = targets.getOrNull(selectedIndex)

    ThemedListItemRadioButton(
        selected = selectedIndex == 0,
        headlineContent = {
            Text(i18n.exportRoomTargetPlainText(), style = MaterialTheme.typography.labelLarge)
        },
        onChange = { selectedIndex = 0 }
    )

    ThemedListItemRadioButton(
        selected = selectedIndex == 1,
        headlineContent = {
            Text(i18n.exportRoomTargetCsv(), style = MaterialTheme.typography.labelLarge)
        },
        onChange = { selectedIndex = 1 }
    )

    Spacer(Modifier.height(16.dp))

    when (selectedTarget?.first) {
        "txt" -> {
            val selectedProperties = properties as? PlainTextFileBasedExportRoomProperties
            SelectExportDestination(selectedProperties) { destination ->
                log.debug { "exporting room as txt to: $destination" }
                if (destination != null)
                    exportRoomViewModel.properties.value =
                        selectedProperties?.copy(destination = destination)
                            ?: PlainTextFileBasedExportRoomProperties(destination)
            }
        }

        "csv" -> {
            val selectedProperties = properties as? CSVFileBasedExportRoomProperties
            SelectExportDestination(selectedProperties) { destination ->
                log.debug { "exporting room as csv to: $destination" }
                if (destination != null)
                    exportRoomViewModel.properties.value =
                        selectedProperties?.copy(destination = destination)
                            ?: CSVFileBasedExportRoomProperties(destination)
                // TODO other CSVFileBasedExportRoomProperties properties
            }
        }

        else -> log.warn { "invalid room export target" }
    }
}

@Composable
internal expect fun SelectExportDestination(properties: FileBasedExportRoomProperties?, result: (Destination?) -> Unit)
