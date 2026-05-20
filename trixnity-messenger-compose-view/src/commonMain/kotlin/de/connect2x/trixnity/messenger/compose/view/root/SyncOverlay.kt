package de.connect2x.trixnity.messenger.compose.view.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSync
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModel

interface SyncOverlayView {
    @Composable fun create(syncViewModel: SyncViewModel)
}

@Composable
fun SyncOverlay(syncViewModel: SyncViewModel) {
    DI.get<SyncOverlayView>().create(syncViewModel)
}

class SyncOverlayViewImpl : SyncOverlayView {
    @Composable
    override fun create(syncViewModel: SyncViewModel) {

        val i18n = DI.get<I18nView>()
        val accountSyncStates by syncViewModel.accountSyncStates.collectAsState()

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer).clickable {}) {
            Box(Modifier.align(Alignment.Center).padding(20.dp)) {
                Box(
                    Modifier.align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .sizeIn(maxWidth = 500.dp)
                ) {
                    Column(
                        Modifier.align(Alignment.Center).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                            Text(i18n.syncOverlayTitle(), style = MaterialTheme.typography.titleMedium)
                            accountSyncStates?.entries?.map { (userId, accountSync) ->
                                Spacer(Modifier.size(20.dp))
                                when (accountSync) {
                                    AccountSync.INITIAL_SYNC -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1.0f, fill = true)) {
                                                Text(
                                                    i18n.syncOverlayAccount(userId),
                                                    style = MaterialTheme.typography.titleSmall,
                                                )
                                                Text(i18n.syncOverlayInitialSync())
                                                Text(
                                                    text =
                                                        i18n.syncOverlayInitialSyncInfo(
                                                            DI.get<MatrixMessengerConfiguration>().appName
                                                        ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            Spacer(Modifier.size(20.dp))
                                            ThemedProgressIndicator(
                                                Modifier.padding(horizontal = 20.dp),
                                                MaterialTheme.components.circularProgressIndicator,
                                            )
                                        }
                                    }

                                    AccountSync.DONE -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                userId.full,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1.0f, fill = true),
                                            )
                                            Spacer(Modifier.size(20.dp))
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                i18n.commonDone(),
                                                Modifier.padding(end = 25.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
