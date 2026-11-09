@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress(
    // "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "RemoveRedundantBackticks",
    "RemoveExplicitTypeArguments",
    "RedundantUnitReturnType",
    "UnnecessaryOptInAnnotation",
    // "unused",
)

package livekit

import kotlin.js.*

public external interface LocalParticipant : Participant {
  public fun `setE2EEEnabled`(`enabled`: Boolean): Promise<Nothing?>

  public override fun `getTrackPublication`(`source`: TrackSource): LocalTrackPublication?

  public override fun `getTrackPublicationByName`(`name`: String): LocalTrackPublication?

  public fun `setName`(`name`: String): Promise<Nothing?>

  public fun `setMetadata`(`metadata`: String): Promise<Nothing?>

  public fun `setAttributes`(`attributes`: JsRecord<JsString>): Promise<Nothing?>

  public fun `publishDtmf`(`code`: Double, `digit`: String): Promise<Nothing?>

  public fun `setCameraEnabled`(
      `enabled`: Boolean,
      `options`: VideoCaptureOptions? = definedExternally,
      `publishOptions`: TrackPublishOptions? = definedExternally,
  ): Promise<LocalTrackPublication?>

  public fun `setMicrophoneEnabled`(
      `enabled`: Boolean,
      `options`: AudioCaptureOptions? = definedExternally,
      `publishOptions`: TrackPublishOptions? = definedExternally,
  ): Promise<LocalTrackPublication?>

  public fun `setScreenShareEnabled`(
      `enabled`: Boolean,
      `options`: ScreenShareCaptureOptions? = definedExternally,
      `publishOptions`: TrackPublishOptions? = definedExternally,
  ): Promise<LocalTrackPublication?>

  public fun `enableCameraAndMicrophone`(): Promise<Nothing?>

  public fun `createTracks`(
      `options`: CreateLocalTracksOptions? = definedExternally,
  ): Promise<JsArray<LocalTrack>>

  public fun `createScreenTracks`(
      `options`: ScreenShareCaptureOptions? = definedExternally,
  ): Promise<JsArray<LocalTrack>>

  public fun `publishTrack`(
      `track`: PublishableTrack,
      `options`: TrackPublishOptions? = definedExternally,
  ): Promise<LocalTrackPublication>

  public fun `unpublishTrack`(
      `track`: PublishableTrack,
      `stopOnUnpublish`: Boolean? = definedExternally,
  ): Promise<LocalTrackPublication?>

  public fun `publishData`(
      `data`: org.khronos.webgl.Uint8Array,
      `options`: DataPublishOptions? = definedExternally,
  ): Promise<Nothing?>

  public val `lastCameraError`: JsError?
  public val `lastMicrophoneError`: JsError?
}

public object LocalParticipantEvent {
  public val LocalTrackUnpublished: EventCallback1<LocalTrackPublication> =
      eventCallback1("localTrackUnpublished")
  public val LocalTrackPublished: EventCallback1<LocalTrackPublication> =
      eventCallback1("localTrackPublished")
  public val MediaDevicesError: EventCallback2<JsError, MediaDeviceKind?> =
      eventCallback2("mediaDevicesError")
}
