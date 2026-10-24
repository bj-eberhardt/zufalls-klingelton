package de.emailspeicher.zufallsklingelton

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

data class HomeUiState(
    val tracks: List<Track> = emptyList(),
    val active: Boolean = false,
    val mode: SwitchMode = SwitchMode.DAILY,
    val currentTrackId: String? = null,
    val previousRingtoneName: String? = null,
    val isLoading: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val coordinator = RingtoneCoordinator(application)
    private val playbackStore = PlaybackStore(application)
    private val _uiState = MutableStateFlow(HomeUiState())
    private val startupInitialized = AtomicBoolean(false)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        val initialize = startupInitialized.compareAndSet(false, true)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val state = withContext(Dispatchers.IO) {
                if (initialize) coordinator.onStartup()
                readState()
            }
            _uiState.value = state.copy(isLoading = false)
        }
    }

    fun setActive(enabled: Boolean) {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                coordinator.setActive(enabled)
                readState()
            }
            _uiState.value = state
        }
    }

    fun setMode(mode: SwitchMode) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { coordinator.setMode(mode) }
            _uiState.value = _uiState.value.copy(mode = mode)
        }
    }

    fun addTracks(tracks: List<Track>) {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                coordinator.addTracks(tracks)
                readState()
            }
            _uiState.value = state
        }
    }

    fun deleteTrack(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { coordinator.deleteTrack(id) }
            _uiState.value = withContext(Dispatchers.IO) { readState() }
        }
    }

    fun clearTracks() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { coordinator.clearTracks() }
            _uiState.value = withContext(Dispatchers.IO) { readState() }
        }
    }

    private fun readState(): HomeUiState {
        val active = playbackStore.active
        return HomeUiState(
            tracks = coordinator.pool(),
            active = active,
            mode = playbackStore.mode,
            currentTrackId = playbackStore.currentTrackId,
            previousRingtoneName = if (active) coordinator.previousRingtoneName() else null,
        )
    }
}
