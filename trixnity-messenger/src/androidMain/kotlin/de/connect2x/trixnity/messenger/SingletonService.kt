package de.connect2x.trixnity.messenger

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

open class SingletonService<I : AutoCloseable>(
    val factory: suspend (Context) -> I,
) : Service() {

    private val log = Logger("de.connect2x.trixnity.messenger.SingletonService")
    private val coroutineScope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, exception ->
        log.error(exception) { "Exception in ${this::class.simpleName} coroutine" }
    })
    private var closeSelfJob: Job? = null

    private val binder = LocalBinder<I>()
    private val _instance: MutableStateFlow<I?> = MutableStateFlow(null)
    val instance = _instance.asStateFlow()

    inner class LocalBinder<T : AutoCloseable> : Binder() {
        @Suppress("UNCHECKED_CAST")
        fun getService(): SingletonService<T> = this@SingletonService as SingletonService<T>
    }

    override fun onCreate() {
        log.info { "Starting ${this::class.simpleName}" }
        if (_instance.value == null) {
            coroutineScope.launch {
                log.info { "create instance for ${this@SingletonService::class.simpleName}" }
                _instance.value = factory(applicationContext)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        log.info { "Binding service" }
        closeSelfJob?.cancel()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        closeSelfJob = coroutineScope.launch {
            delay(5.seconds)
            stopSelf()
        }
        return false
    }

    override fun onDestroy() {
        log.info { "Destroying service" }
        coroutineScope.cancel()
        _instance.value?.close()
        _instance.value = null
        super.onDestroy()
    }
}

open class SingletonServiceConnection<I : AutoCloseable, S : SingletonService<I>>(
    private val singletonServiceClass: Class<S>,
) : ServiceConnection {
    private val log = Logger("de.connect2x.trixnity.messenger.SingletonServiceConnection")
    private var coroutineScope: CoroutineScope? = null

    private val _instance = MutableStateFlow<I?>(null)
    val instance = _instance.asStateFlow()

    override fun onServiceConnected(className: ComponentName, rawBinder: IBinder) {
        @Suppress("UNCHECKED_CAST")
        val binder = rawBinder as SingletonService<I>.LocalBinder<I>
        log.debug { "bound ${singletonServiceClass.simpleName}" }
        coroutineScope?.cancel()
        coroutineScope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, exception ->
            log.error(exception) { "Exception in ${singletonServiceClass.simpleName} connection coroutine" }
        })
        coroutineScope?.launch {
            val service = binder.getService()
            service.instance.collect {
                log.debug { "instance found in ${singletonServiceClass.simpleName}" }
                _instance.value = it
            }
        }?.invokeOnCompletion { _instance.value = null }
    }

    override fun onServiceDisconnected(className: ComponentName) {
        coroutineScope?.cancel()
    }

    fun bind(context: Context) {
        val intent = Intent(context, singletonServiceClass)
        log.debug { "bind ${singletonServiceClass.simpleName}" }
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        log.debug { "unbind ${singletonServiceClass.simpleName}" }
        context.unbindService(this)
    }
}

private inline fun <T, reified I : AutoCloseable, reified S : SingletonService<I>> withSingletonServiceConnection(
    context: Context,
    block: (connection: SingletonServiceConnection<I, S>) -> T
): T {
    val connection = SingletonServiceConnection(S::class.java)
    connection.bind(context)
    return try {
        block(connection)
    } finally {
        connection.unbind(context)
    }
}

internal suspend inline fun <T, reified I : AutoCloseable, reified S : SingletonService<I>> withSingletonService(
    context: Context,
    block: suspend (instance: I) -> T
): T = withSingletonServiceConnection<T, I, S>(context) {
    block(it.instance.filterNotNull().first())
}

