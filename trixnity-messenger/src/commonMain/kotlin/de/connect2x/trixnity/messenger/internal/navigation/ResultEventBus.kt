package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

@TrixnityMessengerPrivateApi
interface ResultEventBus {
    fun getResult(key: Any?): Flow<Any?>

    fun sendResult(key: Any?, result: Any?)

    fun removeResult(key: Any?)
}

internal fun ResultEventBus(): ResultEventBus {
    return ResultEventBusImpl()
}

private class ResultEventBusImpl : ResultEventBus {

    private val channelMap: MutableStateFlow<Map<Any?, Channel<Any?>>> = MutableStateFlow(emptyMap())

    override fun getResult(key: Any?): Flow<Any?> {
        return getChannel(key).receiveAsFlow()
    }

    override fun sendResult(key: Any?, result: Any?) {
        getChannel(key).trySend(result)
    }

    override fun removeResult(key: Any?) {
        channelMap.update { it - key }
    }

    private fun getChannel(key: Any?): Channel<Any?> {
        val channels = channelMap.updateAndGet { channels ->
            val channel = channels[key] ?: Channel(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
            channels + (key to channel)
        }

        return checkNotNull(channels[key])
    }
}
