package de.connect2x.livekitnity.core.audio

import de.connect2x.livekitnity.api.audio.AudioDevice
import de.connect2x.livekitnity.api.audio.AudioDeviceChangeListener
import de.connect2x.livekitnity.api.audio.AudioSwitchHandler
import de.connect2x.livekitnity.core.platform.Converter

internal interface AndroidAudioSwitchHandler : AudioSwitchHandler

internal fun AndroidAudioSwitchHandler(
    audioSwitchHandler: io.livekit.android.audio.AudioSwitchHandler,
): AndroidAudioSwitchHandler {
  return AndroidAudioSwitchHandlerImpl(
      audioSwitchHandler = audioSwitchHandler,
  )
}

private class AndroidAudioSwitchHandlerImpl(
    private val audioSwitchHandler: io.livekit.android.audio.AudioSwitchHandler,
) : AndroidAudioSwitchHandler {

  override val selectedAudioDevice: AudioDevice?
    get() = Converter.toCommon(audioSwitchHandler.selectedAudioDevice ?: return null)

  override val availableAudioDevices: List<AudioDevice>
    get() = Converter.toCommon(audioSwitchHandler.availableAudioDevices)

  override fun selectDevice(audioDevice: AudioDevice?) {
    if (audioDevice == null) {
      audioSwitchHandler.selectDevice(null)
    } else {
      audioSwitchHandler.selectDevice(findMatchingDevice(audioDevice) ?: return)
    }
  }

  private val listenerMap =
      mutableMapOf<AudioDeviceChangeListener, com.twilio.audioswitch.AudioDeviceChangeListener>()

  override fun registerAudioDeviceChangeListener(listener: AudioDeviceChangeListener) {
    val nativeListener: com.twilio.audioswitch.AudioDeviceChangeListener =
        { audioDevices, selectedAudioDevice ->
          listener.onChange(
              audioDevices = Converter.toCommon(audioDevices),
              selectedAudioDevice =
                  if (selectedAudioDevice == null) null
                  else Converter.toCommon(selectedAudioDevice),
          )
        }
    synchronized(listenerMap) {
      if (listenerMap.putIfAbsent(listener, nativeListener) != null) return
    }
    audioSwitchHandler.registerAudioDeviceChangeListener(nativeListener)
  }

  override fun unregisterAudioDeviceChangeListener(listener: AudioDeviceChangeListener) {
    val nativeListener = synchronized(listenerMap) { listenerMap.remove(listener) } ?: return
    audioSwitchHandler.unregisterAudioDeviceChangeListener(nativeListener)
  }

  private fun findMatchingDevice(audioDevice: AudioDevice): com.twilio.audioswitch.AudioDevice? {
    return audioSwitchHandler.availableAudioDevices.find { it.name == audioDevice.name }
  }
}
