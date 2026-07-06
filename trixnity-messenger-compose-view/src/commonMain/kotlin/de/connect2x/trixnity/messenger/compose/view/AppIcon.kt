package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import de.connect2x.trixnity.messenger.AppIcon
import de.connect2x.trixnity.messenger.ByteArrayAppIcon
import kotlin.reflect.KClass
import kotlin.reflect.safeCast
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.jetbrains.compose.resources.painterResource
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

data class DrawableResourceAppIcon(val resource: DrawableResource) : AppIcon {
    override suspend fun readBytes(): ByteArray = getDrawableResourceBytes(getSystemResourceEnvironment(), resource)
}

interface GetAppIconPainter<I : AppIcon> {
    val type: KClass<I>

    @Composable fun getPainter(icon: I): Painter

    @Composable
    fun getPainterOrNull(icon: AppIcon): Painter? {
        return type.safeCast(icon)?.let { castedIcon -> getPainter(castedIcon) }
    }
}

object GetDrawableResourceAppIconPainter : GetAppIconPainter<DrawableResourceAppIcon> {
    override val type: KClass<DrawableResourceAppIcon> = DrawableResourceAppIcon::class

    @Composable override fun getPainter(icon: DrawableResourceAppIcon): Painter = painterResource(icon.resource)
}

object GetByteArrayResourceAppIconPainter : GetAppIconPainter<ByteArrayAppIcon> {
    override val type: KClass<ByteArrayAppIcon> = ByteArrayAppIcon::class

    @Composable
    override fun getPainter(icon: ByteArrayAppIcon): Painter {
        val bitmap = remember(icon) { icon.data.decodeToImageBitmap() }
        return BitmapPainter(bitmap)
    }
}

@Composable
fun getAppIconPainter(icon: AppIcon): Painter {
    val painters: List<GetAppIconPainter<*>> = DI.current.getAll()
    for (getPainter in painters) {
        return getPainter.getPainterOrNull(icon) ?: continue
    }

    throw IllegalArgumentException("No painter registered for ${icon::class}")
}

inline fun <reified T : AppIcon> Module.appIconPainter(
    noinline definition: Scope.(ParametersHolder) -> GetAppIconPainter<T>
) = single<GetAppIconPainter<*>>(named<T>(), definition = definition)

fun appIconPainterModule() = module {
    appIconPainter { GetByteArrayResourceAppIconPainter }
    appIconPainter { GetDrawableResourceAppIconPainter }
}
