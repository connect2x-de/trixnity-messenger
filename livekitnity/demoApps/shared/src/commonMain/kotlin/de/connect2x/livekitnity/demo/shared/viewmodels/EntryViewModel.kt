package de.connect2x.livekitnity.demo.shared.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.connect2x.livekitnity.demo.shared.token.DefaultLiveKitTokenGenerator
import de.connect2x.livekitnity.demo.shared.token.LiveKitTokenGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

internal class EntryViewModel : ViewModel() {

  private val tokenGenerator = DefaultLiveKitTokenGenerator()

  val roomName: TextFieldState = TextFieldState()

  val identity: TextFieldState = TextFieldState()

  val connectionInfo: StateFlow<ConnectionInfo?> =
      combine(
              snapshotFlow { roomName.text },
              snapshotFlow { identity.text },
              flowOf(defaultPeers),
          ) { roomName, identity, peers ->
            if (roomName.isBlank()) return@combine null
            if (identity.isBlank()) return@combine null

            val identity = identity.toString()
            val roomName = roomName.toString()

            val tokens = peers.associateWith { peer ->
              val token =
                  tokenGenerator.createToken(
                      LiveKitTokenGenerator.Options(
                          apiKey = peer.apiKey,
                          apiSecret = peer.apiSecret,
                          identity = identity,
                          roomName = roomName,
                      ),
                  )
              token
            }

            val ownPeer = peers.random()
            val otherPeers = peers - ownPeer

            ConnectionInfo(
                ownPeer = ownPeer,
                otherPeers = otherPeers,
                tokens = tokens,
                roomName = roomName,
                identity = identity,
            )
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = null,
          )

  data class ConnectionInfo(
      val ownPeer: Peer,
      val otherPeers: Set<Peer>,
      val tokens: Map<Peer, String>,
      val roomName: String,
      val identity: String,
  )

  data class Peer(
      val url: String,
      val apiKey: String = "devkey",
      val apiSecret: String = "secret",
  )

  companion object {
    private val defaultPeers: Set<Peer> =
        setOf(
            Peer("ws://127.0.0.1:7880"),
            Peer("ws://127.0.0.1:7890"),
            Peer("ws://127.0.0.1:7900"),
        )
  }
}
