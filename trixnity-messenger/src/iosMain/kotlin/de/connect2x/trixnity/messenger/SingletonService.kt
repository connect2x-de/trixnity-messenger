package de.connect2x.trixnity.messenger

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class SingletonService<I : AutoCloseable> : AutoCloseable {

    private val mutex = Mutex()
    private var instance: I? = null

    abstract suspend fun factory(): I

    suspend fun init(): I = mutex.withLock {
        val currentInstance = instance
        if (currentInstance == null) {
            val newInstance = factory()
            instance = newInstance
            newInstance
        } else currentInstance
    }

    fun get(): I? = instance

    override fun close() {
        instance?.close()
        instance = null
    }
}

