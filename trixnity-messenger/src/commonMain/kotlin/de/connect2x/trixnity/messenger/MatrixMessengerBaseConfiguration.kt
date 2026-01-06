package de.connect2x.trixnity.messenger

import io.ktor.client.*
import io.ktor.client.engine.*

@TrixnityMessengerDsl
interface MatrixMessengerBaseConfiguration {
    var appName: String
    var appId: String
    var appVersion: String?

    /**
     * The app uri used for callbacks and deep linking. Must be of form `app:` or `https://app.example.com`.
     *
     * It is related to the [oAuth2ClientUrl], because [appUri] is used to generate the redirect URIs.
     * Therefore, there are some limitations described in the Matrix spec.
     */
    var appUri: String

    /**
     * A URL to a valid web page that gives the user more information about the client. Must use https scheme.
     *
     * It is related to the [appUri], because [appUri] is used to generate the redirect URIs.
     * Therefore, there are some limitations described in the Matrix spec.
     */
    var oAuth2ClientUrl: String
    var sendLogsEmailAddress: String?

    /**
     * The privacy info of the application in a Markdown format
     */
    var privacyInfo: String?

    /**
     * The imprint of the application in a Markdown format
     */
    var imprint: String?
    var licenses: String?

    var httpClientEngine: HttpClientEngine?
    var httpClientConfig: (HttpClientConfig<*>.() -> Unit)?

    fun copyTo(other: MatrixMessengerBaseConfiguration) {
        other.appVersion = appVersion
        other.appName = appName
        other.appId = appId
        other.appUri = appUri
        other.oAuth2ClientUrl = oAuth2ClientUrl
        other.sendLogsEmailAddress = sendLogsEmailAddress
        other.privacyInfo = privacyInfo
        other.imprint = imprint
        other.licenses = licenses
        other.httpClientEngine = httpClientEngine
        other.httpClientConfig = httpClientConfig
    }
}
