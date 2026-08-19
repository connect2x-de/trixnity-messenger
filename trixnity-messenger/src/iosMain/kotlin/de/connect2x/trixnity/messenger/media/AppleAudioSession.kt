package de.connect2x.trixnity.messenger.media

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.setActive
import platform.Foundation.NSError

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun configureSession(): Result<Unit> = memScoped {
    val session = AVAudioSession.sharedInstance()
    val categoryError = alloc<ObjCObjectVar<NSError?>>()
    session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = categoryError.ptr)
    categoryError.value?.let {
        return@memScoped Result.failure(Throwable("AudioRecorder setCategory error: ${it.localizedDescription}"))
    }
    val activeError = alloc<ObjCObjectVar<NSError?>>()
    session.setActive(true, error = activeError.ptr)
    activeError.value?.let {
        return@memScoped Result.failure(Throwable("AudioRecorder setActive error: ${it.localizedDescription}"))
    }
    Result.success(Unit)
}
