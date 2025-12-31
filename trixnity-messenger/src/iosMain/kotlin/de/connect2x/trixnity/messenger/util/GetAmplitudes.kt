package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.util.GetAmplitudes
import net.folivo.trixnity.client.media.PlatformMedia

class GetAmplitudesImpl : GetAmplitudes {
    override suspend fun invoke(
        media: PlatformMedia,
        count: Int?,
        deleteFile: Boolean,
        normalizeWithMax: Boolean
    ): Result<MutableList<Float>> =
        Result.success(MutableList(count!!) { 0f })
}
