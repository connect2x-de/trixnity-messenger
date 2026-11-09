package de.connect2x.livekitnity.api.audio

public fun interface AudioDeviceChangeListener {
  public fun onChange(audioDevices: List<AudioDevice>, selectedAudioDevice: AudioDevice?)
}
