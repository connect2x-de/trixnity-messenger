package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.mikepenz.aboutlibraries.ui.compose.LibraryColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.LibraryDimensions
import com.mikepenz.aboutlibraries.ui.compose.LibraryPadding
import com.mikepenz.aboutlibraries.ui.compose.LibraryShapes
import com.mikepenz.aboutlibraries.ui.compose.LibraryTextStyles
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors

data class LibraryStyle(
    val typography: Typography,
    val colors: LibraryColors,
    val padding: LibraryPadding,
    val dimensions: LibraryDimensions,
    val textStyles: LibraryTextStyles,
    val shapes: LibraryShapes,
) {
    companion object {
        @Composable
        fun default(
            typography: Typography = MaterialTheme.typography,
            colors: LibraryColors = LibraryDefaults.libraryColors(),
            padding: LibraryPadding = LibraryDefaults.libraryPadding(),
            dimensions: LibraryDimensions = LibraryDefaults.libraryDimensions(),
            textStyles: LibraryTextStyles = LibraryDefaults.libraryTextStyles(),
            shapes: LibraryShapes = LibraryDefaults.libraryShapes(),
        ) = LibraryStyle(
            typography,
            colors,
            padding,
            dimensions,
            textStyles,
            shapes,
        )
    }
}
