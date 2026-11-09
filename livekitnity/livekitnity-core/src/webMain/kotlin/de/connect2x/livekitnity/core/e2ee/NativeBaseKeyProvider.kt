package de.connect2x.livekitnity.core.e2ee

import de.connect2x.livekitnity.api.e2ee.KeyDerivationAlgorithm
import kotlin.js.Promise
import livekit.BaseKeyProvider
import org.khronos.webgl.Int8Array

internal external interface NativeBaseKeyProvider : BaseKeyProvider {
  fun setKey(
      key: Int8Array,
      participantId: String = definedExternally,
      keyIndex: Int = definedExternally,
  ): Promise<Nothing?>
}

internal fun NativeBaseKeyProvider(
    options: livekit.PartialKeyProviderOptions,
    algorithm: KeyDerivationAlgorithm,
): NativeBaseKeyProvider {
  val provider = BaseKeyProvider(options)
  val algorithm =
      when (algorithm) {
        KeyDerivationAlgorithm.PBKDF2 -> "PBKDF2"
        KeyDerivationAlgorithm.HKDF -> "HKDF"
      }
  return addSetKey(provider, algorithm)
}

@JsFun(
    """
    (provider, algorithm) => {
      provider.setKey = async function(key, participantId, keyIndex) {
        return globalThis.crypto.subtle.importKey(
          "raw",
          key,
          { name: algorithm },
          false,
          ["deriveBits", "deriveKey"]
        ).then(importedKey => {
          this.onSetEncryptionKey(
              importedKey,
              participantId,
              keyIndex
          );
        });
      };

      return provider;
    }
""",
)
private external fun addSetKey(
    provider: BaseKeyProvider,
    algorithm: String,
): NativeBaseKeyProvider
