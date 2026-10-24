package de.emailspeicher.zufallsklingelton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TrackSelectionScreen(
    tracks: List<Track>, selectedIds: Set<String>, error: String?, onBack: () -> Unit, onQuery: (String) -> Unit,
    onToggle: (String) -> Unit, onConfirm: () -> Unit, playingTrackId: String? = null,
    previewErrorTrackId: String? = null, onPreview: (Track) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(300)
        onQuery(query)
    }
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back_button)) }
                Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.library_help), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 14.dp))
            TextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.search_label)) })
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(tracks, key = { it.id }) { track ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = track.id in selectedIds, onCheckedChange = { onToggle(track.id) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
                            Text(track.artist.ifBlank { stringResource(R.string.unknown_artist) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (previewErrorTrackId == track.id) Text(stringResource(R.string.preview_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(formatDuration(track.durationMs), style = MaterialTheme.typography.bodySmall)
                        val playing = playingTrackId == track.id
                        IconButton(onClick = { onPreview(track) }) { Icon(if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, stringResource(if (playing) R.string.preview_stop else R.string.preview_start)) }
                    }
                    HorizontalDivider()
                }
            }
            Button(onClick = onConfirm, enabled = selectedIds.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.add_selected_button, selectedIds.size))
            }
        }
    }
}
