package de.connect2x.livekitnity.demo.shared.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.connect2x.livekitnity.demo.shared.viewmodels.EntryViewModel

@Composable
internal fun ConnectionScreen(
    onConnect: (EntryViewModel.ConnectionInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
  val entryViewModel = viewModel { EntryViewModel() }
  val connectionInfo by entryViewModel.connectionInfo.collectAsState()
  val focusManager = LocalFocusManager.current

  Surface(
      modifier = modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
  ) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Column(
          modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
              text = "Join a room",
              style = MaterialTheme.typography.headlineLarge,
          )
          Text(
              text = "Choose a room and the name you’ll connect with.",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        OutlinedTextField(
            state = entryViewModel.roomName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Room name") },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            shape = MaterialTheme.shapes.medium,
        )

        OutlinedTextField(
            state = entryViewModel.identity,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("User name") },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
            shape = MaterialTheme.shapes.medium,
        )

        ConnectionPreview(info = connectionInfo)

        Button(
            onClick = {
              connectionInfo?.let { info ->
                focusManager.clearFocus()
                onConnect(info)
              }
            },
            enabled = connectionInfo != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
          Text(
              text = "Connect",
              style = MaterialTheme.typography.titleMedium,
          )
        }
      }
    }
  }
}

@Composable
private fun ConnectionPreview(
    info: EntryViewModel.ConnectionInfo?,
    modifier: Modifier = Modifier,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }

  Card(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
  ) {
    Surface(
        onClick = { expanded = !expanded },
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier =
            Modifier.fillMaxWidth().semantics {
              role = Role.Button
              stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
    ) {
      Row(
          modifier = Modifier.padding(20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
              text = "Connection preview",
              style = MaterialTheme.typography.titleMedium,
          )
          Text(
              text =
                  if (info != null) {
                    "Ready to connect"
                  } else {
                    "Waiting for connection details"
                  },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Text(
            text = if (expanded) "Hide" else "Show",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    AnimatedVisibility(visible = expanded) {
      Column(
          modifier =
              Modifier.padding(
                  start = 20.dp,
                  end = 20.dp,
                  bottom = 20.dp,
              ),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        if (info == null) {
          Text(
              text = "Enter a room and user name to preview your connection.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          PreviewDetail(label = "Room", value = info.roomName)
          PreviewDetail(
              label = "Connecting as",
              value = info.identity,
          )

          PeerPreview(
              label = "Your peer",
              url = info.ownPeer.url,
              hasToken = !info.tokens[info.ownPeer].isNullOrBlank(),
          )

          Text(
              text = "Other peers (${info.otherPeers.size})",
              style = MaterialTheme.typography.titleSmall,
          )

          if (info.otherPeers.isEmpty()) {
            Text(
                text = "No other peers configured",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            info.otherPeers
                .sortedBy { it.url }
                .forEachIndexed { index, peer ->
                  PeerPreview(
                      label = "Peer ${index + 1}",
                      url = peer.url,
                      hasToken = !info.tokens[peer].isNullOrBlank(),
                  )
                }
          }
        }
      }
    }
  }
}

@Composable
private fun PreviewDetail(
    label: String,
    value: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
private fun PeerPreview(
    label: String,
    url: String,
    hasToken: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = url,
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = if (hasToken) "Token available" else "No token available",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
