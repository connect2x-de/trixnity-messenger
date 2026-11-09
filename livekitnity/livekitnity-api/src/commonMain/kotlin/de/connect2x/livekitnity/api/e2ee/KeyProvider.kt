package de.connect2x.livekitnity.api.e2ee

public interface KeyProvider {
  public suspend fun setKey(key: ByteArray, participantId: String, keyIndex: Int = 0)
}
