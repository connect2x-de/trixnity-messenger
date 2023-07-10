package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

object SizeComputations {
    fun getHeight(height: Int, maxHeight: Int, width: Int, maxWidth: Float): Int {
        val imageFitsBounds = height <= maxHeight && width <= maxWidth
        if (imageFitsBounds) {
            return height
        }
        val imageIsOnlyHigher = height > maxHeight && width <= maxWidth
        if (imageIsOnlyHigher) {
            return maxHeight
        }
        val imageIsOnlyWider = height <= maxHeight && width > maxWidth
        if (imageIsOnlyWider) {
            return (height / (width.toFloat() / maxWidth)).toInt()
        }
        // imageIsWiderAndHigher
        val newHeight = (height / (width.toFloat() / maxWidth)).toInt()
        return if (newHeight <= maxHeight) {
            newHeight
        } else {
            maxHeight
        }
    }

    fun getWidth(height: Int, maxHeight: Float, width: Int, maxWidth: Float): Int {
        val imageFitsBounds = height <= maxHeight && width <= maxWidth
        if (imageFitsBounds) {
            return width
        }
        val imageIsOnlyWider = height <= maxHeight && width > maxWidth
        if (imageIsOnlyWider) {
            return maxWidth.toInt()
        }
        val imageIsOnlyHigher = height > maxHeight && width <= maxWidth
        if (imageIsOnlyHigher) {
            return (width / (height.toFloat() / maxHeight)).toInt()
        }
        // imageIsWiderAndHigher
        val newWidth = (width / (height.toFloat() / maxHeight)).toInt()
        return if (newWidth <= maxWidth) {
            newWidth
        } else {
            maxWidth.toInt()
        }
    }
}