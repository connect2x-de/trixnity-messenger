package de.connect2x.messenger.android

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.create
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class MatrixMultiMessengerService : Service() {
    private val log = KotlinLogging.logger { }
    private val coroutineScope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, exception ->
        log.error(exception) { "Exception in MatrixMultiMessengerService coroutine" }
    })

    private val binder = LocalBinder()
    private val _matrixMultiMessenger: MutableStateFlow<MatrixMultiMessenger?> = MutableStateFlow(null)
    val matrixMultiMessenger = _matrixMultiMessenger.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): MatrixMultiMessengerService = this@MatrixMultiMessengerService
    }

    override fun onCreate() {
        log.info { "Starting Messenger client" }
        if (_matrixMultiMessenger.value == null) {
            coroutineScope.launch {
                _matrixMultiMessenger.value = MatrixMultiMessenger.create(applicationContext) {
                    val service = ServiceLoader.load(ConfigurationProvider::class.java) // findFirst only API >= 34
                        .iterator().asSequence()
                        .firstOrNull()
                    log.debug { "service: $service" }
                    if (service != null) {
                        log.debug { "found ConfigurationProvider: $service" }
                        service.configuration()()
                    } else {
                        throw IllegalStateException("Cannot find configuration -> see README.md")
                    }
                    modulesFactories += ::initialSyncModule
                    modulesFactories += ::notificationModule
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        log.info { "Binding service" }
        return binder
    }

    override fun onDestroy() {
        log.info { "Destroying service" }
        coroutineScope.cancel()
        runBlocking {
            _matrixMultiMessenger.value?.apply {
                di.get<NotificationHandler>().close()
                di.get<NotificationHandlerProvider>().close()
                close()
            }

            _matrixMultiMessenger.value = null
        }
        super.onDestroy()
    }
}

class MatrixMessengerServiceConnection : ServiceConnection {
    private val log = KotlinLogging.logger { }
    private lateinit var coroutineScope: CoroutineScope

    private val _matrixMultiMessenger = MutableStateFlow<MatrixMultiMessenger?>(null)
    val matrixMultiMessenger = _matrixMultiMessenger.asStateFlow()

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        val binder = service as MatrixMultiMessengerService.LocalBinder
        log.info { "bind service" }
        coroutineScope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, exception ->
            log.error(exception) { "Exception in MatrixMessengerServiceConnection coroutine" }
        })
        coroutineScope.launch {
            binder.getService().matrixMultiMessenger.collect {
                log.debug { "matrixMultiMessenger found" }
                _matrixMultiMessenger.value = it
            }
        }.invokeOnCompletion { _matrixMultiMessenger.value = null }
    }

    override fun onServiceDisconnected(className: ComponentName) {
        coroutineScope.cancel()
    }

    fun bind(context: Context) {
        val intent = Intent(context, MatrixMultiMessengerService::class.java)
        log.debug { "start ${MatrixMultiMessengerService::class.simpleName}" }
        context.startService(intent)
        log.debug { "bind ${MatrixMultiMessengerService::class.simpleName}" }
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        log.debug { "unbind AndroidMatrixClientService" }
        context.unbindService(this)
    }
}

inline fun <T> withMatrixMessengerServiceConnection(
    context: Context,
    block: (connection: MatrixMessengerServiceConnection) -> T
): T {
    val connection = MatrixMessengerServiceConnection()
    return try {
        connection.bind(context)
        block(connection)
    } finally {
        connection.unbind(context)
    }
}

suspend inline fun <T> withMatrixMessengerService(
    context: Context,
    block: (matrixMultiMessenger: MatrixMultiMessenger) -> T
): T = withMatrixMessengerServiceConnection(context) {
    block(it.matrixMultiMessenger.filterNotNull().first())
}
