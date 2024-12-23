package de.connect2x.trixnity.messenger

import io.ktor.client.*
import io.ktor.client.engine.*

@TrixnityMessengerDsl
interface MatrixMessengerBaseConfiguration {
    var appName: String
    var appId: String
    var appVersion: String?
    var urlProtocol: String
    var urlHost: String
    var sendLogsEmailAddress: String?
    var privacyInfoUrl: String?
    var imprintUrl: String?
    var licenses: String?
    var pushUrl: String?

    var httpClientEngine: HttpClientEngine?
    var httpClientConfig: (HttpClientConfig<*>.() -> Unit)?

    fun copyTo(other: MatrixMessengerBaseConfiguration) {
        other.appVersion = appVersion
        other.appName = appName
        other.appId = appId
        other.urlProtocol = urlProtocol
        other.urlHost = urlHost
        other.sendLogsEmailAddress = sendLogsEmailAddress
        other.privacyInfoUrl = privacyInfoUrl
        other.imprintUrl = imprintUrl
        other.licenses = licenses
        other.pushUrl = pushUrl
        other.httpClientEngine = httpClientEngine
        other.httpClientConfig = httpClientConfig
    }
}
