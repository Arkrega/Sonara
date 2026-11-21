/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.component.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer

@Stable
private class PrismalToKyantBackdrop(
    private val source: PrismalGlassLayer,
) : Backdrop {
    override val isCoordinatesDependent: Boolean = source.isCoordinatesDependent

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        source.readSamplingState()
        with(source) {
            drawPrismalGlass(density, coordinates, layerBlock)
        }
    }
}

@Stable
class RegaBackdropState internal constructor(
    val enabled: Boolean,
    val prismalBackdrop: PrismalGlassLayer,
    val kyantBackdrop: Backdrop,
)

val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

val LocalRegaBackdrop = staticCompositionLocalOf<RegaBackdropState?> { null }

@Composable
fun rememberRegaBackdropState(
    enabled: Boolean = LocalLiquidGlassEnabled.current,
): RegaBackdropState {
    val prismalLayer = rememberPrismalGlassLayer()
    val kyantBridge = remember(prismalLayer) { PrismalToKyantBackdrop(prismalLayer) }
    return remember(enabled, prismalLayer, kyantBridge) {
        RegaBackdropState(
            enabled = enabled,
            prismalBackdrop = prismalLayer,
            kyantBackdrop = kyantBridge,
        )
    }
}

fun Modifier.regaBackdropSource(state: RegaBackdropState?): Modifier =
    if (state != null && state.enabled) {
        this.prismalGlassLayer(state.prismalBackdrop)
    } else {
        this
    }

typealias SonaraBackdropState = RegaBackdropState
val LocalSonaraBackdrop = LocalRegaBackdrop

@Composable
fun rememberSonaraBackdropState(
    enabled: Boolean = LocalLiquidGlassEnabled.current,
): RegaBackdropState = rememberRegaBackdropState(enabled = enabled)

fun Modifier.sonaraBackdropSource(state: RegaBackdropState?): Modifier = regaBackdropSource(state)
