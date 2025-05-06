package de.connect2x.trixnity.messenger

/**
 * A list of feature toggles for the Matrix Messenger.
 */
data class MatrixMessengerFeatures(
    /**
     * If true, the PDF reader details view in the timeline will be enabled.
     */
    var enablePdfReader: Boolean = true,
)
