package de.connect2x.trixnity.messenger

@TrixnityMessengerDsl
interface MatrixMessengerBaseConfiguration {
    var appName: String
    var appId: String
    var urlProtocol: String
    var urlHost: String
    var sendLogsEmailAddress: String?
    var privacyInfoUrl: String?
    var imprintUrl: String?
    var licenses: String?
    var pushUrl: String?

    fun copyTo(other: MatrixMessengerBaseConfiguration) {
        other.appName = appName
        other.appId = appId
        other.urlProtocol = urlProtocol
        other.urlHost = urlHost
        other.sendLogsEmailAddress = sendLogsEmailAddress
        other.privacyInfoUrl = privacyInfoUrl
        other.imprintUrl = imprintUrl
        other.licenses = licenses
        other.pushUrl = pushUrl
    }
}
