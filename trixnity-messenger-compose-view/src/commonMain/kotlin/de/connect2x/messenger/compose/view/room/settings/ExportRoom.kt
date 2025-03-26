package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.RunningText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.export.CSVFileBasedExportRoomProperties
import de.connect2x.trixnity.messenger.export.Destination
import de.connect2x.trixnity.messenger.export.FileBasedExportRoomProperties
import de.connect2x.trixnity.messenger.export.PlainTextFileBasedExportRoomProperties
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModel
import io.github.oshai.kotlinlogging.KotlinLogging


private val log = KotlinLogging.logger {}

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

        Column(Modifier.fillMaxSize()) {
            Header(
                exportRoomViewModel::back, i18n.exportRoom(
                    if (isDirect) i18n.commonChat().capitalize(Locale.current)
                    else i18n.commonGroup().capitalize(Locale.current)
                )
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(PaddingValues(vertical = 20.dp, horizontal = 20.dp))
            ) {
                RunningText(text = i18n.exportRoomBodyLabel(roomName))
                Spacer(modifier = Modifier.height(20.dp))
                // TODO Range(exportRoomViewModel)
                ExportRoomProperties(exportRoomViewModel)
                Spacer(modifier = Modifier.height(20.dp))
                when (state) {
                    ExportRoomViewModel.State.None -> {}
                    is ExportRoomViewModel.State.Running -> {
                        val progress by state.progress.collectAsState()
                        val progressString by state.progressString.collectAsState()
                        val (processed, total) = progress
                        if (processed == null || total == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                        else LinearProgressIndicator(
                            progress = { processed.toFloat() / total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(progressString)
                        Spacer(Modifier.size(20.dp))
                    }

                    is ExportRoomViewModel.State.Success -> {
                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(state.progressString)
                        Spacer(Modifier.size(20.dp))
                    }

                    is ExportRoomViewModel.State.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.size(20.dp))
                        // TODO list missing media
                    }
                }

                Row(Modifier.fillMaxWidth()) {
                    val abortText = i18n.exportRoomAbort()
                    Column(Modifier.weight(0.5f), horizontalAlignment = Alignment.Start) {
                        ThemedButton(
                            style = MaterialTheme.components.destructiveButton,
                            onClick = { exportRoomViewModel.abort() },
                            enabled = isExporting,
                        ) {
                            Text(
                                text = abortText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    val exportRoomText = i18n.exportRoomButton()
                    Column(Modifier.weight(0.5f), horizontalAlignment = Alignment.End) {
                        ThemedButton(
                            style = MaterialTheme.components.primaryButton,
                            onClick = { exportRoomViewModel.start() },
                            enabled = canExport && !isExporting,
                        ) {
                            Text(
                                text = exportRoomText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedIndex = 0 },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                i18n.exportRoomTargetPlainText(),
                modifier = Modifier.weight(1.0f, fill = true),
                style = MaterialTheme.typography.labelLarge,
            )
            RadioButton(
                selected = selectedIndex == 0,
                onClick = { selectedIndex = 0 }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedIndex = 1 },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                i18n.exportRoomTargetCsv(),
                modifier = Modifier.weight(1.0f, fill = true),
                style = MaterialTheme.typography.labelLarge,
            )
            RadioButton(
                selected = selectedIndex == 1,
                onClick = { selectedIndex = 1 }
            )
        }
    }
    Spacer(modifier = Modifier.height(20.dp))

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

        else -> {
            log.warn { "invalid room export target" }
        }
    }
}

@Composable
internal expect fun SelectExportDestination(properties: FileBasedExportRoomProperties?, result: (Destination?) -> Unit)
