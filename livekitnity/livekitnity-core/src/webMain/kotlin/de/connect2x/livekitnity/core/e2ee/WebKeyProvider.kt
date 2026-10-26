package de.connect2x.livekitnity.core.e2ee

import de.connect2x.livekitnity.api.e2ee.KeyProvider
import de.connect2x.livekitnity.core.platform.Converter
import org.khronos.webgl.toInt8Array

internal fun WebKeyProvider(
    keyProvider: NativeBaseKeyProvider,
): KeyProvider {
  return WebKeyProviderImpl(
      keyProvider = keyProvider,
  )
}

private class WebKeyProviderImpl(
    private val keyProvider: NativeBaseKeyProvider,
) : KeyProvider {
  override suspend fun setKey(key: ByteArray, participantId: String, keyIndex: Int) {
    val setKeyPromise =
        keyProvider.setKey(
            key = key.toInt8Array(),
            participantId = participantId,
            keyIndex = keyIndex,
        )
    Converter.await(setKeyPromise)
  }
}
