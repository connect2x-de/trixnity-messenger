package de.connect2x.trixnity.messenger.viewmodel.util

import net.folivo.trixnity.client.media.PlatformMedia

interface GetAmplitudes {
    /**
     * This function instruments the platform-available decoder for extracting the amplitudes from the specified media
     * file.
     *
     * @param media            the platform media, which is expected to be a media file
     * @param count            the expected count of amplitudes. When not matching, this function upsamples or
     *                         downsamples based on the count of amplitudes extracted.
     * @param deleteFile       delete the temporary file created when invoking this function. By default, we want to
     *                         preserve the file created.
     * @param normalizeWithMax by default, this function normalizes with the maximum amplitude value extracted from the
     *                         media. When turned off, the maximum short value is used for normalization.
     *
     * @return When successfully, a list of normalized amplitudes from 0.0 to 1.0
     */
    suspend operator fun invoke(
        media: PlatformMedia,
        count: Int? = 200,
        deleteFile: Boolean = false,
        normalizeWithMax: Boolean = true
    ): Result<MutableList<Float>>
}
