package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.clientserverapi.model.authentication.LoginType

sealed interface AddMatrixAccountMethod {
    val serverUrl: String

    data class Password(override val serverUrl: String) : AddMatrixAccountMethod
    data class SSO(
        override val serverUrl: String,
        val identityProvider: LoginType.SSO.IdentityProvider?,
        val icon: ByteArray?,
    ) : AddMatrixAccountMethod {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SSO

            if (serverUrl != other.serverUrl) return false
            if (identityProvider != other.identityProvider) return false
            if (icon != null) {
                if (other.icon == null) return false
                if (!icon.contentEquals(other.icon)) return false
            } else if (other.icon != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = serverUrl.hashCode()
            result = 31 * result + identityProvider.hashCode()
            result = 31 * result + (icon?.contentHashCode() ?: 0)
            return result
        }
    }

    data class Register(override val serverUrl: String) : AddMatrixAccountMethod
    data class OAuth2(override val serverUrl: String, val type: OAuth2LoginViewModel.Type) : AddMatrixAccountMethod
}
