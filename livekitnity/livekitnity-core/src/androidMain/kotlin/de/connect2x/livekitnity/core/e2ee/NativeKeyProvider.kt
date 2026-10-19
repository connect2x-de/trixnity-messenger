package de.connect2x.livekitnity.core.e2ee

import io.livekit.android.e2ee.KeyProvider

internal class NativeKeyProvider(
    base: KeyProvider,
) : KeyProvider by base {
  private val latestSetIndex = mutableMapOf<String, Int>()

  override fun setKey(key: String, participantId: String?, keyIndex: Int?) {
    error("setKey with a string key is a footgun and should not be used because of encoding issues")
  }

  fun setKey(key: ByteArray, participantId: String, keyIndex: Int) {
    check(!enableSharedKey) { "setKey can only be used for non shared keys" }
    latestSetIndex[participantId] = keyIndex
    rtcKeyProvider.setKey(participantId, keyIndex, key)
  }

  override fun getLatestKeyIndex(participantId: String): Int {
    return latestSetIndex[participantId] ?: 0
  }
}
