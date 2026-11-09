package de.connect2x.livekitnity.api.audio

public interface AudioSwitchHandler {
  public val selectedAudioDevice: AudioDevice?
  public val availableAudioDevices: List<AudioDevice>

  public fun selectDevice(audioDevice: AudioDevice?)

  public fun registerAudioDeviceChangeListener(listener: AudioDeviceChangeListener)

  public fun unregisterAudioDeviceChangeListener(listener: AudioDeviceChangeListener)
}
