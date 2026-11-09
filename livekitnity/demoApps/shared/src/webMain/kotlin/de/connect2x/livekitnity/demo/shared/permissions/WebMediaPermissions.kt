@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.livekitnity.demo.shared.permissions

import kotlin.js.JsAny
import kotlin.js.Promise
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private external interface BrowserPermissionStatus : JsAny {
  val state: String
}

private external interface BrowserSubscription : JsAny {
  fun dispose()
}

private external interface CaptureResult : JsAny {
  val granted: Boolean
  val errorName: String?
  val errorMessage: String?
}

@JsFun(
    """
    (name) => {
        try {
            if (!navigator.permissions) return null;
            return navigator.permissions.query({ name });
        } catch (_) {
            return null;
        }
    }
""",
)
private external fun queryPermission(
    name: String,
): Promise<BrowserPermissionStatus?>

@JsFun(
    """
    (status, onChange) => {
        const listener = () => onChange(status.state);
        status.addEventListener("change", listener);

        return {
            dispose() {
                status.removeEventListener("change", listener);
            }
        };
    }
""",
)
private external fun observePermission(
    status: BrowserPermissionStatus,
    onChange: (String) -> Unit,
): BrowserSubscription

@JsFun(
    """
    (onResume) => {
        const onFocus = () => onResume();
        const onVisibility = () => {
            if (document.visibilityState === "visible") onResume();
        };

        window.addEventListener("focus", onFocus);
        document.addEventListener("visibilitychange", onVisibility);

        return {
            dispose() {
                window.removeEventListener("focus", onFocus);
                document.removeEventListener(
                    "visibilitychange", onVisibility
                );
            }
        };
    }
""",
)
private external fun observePageResume(
    onResume: () -> Unit,
): BrowserSubscription

@JsFun(
    """
    (name) => {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        return Promise.resolve({
            granted: false,
            errorName: "NotSupportedError",
            errorMessage: "Media capture requires a supported, secure context."
        });
    }

    return navigator.mediaDevices
        .getUserMedia({
            video: name === "camera",
            audio: name === "microphone"
        })
        .then(function (stream) {
            var tracks = stream.getTracks();
            for (var i = 0; i < tracks.length; i++) {
                tracks[i].stop();
            }

            return {
                granted: true,
                errorName: null,
                errorMessage: null
            };
        })
        .catch(function (error) {
            return {
                granted: false,
                errorName: String(
                    error != null && error.name != null
                        ? error.name
                        : "Error"
                ),
                errorMessage: String(
                    error != null && error.message != null
                        ? error.message
                        : error
                )
            };
        });
    }
""",
)
private external fun requestBrowserPermission(
    name: String,
): Promise<CaptureResult>

// ---------- Kotlin implementation ----------

private class MediaCaptureException(
    val browserErrorName: String,
    message: String,
) : Exception(message)

public class WebMediaPermissions : MediaPermissions {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val cameraState = MutableStateFlow(PermissionStatus.Unknown)
  private val microphoneState = MutableStateFlow(PermissionStatus.Unknown)

  override val camera: StateFlow<PermissionStatus> = cameraState.asStateFlow()
  override val microphone: StateFlow<PermissionStatus> = microphoneState.asStateFlow()

  private val subscriptions = mutableMapOf<MediaPermission, BrowserSubscription>()

  private val requestMutex = Mutex()
  private var refreshJob: Job? = null
  private var requesting = false
  private var closed = false

  private val pageSubscription = observePageResume {
    refresh()
  }

  init {
    refresh()
  }

  private fun refresh() {
    if (closed || requesting) return

    refreshJob?.cancel()
    refreshJob = scope.launch {
      for (permission in MediaPermission.entries) {
        updateFromBrowser(permission)
      }
    }
  }

  override suspend fun request(
      permission: MediaPermission,
  ): PermissionStatus = requestMutex.withLock {
    check(!closed) { "Permissions controller is closed" }

    requesting = true
    refreshJob?.cancel()

    try {
      // Start immediately, before any suspension, so this can
      // originate directly from a user interaction.
      val result =
          requestBrowserPermission(
                  permission.browserName,
              )
              .await()

      currentCoroutineContext().ensureActive()
      check(!closed) { "Permissions controller is closed" }

      val queried = updateFromBrowser(permission)

      if (result.granted) {
        // If querying is unsupported, successful capture is
        // evidence of permission at this moment.
        val status =
            if (queried == PermissionStatus.Unknown) {
              PermissionStatus.Granted
            } else {
              queried
            }

        state(permission).value = status
        return@withLock status
      }

      if (result.errorName == "NotAllowedError") {
        // A capture denial alone doesn't establish whether
        // another prompt is possible.
        return@withLock queried
      }

      // Device missing, device busy, unsupported context, etc.
      // These aren't permission states.
      throw MediaCaptureException(
          browserErrorName = result.errorName ?: "Error",
          message = result.errorMessage ?: "Media capture failed",
      )
    } finally {
      requesting = false
    }
  }

  private suspend fun updateFromBrowser(
      permission: MediaPermission,
  ): PermissionStatus {
    val browserStatus = queryPermission(permission.browserName).await()

    currentCoroutineContext().ensureActive()

    if (closed) return PermissionStatus.Unknown

    subscriptions.remove(permission)?.dispose()

    if (browserStatus == null) {
      state(permission).value = PermissionStatus.Unknown
      return PermissionStatus.Unknown
    }

    subscriptions[permission] =
        observePermission(browserStatus) {
          if (!closed) {
            state(permission).value = mapStatus(it)
          }
        }

    return mapStatus(browserStatus.state).also {
      state(permission).value = it
    }
  }

  // TODO: where do we call that?
  private fun close() {
    if (closed) return
    closed = true

    pageSubscription.dispose()
    subscriptions.values.forEach { it.dispose() }
    subscriptions.clear()
    scope.cancel()
  }

  private fun state(permission: MediaPermission) =
      when (permission) {
        MediaPermission.Camera -> cameraState
        MediaPermission.Microphone -> microphoneState
      }

  private fun mapStatus(value: String): PermissionStatus =
      when (value) {
        "granted" -> PermissionStatus.Granted
        "prompt" -> PermissionStatus.Promptable
        "denied" -> PermissionStatus.Blocked
        else -> PermissionStatus.Unknown
      }

  private val MediaPermission.browserName: String
    get() =
        when (this) {
          MediaPermission.Camera -> "camera"
          MediaPermission.Microphone -> "microphone"
        }
}
