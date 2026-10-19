package de.connect2x.livekitnity.api.e2ee

public enum class E2EEState {
  NEW,
  OK,
  KEY_RATCHETED,
  MISSING_KEY,
  ENCRYPTION_FAILED,
  DECRYPTION_FAILED,
  INTERNAL_ERROR,
}
