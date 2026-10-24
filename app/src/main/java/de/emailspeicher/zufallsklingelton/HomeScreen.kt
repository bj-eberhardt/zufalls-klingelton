package de.emailspeicher.zufallsklingelton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    tracks: List<Track>, active: Boolean, mode: SwitchMode, currentTrackId: String?, previousRingtoneName: String?,
    playingTrackId: String?, previewErrorTrackId: String?, importError: String?, onToggleActive: (Boolean) -> Unit,
    onModeSelected: (SwitchMode) -> Unit, onAdd: () -> Unit, onDelete: (String) -> Unit, onClear: () -> Unit,
    onPreview: (Track) -> Unit,
) {
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.home_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (importError != null) Text(importError, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.feature_title), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text(if (active) stringResource(R.string.feature_active) else stringResource(R.string.feature_inactive), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (active && previousRingtoneName != null) Text(stringResource(R.string.previous_ringtone_hint, previousRingtoneName), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Switch(checked = active, onCheckedChange = onToggleActive, enabled = tracks.isNotEmpty(), modifier = Modifier.testTag("activation_switch"))
                        }
                        if (tracks.isEmpty()) Text(stringResource(R.string.activation_needs_track), color = MaterialTheme.colorScheme.error)
                        SwitchOption(stringResource(R.string.mode_daily), mode == SwitchMode.DAILY, active) { onModeSelected(SwitchMode.DAILY) }
                        SwitchOption(stringResource(R.string.mode_after_call), mode == SwitchMode.AFTER_INCOMING_CALL, active) { onModeSelected(SwitchMode.AFTER_INCOMING_CALL) }
                    }
                }
            }
            if (tracks.isEmpty()) item { EmptyPoolCard(onAdd) } else {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.pool_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = onAdd) { Text(stringResource(R.string.add_track_short)) }
                        TextButton(onClick = onClear) { Text(stringResource(R.string.clear_tracks_button)) }
                    }
                }
                items(tracks, key = { it.id }) { track -> TrackRow(track, active && currentTrackId == track.id, playingTrackId == track.id, previewErrorTrackId == track.id, onPreview, onDelete) }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SwitchOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .45f))
    }
}

@Composable
private fun EmptyPoolCard(onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🎶", fontSize = 48.sp)
            Text(stringResource(R.string.empty_pool_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(R.string.empty_pool_message), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAdd, modifier = Modifier.testTag("add_track_button"), shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.add_track_button)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(track: Track, current: Boolean, playing: Boolean, error: Boolean, onPreview: (Track) -> Unit, onDelete: (String) -> Unit) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(track.title) } }, state = tooltipState,
    ) {
        Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { scope.launch { tooltipState.show() } }), border = if (current) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("♫", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(listOf(track.artist.ifBlank { stringResource(R.string.unknown_artist) }, formatDuration(track.durationMs)).joinToString(" • "), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (current) Text(stringResource(R.string.current_ringtone), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    if (error) Text(stringResource(R.string.preview_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { onPreview(track) }) { Icon(if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, stringResource(if (playing) R.string.preview_stop else R.string.preview_start)) }
                IconButton(onClick = { onDelete(track.id) }) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.delete_track)) }
            }
        }
    }
}
