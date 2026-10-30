package de.emailspeicher.zufallsklingelton

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import androidx.activity.viewModels
import android.widget.Toast
import android.util.Log
import de.emailspeicher.zufallsklingelton.ui.theme.ZufallsKlingeltonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppScreen { HOME, LIBRARY }

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val audioLibraryRepository by lazy { AudioLibraryRepository(this) }
    private var permissionStatus by mutableStateOf<PermissionStatus?>(null)
    private var showHome by mutableStateOf(false)
    private var screen by mutableStateOf(AppScreen.HOME)
    private var libraryTracks by mutableStateOf(emptyList<Track>())
    private var selectedLibraryIds by mutableStateOf(emptySet<String>())
    private var showSourceSheet by mutableStateOf(false)
    private var showClearDialog by mutableStateOf(false)
    private var selectionError by mutableStateOf<String?>(null)
    private var playingTrackId by mutableStateOf<String?>(null)
    private var previewErrorTrackId by mutableStateOf<String?>(null)
    private var previewPlayer: android.media.MediaPlayer? = null
    private var permissionFlowActive = false
    private var waitingForSettings = false

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionStatus = readPermissionStatus(this)
        if (permissionFlowActive && granted) requestNextPermission() else permissionFlowActive = false
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val selectedUris = if (result.resultCode == RESULT_OK) result.data.selectedUris() else emptyList()
        if (selectedUris.isNotEmpty()) {
            selectedUris.forEach { uri ->
                runCatching {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            addUris(selectedUris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionStatus()
        showHome = permissionStatus?.allGranted == true
        if (showHome) viewModel.refresh()
        setContent {
            ZufallsKlingeltonTheme {
                val homeState by viewModel.uiState.collectAsState()
                Surface(modifier = Modifier.fillMaxSize()) {
                    val status = permissionStatus
                    if (!showHome || status == null || !status.allGranted) {
                        SetupScreen(
                            status = status ?: PermissionStatus(false, false, false),
                            onRequestPermissions = ::requestNextPermission,
                            onStart = { showHome = true; viewModel.refresh() },
                        )
                    } else if (screen == AppScreen.LIBRARY) {
                        TrackSelectionScreen(
                            tracks = libraryTracks,
                            selectedIds = selectedLibraryIds,
                            error = selectionError,
                            playingTrackId = playingTrackId,
                            previewErrorTrackId = previewErrorTrackId,
                            onBack = { stopPreview(); screen = AppScreen.HOME; selectionError = null },
                            onQuery = ::loadLibraryTracks,
                            onToggle = { id -> selectedLibraryIds = selectedLibraryIds.toggle(id) },
                            onConfirm = ::addSelectedLibraryTracks,
                            onPreview = ::togglePreview,
                        )
                    } else {
                        HomeScreen(
                            tracks = homeState.tracks,
                            active = homeState.active,
                            mode = homeState.mode,
                            currentTrackId = homeState.currentTrackId,
                            previousRingtoneName = homeState.previousRingtoneName,
                            playingTrackId = playingTrackId,
                            previewErrorTrackId = previewErrorTrackId,
                            importError = selectionError,
                            onToggleActive = ::toggleActive,
                            onModeSelected = ::selectMode,
                            onAdd = { showSourceSheet = true },
                            onDelete = ::deleteTrack,
                            onClear = { showClearDialog = true },
                            onPreview = ::togglePreview,
                        )
                    }
                }
                if (showSourceSheet) SourceSheet(
                    onDismiss = { showSourceSheet = false },
                    onLibrary = {
                        showSourceSheet = false
                        stopPreview()
                        screen = AppScreen.LIBRARY
                        selectedLibraryIds = emptySet()
                        loadLibraryTracks("")
                    },
                    onStorage = {
                        showSourceSheet = false
                        launchFilePicker()
                    },
                )
                if (showClearDialog) ClearTracksDialog(
                    count = homeState.tracks.size,
                    onDismiss = { showClearDialog = false },
                    onConfirm = {
                        stopPreview()
                        viewModel.clearTracks()
                        showClearDialog = false
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForSettings) {
            waitingForSettings = false
            refreshPermissionStatus()
            permissionFlowActive = false
        } else if (permissionStatus != null) {
            refreshPermissionStatus()
            if (permissionStatus?.allGranted != true) showHome = false
        }
        if (showHome) viewModel.refresh()
    }

    override fun onDestroy() {
        stopPreview()
        super.onDestroy()
    }

    override fun onStop() {
        stopPreview()
        super.onStop()
    }

    private fun toggleActive(enabled: Boolean) {
        stopPreview()
        val restoredName = if (!enabled) viewModel.uiState.value.previousRingtoneName else null
        viewModel.setActive(enabled)
        if (restoredName != null) {
            Toast.makeText(this, getString(R.string.previous_ringtone_restored, restoredName), Toast.LENGTH_LONG).show()
        }
    }

    private fun selectMode(newMode: SwitchMode) {
        viewModel.setMode(newMode)
    }

    private fun deleteTrack(id: String) {
        stopPreview()
        viewModel.deleteTrack(id)
    }

    private fun preview(track: Track) {
        previewPlayer?.release()
        previewErrorTrackId = null
        playingTrackId = track.id
        previewPlayer = RingtoneController(this).preview(
            uri = Uri.parse(track.uri),
            onError = { playingTrackId = null; previewErrorTrackId = track.id },
            onFinished = { playingTrackId = null },
        )
    }

    private fun togglePreview(track: Track) {
        if (playingTrackId == track.id) {
            stopPreview()
        } else {
            preview(track)
        }
    }

    private fun stopPreview() {
        previewPlayer?.release()
        previewPlayer = null
        playingTrackId = null
    }

    private fun loadLibraryTracks(query: String) {
        lifecycleScope.launch {
            libraryTracks = withContext(Dispatchers.IO) { audioLibraryRepository.queryTracks(query) }
        }
    }

    private fun addSelectedLibraryTracks() {
        stopPreview()
        viewModel.addTracks(libraryTracks.filter { it.id in selectedLibraryIds })
        screen = AppScreen.HOME
        selectedLibraryIds = emptySet()
        selectionError = null
    }

    private fun addUris(uris: List<Uri>) {
        Log.i("MainActivity", "Adding ${uris.size} selected URI(s) to the track pool")
        lifecycleScope.launch {
            val imported = withContext(Dispatchers.IO) { uris.mapNotNull(audioLibraryRepository::readTrack) }
            Log.i("MainActivity", "Imported ${imported.size}/${uris.size} selected URI(s)")
            if (imported.isEmpty()) {
                Log.e("MainActivity", "No selected URI could be imported")
                selectionError = getString(R.string.import_error)
            } else {
                viewModel.addTracks(imported)
                screen = AppScreen.HOME
                selectionError = null
            }
        }
    }

    private fun launchFilePicker() {
        Log.d("MainActivity", "Launching audio file picker")
        filePickerLauncher.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            },
        )
    }

    private fun requestNextPermission() {
        permissionFlowActive = true
        val status = readPermissionStatus(this)
        permissionStatus = status
        when {
            !status.audioLibraryGranted -> runtimePermissionLauncher.launch(audioPermission())
            !status.mediaStoreWriteGranted -> runtimePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            !status.phoneStateGranted -> runtimePermissionLauncher.launch(android.Manifest.permission.READ_PHONE_STATE)
            !status.ringtoneChangeGranted -> {
                waitingForSettings = true
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            }
            else -> permissionFlowActive = false
        }
    }

    private fun refreshPermissionStatus() {
        permissionStatus = readPermissionStatus(this)
    }
}

private fun Set<String>.toggle(value: String): Set<String> = toMutableSet().apply { if (!add(value)) remove(value) }

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    ZufallsKlingeltonTheme {
        HomeScreen(emptyList(), false, SwitchMode.DAILY, null, null, null, null, null, {}, {}, {}, {}, {}, {})
    }
}
