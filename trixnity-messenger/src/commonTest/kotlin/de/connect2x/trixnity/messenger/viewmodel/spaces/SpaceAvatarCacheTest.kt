package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.MatrixClientConfiguration
import de.connect2x.trixnity.client.MediaStoreModule
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.media.MediaStore
import de.connect2x.trixnity.client.media.inMemory
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.search.user.homeserver.getImage
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SpaceAvatarCacheTest {
    @Test
    fun `cached thumbnail returns without waiting for cache lifetime`() = runTest {
        val mediaModule = MediaStoreModule.inMemory().create()
        val di =
            koinApplication {
                    modules(
                        mediaModule,
                        module {
                            single<CoroutineScope> { backgroundScope }
                            single { MatrixClientConfiguration() }
                            single<Clock> { Clock.System }
                        },
                    )
                }
                .koin
        val store = di.get<MediaStore>()
        val url = "mxc://example.org/avatar"
        val bytes = byteArrayOf(1, 2, 3, 4)
        store.addMedia(url, flowOf(bytes))
        val thumbnail = checkNotNull(store.getMedia(url))
        val configuration = MatrixMessengerConfiguration()
        val media = mock<MediaService>()
        everySuspend {
            media.getThumbnail(
                url,
                any(),
                any(),
                configuration.downloadLimits.thumbnail,
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(thumbnail)
        val client = mock<MatrixClient>()
        every { client.di } returns koinApplication { modules(module { single { media } }) }.koin
        repeat(2) {
            assertContentEquals(
                bytes,
                withTimeout(1.seconds) {
                    flow {
                            coroutineScope {
                                emitAll(getImage(url, client, configuration.downloadLimits.thumbnail, this))
                            }
                        }
                        .first { it != null }
                },
            )
        }
    }
}
