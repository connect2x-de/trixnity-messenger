package de.connect2x.livekitnity.core.e2ee

import de.connect2x.livekitnity.api.e2ee.KeyProvider
import io.livekit.android.e2ee.E2EEManager
import livekit.org.webrtc.FrameCryptor

internal class AndroidKeyProvider(
    private val keyProvider: NativeKeyProvider,
    private val e2EEManager: () -> E2EEManager?,
) : KeyProvider {

  override suspend fun setKey(key: ByteArray, participantId: String, keyIndex: Int) {
    keyProvider.setKey(key, participantId, keyIndex)

    val e2eeManager = e2EEManager() ?: return

    e2eeManager.frameCryptors.forEach { (pair, cryptor) ->
      if (pair.second.value == participantId) {
        cryptor.keyIndex = keyIndex
      }
    }
  }
}

private val frameCryptorsField =
    E2EEManager::class.java.getDeclaredField("frameCryptors").apply {
      isAccessible = true
    }

@Suppress("UNCHECKED_CAST")
private val E2EEManager.frameCryptors:
    Map<Pair<String, io.livekit.android.room.participant.Participant.Identity>, FrameCryptor>
  get() =
      frameCryptorsField.get(this)
          as
          Map<Pair<String, io.livekit.android.room.participant.Participant.Identity>, FrameCryptor>
