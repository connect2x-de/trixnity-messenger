package de.connect2x.trixnity.messenger.viewmodel.mock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.clientserverapi.client.OlmKeysChange
import net.folivo.trixnity.clientserverapi.client.SyncApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.model.sync.Sync
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.EventContent
import net.folivo.trixnity.core.model.events.m.Presence
import org.kodein.mock.Mocker
import kotlin.coroutines.SuspendFunction1
import kotlin.reflect.KClass

internal class MockSyncApiClient(
    private val mocker: Mocker,
) : SyncApiClient {
    public override val currentSyncState: StateFlow<SyncState>
        get() = this.mocker.register(this, "get:currentSyncState")

    public override suspend fun cancel(wait: Boolean): Unit = this.mocker.registerSuspend(
        this,
        "cancel(kotlin.Boolean)", wait
    )

    public override suspend fun emitEvent(event: Event<*>): Unit = this.mocker.registerSuspend(
        this,
        "emitEvent(net.folivo.trixnity.core.model.events.Event)", event
    )

    public override suspend fun start(
        filter: String?,
        setPresence: Presence?,
        currentBatchToken: MutableStateFlow<String?>,
        timeout: Long,
        withTransaction: suspend (block: suspend () -> Unit) -> Unit,
        asUserId: UserId?,
        wait: Boolean,
        scope: CoroutineScope
    ): Unit = this.mocker.registerSuspend(
        this,
        "start(kotlin.String, net.folivo.trixnity.core.model.events.m.Presence, kotlinx.coroutines.flow.MutableStateFlow, kotlin.Long, net.folivo.trixnity.core.model.UserId, kotlin.Boolean, kotlinx.coroutines.CoroutineScope)",
        filter, setPresence, currentBatchToken, timeout, withTransaction, asUserId, wait, scope
    )

    public override suspend fun <T> startOnce(
        filter: String?,
        setPresence: Presence?,
        currentBatchToken: MutableStateFlow<String?>,
        timeout: Long,
        withTransaction: suspend (block: suspend () -> Unit) -> Unit,
        asUserId: UserId?,
        runOnce: suspend (Sync.Response) -> T
    ): Result<T> = this.mocker.registerSuspend(
        this,
        "startOnce(kotlin.String, net.folivo.trixnity.core.model.events.m.Presence, kotlinx.coroutines.flow.MutableStateFlow, kotlin.Long, net.folivo.trixnity.core.model.UserId)",
        filter, setPresence, currentBatchToken, timeout, withTransaction, asUserId, runOnce
    )

    public override suspend fun stop(wait: Boolean): Unit = this.mocker.registerSuspend(
        this,
        "stop(kotlin.Boolean)", wait
    )

    public override fun <T : EventContent> subscribe(
        clazz: KClass<T>,
        subscriber: SuspendFunction1<Event<T>, Unit>
    ): Unit = this.mocker.register(
        this,
        "subscribe(kotlin.reflect.KClass, kotlin.coroutines.SuspendFunction1)", clazz, subscriber
    )


    public override fun subscribeAfterSyncProcessing(subscriber: SuspendFunction1<Sync.Response, Unit>):
            Unit = this.mocker.register(
        this,
        "subscribeAfterSyncProcessing(kotlin.coroutines.SuspendFunction1)", subscriber
    )

    public override fun subscribeAllEvents(subscriber: SuspendFunction1<Event<EventContent>, Unit>):
            Unit = this.mocker.register(
        this, "subscribeAllEvents(kotlin.coroutines.SuspendFunction1)",
        subscriber
    )

    public override
    fun subscribeDeviceLists(subscriber: SuspendFunction1<Sync.Response.DeviceLists?, Unit>): Unit =
        this.mocker.register(
            this, "subscribeDeviceLists(kotlin.coroutines.SuspendFunction1)",
            subscriber
        )

    public override
    fun subscribeOlmKeysChange(subscriber: SuspendFunction1<OlmKeysChange, Unit>): Unit =
        this.mocker.register(
            this,
            "subscribeOlmKeysChange(kotlin.coroutines.SuspendFunction1)"
        )

    public override fun subscribeSyncResponse(subscriber: SuspendFunction1<Sync.Response, Unit>): Unit =
        this.mocker.register(
            this, "subscribeSyncResponse(kotlin.coroutines.SuspendFunction1)",
            subscriber
        )

    public override suspend fun sync(
        filter: String?,
        since: String?,
        fullState: Boolean,
        setPresence: Presence?,
        timeout: Long,
        asUserId: UserId?,
    ): Result<Sync.Response> = this.mocker.registerSuspend(
        this,
        "sync(kotlin.String, kotlin.String, kotlin.Boolean, net.folivo.trixnity.core.model.events.m.Presence, kotlin.Long, net.folivo.trixnity.core.model.UserId)",
        filter, since, fullState, setPresence, timeout, asUserId
    )

    public override fun toString(): String = this.mocker.register(this, "toString()", default = {
        super.toString()
    })

    public override fun <T : EventContent> unsubscribe(
        clazz: KClass<T>,
        subscriber: SuspendFunction1<Event<T>, Unit>
    ): Unit = this.mocker.register(
        this,
        "unsubscribe(kotlin.reflect.KClass, kotlin.coroutines.SuspendFunction1)", clazz, subscriber
    )

    public override
    fun unsubscribeAfterSyncProcessing(subscriber: SuspendFunction1<Sync.Response, Unit>): Unit =
        this.mocker.register(
            this, "unsubscribeAfterSyncProcessing(kotlin.coroutines.SuspendFunction1)",
            subscriber
        )

    public override fun unsubscribeAllEvents(subscriber: SuspendFunction1<Event<EventContent>, Unit>):
            Unit = this.mocker.register(
        this, "unsubscribeAllEvents(kotlin.coroutines.SuspendFunction1)",
        subscriber
    )

    public override
    fun unsubscribeDeviceLists(subscriber: SuspendFunction1<Sync.Response.DeviceLists?, Unit>):
            Unit = this.mocker.register(
        this,
        "unsubscribeDeviceLists(kotlin.coroutines.SuspendFunction1)", subscriber
    )

    override fun unsubscribeOlmKeysChange(subscriber: SuspendFunction1<OlmKeysChange, Unit>):
            Unit = this.mocker.register(
        this,
        "unsubscribeOlmKeysChange(kotlin.coroutines.SuspendFunction1)", subscriber
    )

    public override fun unsubscribeSyncResponse(subscriber: SuspendFunction1<Sync.Response, Unit>):
            Unit = this.mocker.register(
        this,
        "unsubscribeSyncResponse(kotlin.coroutines.SuspendFunction1)", subscriber
    )
}
