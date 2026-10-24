package de.emailspeicher.zufallsklingelton

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

fun Intent?.selectedUris(): List<Uri> {
    if (this == null) return emptyList()
    val result = mutableListOf<Uri>()
    data?.let(result::add)
    clipData?.let { clip -> for (index in 0 until clip.itemCount) result += clip.getItemAt(index).uri }
    return result.distinct()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSheet(onDismiss: () -> Unit, onLibrary: () -> Unit, onStorage: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.add_source_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.add_source_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onLibrary, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_from_library)) }
            OutlinedButton(onClick = onStorage, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_from_storage)) }
            Spacer(modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
fun ClearTracksDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_tracks_title)) },
        text = { Text(stringResource(R.string.clear_tracks_message, count)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.clear_tracks_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
