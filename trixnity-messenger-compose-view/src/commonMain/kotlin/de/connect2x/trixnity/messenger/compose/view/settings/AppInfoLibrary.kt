package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.layout.LibraryScaffoldLayout
import com.mikepenz.aboutlibraries.ui.compose.m3.component.LibraryChip
import com.mikepenz.aboutlibraries.ui.compose.util.author
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.LibraryStyle

@Composable
internal fun LibraryItem(
    library: Library,
    modifier: Modifier = Modifier.Companion,
    style: LibraryStyle = MaterialTheme.components.library,
) {
    LibraryScaffoldLayout(
        modifier = modifier,
        libraryPadding = style.padding,
        name = {
            Text(
                text = library.name,
                style = style.textStyles.nameTextStyle ?: style.typography.titleLarge,
                color = style.colors.contentColor,
                maxLines = style.textStyles.nameMaxLines,
                overflow = style.textStyles.nameOverflow,
            )
        },
        version = {
            library.artifactVersion?.let { version ->
                Text(
                    modifier = Modifier.padding(style.padding.versionPadding.contentPadding),
                    text = version,
                    style = style.textStyles.versionTextStyle ?: style.typography.bodyMedium,
                    maxLines = style.textStyles.versionMaxLines,
                    textAlign = TextAlign.Center,
                    overflow = style.textStyles.defaultOverflow,
                    color = style.colors.contentColor,
                )
            }
        },
        author = {
            if (library.author.isNotBlank()) {
                Text(
                    text = library.author,
                    style = style.textStyles.authorTextStyle ?: style.typography.bodyMedium,
                    color = style.colors.contentColor,
                    maxLines = style.textStyles.authorMaxLines,
                    overflow = style.textStyles.defaultOverflow,
                )
            }
        },
        description = {},
        licenses = {
            for (license in library.licenses) {
                LibraryChip(
                    modifier = Modifier.padding(style.padding.licensePadding.containerPadding),
                    minHeight = style.dimensions.chipMinHeight,
                    containerColor = style.colors.licenseChipColors.containerColor,
                    contentColor = style.colors.licenseChipColors.contentColor,
                    shape = style.shapes.chipShape,
                ) {
                    Text(
                        modifier = Modifier.padding(style.padding.licensePadding.contentPadding),
                        maxLines = 1,
                        text = license.name,
                        style = style.textStyles.licensesTextStyle ?: LocalTextStyle.current,
                        textAlign = TextAlign.Center,
                        overflow = style.textStyles.defaultOverflow,
                    )
                }
            }
        },
        actions = {}
    )
}
