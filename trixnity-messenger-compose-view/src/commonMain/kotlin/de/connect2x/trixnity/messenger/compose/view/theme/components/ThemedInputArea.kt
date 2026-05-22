package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

data class InputAreaStyle(
    val shape: Shape,
    val textStyle: TextStyle,
    val colors: TextFieldColors,
    val contentPadding: PaddingValues,
) {
    fun textColor(enabled: Boolean, isError: Boolean, focused: Boolean) =
        when {
            !enabled -> colors.disabledTextColor
            isError -> colors.errorTextColor
            focused -> colors.focusedTextColor
            else -> colors.unfocusedTextColor
        }

    fun containerColor(enabled: Boolean, isError: Boolean, focused: Boolean) =
        when {
            !enabled -> colors.disabledContainerColor
            isError -> colors.errorContainerColor
            focused -> colors.focusedContainerColor
            else -> colors.unfocusedContainerColor
        }

    fun placeholderColor(enabled: Boolean, isError: Boolean, focused: Boolean) =
        when {
            !enabled -> colors.disabledPlaceholderColor
            isError -> colors.errorPlaceholderColor
            focused -> colors.focusedPlaceholderColor
            else -> colors.unfocusedPlaceholderColor
        }

    companion object {
        @Composable
        fun default(
            shape: Shape = RoundedCornerShape(8.dp),
            textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
            colors: TextFieldColors =
                TextFieldDefaults.colors(
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    errorTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                    errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                ),
            contentPadding: PaddingValues = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
        ) = InputAreaStyle(shape = shape, textStyle = textStyle, colors = colors, contentPadding = contentPadding)
    }
}
