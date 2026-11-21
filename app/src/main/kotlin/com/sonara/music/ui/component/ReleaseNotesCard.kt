/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonara.music.R
import com.sonara.music.ui.component.glass.LocalLiquidGlassEnabled
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaLiquidGlass
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.utils.Updater

@Composable
fun ReleaseNotesCard() {
    val releaseInfo = Updater.getCachedLatestRelease() ?: return
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.release_notes),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = releaseInfo.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }

    if (liquidGlassEnabled) {
        RegaLiquidGlass(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            borderColor = LocalRegaColors.current.glassBorder,
            style = RegaGlassStyle.preset(
                material = RegaGlassMaterial.Clear,
                tint = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                borderRadius = 20.dp,
                elevation = 4.dp,
            ),
            contentAlignment = Alignment.TopStart,
        ) {
            cardContent()
        }
    } else {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            )
        ) {
            cardContent()
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}
