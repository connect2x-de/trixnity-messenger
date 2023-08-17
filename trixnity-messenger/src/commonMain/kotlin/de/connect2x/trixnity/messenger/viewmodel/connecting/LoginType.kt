package de.connect2x.trixnity.messenger.viewmodel.connecting

sealed interface LoginType {
    data class Password(val serverUrl: String) : LoginType
    data class SSO(val serverUrl: String, val id: String, val name: String) : LoginType
}