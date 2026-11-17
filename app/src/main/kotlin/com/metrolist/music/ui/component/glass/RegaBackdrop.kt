/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Backdrop capture is no longer needed now that surfaces are drawn flat.
 * The state object and modifier are kept as cheap no-ops so existing call
 * sites keep compiling without recording the whole window every frame.
 */

package com.metrolist.music.ui.component.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

@Stable
class RegaBackdropState internal constructor()

@Composable
fun rememberRegaBackdropState(): RegaBackdropState = remember { RegaBackdropState() }

val LocalRegaBackdrop = staticCompositionLocalOf<RegaBackdropState?> { null }

fun Modifier.regaBackdropSource(state: RegaBackdropState?): Modifier = this

typealias SonaraBackdropState = RegaBackdropState
val LocalSonaraBackdrop = LocalRegaBackdrop

@Composable
fun rememberSonaraBackdropState(): RegaBackdropState = rememberRegaBackdropState()

fun Modifier.sonaraBackdropSource(state: RegaBackdropState?): Modifier = regaBackdropSource(state)
