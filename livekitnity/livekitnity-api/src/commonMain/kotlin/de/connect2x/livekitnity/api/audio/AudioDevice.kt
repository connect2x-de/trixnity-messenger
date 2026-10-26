package de.connect2x.livekitnity.api.audio

public sealed interface AudioDevice {
  public val name: String

  public data class BluetoothHeadset(
      override val name: String = "Bluetooth",
  ) : AudioDevice

  public data class WiredHeadset(
      override val name: String = "Wired Headset",
  ) : AudioDevice

  public data class Earpiece(
      override val name: String = "Earpiece",
  ) : AudioDevice

  public data class Speakerphone(
      override val name: String = "Speakerphone",
  ) : AudioDevice
}
