package de.connect2x.trixnity.messenger.viewmodel.util

import net.folivo.trixnity.client.key.DeviceTrustLevel

val DeviceTrustLevel.isVerified: Boolean
    get() = this is DeviceTrustLevel.CrossSigned && this.verified
            || this is DeviceTrustLevel.Valid && this.verified