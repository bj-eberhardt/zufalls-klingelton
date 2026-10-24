package de.emailspeicher.zufallsklingelton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SetupScreen(status: PermissionStatus, onRequestPermissions: () -> Unit, onStart: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("setup_card"),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("🎵", fontSize = 48.sp)
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PermissionRow(stringResource(R.string.permission_audio_title), stringResource(R.string.permission_audio_description), status.audioLibraryGranted)
                PermissionRow(stringResource(R.string.permission_phone_title), stringResource(R.string.permission_phone_description), status.phoneStateGranted)
                PermissionRow(stringResource(R.string.permission_ringtone_title), stringResource(R.string.permission_ringtone_description), status.ringtoneChangeGranted)
                if (!status.allGranted) Text(stringResource(R.string.setup_missing_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRequestPermissions, enabled = !status.allGranted, modifier = Modifier.fillMaxWidth().testTag("request_permissions_button"), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Text(stringResource(R.string.setup_permissions_button))
                }
                Button(onClick = onStart, enabled = status.allGranted, modifier = Modifier.fillMaxWidth().testTag("start_button"), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Text(stringResource(R.string.start_button))
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, description: String, granted: Boolean) {
    val statusText = if (granted) stringResource(R.string.permission_granted) else stringResource(R.string.permission_missing)
    val statusColor = if (granted) Color(0xFF7CFFB2) else MaterialTheme.colorScheme.error
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(if (granted) "✓" else "!", color = statusColor, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(statusText, color = statusColor, fontWeight = FontWeight.SemiBold)
    }
}
