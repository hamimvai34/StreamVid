package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChannelEntity
import com.example.data.local.VideoEntity
import com.example.data.remote.YouTubeFetchMethod
import com.example.data.repository.StreamRepository
import com.example.service.BackgroundMediaService
import com.example.service.BackgroundPlaybackBridge
import com.example.service.MediaServiceCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BottomNavTab {
    HOME,
    CHANNELS,
    AUDIO_PLAYER,
    ADMIN
}

data class PlaybackState(
    val activeVideo: VideoEntity? = null,
    val isAudioOnly: Boolean = false,
    val isExpanded: Boolean = false,
    val isPlaying: Boolean = false,
    val isBackgroundPlayEnabled: Boolean = true,
    val currentSec: Float = 0f,
    val durationSec: Float = 1f,
    val playbackSpeed: Float = 1.0f,
    val pendingSeekSec: Float? = null
)

data class StreamUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val allWhitelistedVideos: List<VideoEntity> = emptyList(),
    val filteredVideos: List<VideoEntity> = emptyList(),
    val selectedChannelId: String? = null,
    val searchQuery: String = "",
    val isDarkTheme: Boolean = true,
    val isAdminAuthenticated: Boolean = false,
    val currentTab: BottomNavTab = BottomNavTab.HOME,
    val selectedFetchMethod: YouTubeFetchMethod = YouTubeFetchMethod.AUTO_CASCADE,
    val lastFetchEngineReport: String = "Ready • Auto Multi-Engine Cascade active",
    val isSyncing: Boolean = false,
    val statusBannerMessage: String? = null
)

class StreamViewModel(
    private val appContext: Context,
    private val repository: StreamRepository
) : ViewModel() {

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isDarkTheme = MutableStateFlow(true)
    private val _isAdminAuthenticated = MutableStateFlow(false)
    private val _currentTab = MutableStateFlow(BottomNavTab.HOME)
    private val _selectedFetchMethod = MutableStateFlow(YouTubeFetchMethod.AUTO_CASCADE)
    private val _lastFetchEngineReport = MutableStateFlow("Ready • Auto Multi-Engine Cascade active")
    private val _isSyncing = MutableStateFlow(false)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val uiState: StateFlow<StreamUiState> = combine(
        repository.channelsFlow,
        repository.whitelistedVideosFlow,
        _selectedChannelId,
        _searchQuery,
        combine(
            _isDarkTheme,
            _isAdminAuthenticated,
            _currentTab,
            _isSyncing,
            combine(_selectedFetchMethod, _lastFetchEngineReport, _statusBannerMessage) { m, r, s ->
                Triple(m, r, s)
            }
        ) { dark, admin, tab, syncing, fetchTriple ->
            AdminAndThemeMeta(
                isDarkTheme = dark,
                isAdminAuthenticated = admin,
                currentTab = tab,
                isSyncing = syncing,
                fetchMethod = fetchTriple.first,
                fetchReport = fetchTriple.second,
                statusBannerMessage = fetchTriple.third
            )
        }
    ) { channels, videos, selectedChannelId, query, meta ->
        val validChannelId = if (selectedChannelId != null && channels.any { it.id == selectedChannelId }) {
            selectedChannelId
        } else {
            null
        }

        val filtered = videos.filter { video ->
            val matchesChannel = validChannelId == null || video.channelId == validChannelId
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                video.title.lowercase().contains(q) ||
                video.channelTitle.lowercase().contains(q) ||
                video.description.lowercase().contains(q)
            matchesChannel && matchesQuery
        }

        StreamUiState(
            channels = channels,
            allWhitelistedVideos = videos,
            filteredVideos = filtered,
            selectedChannelId = validChannelId,
            searchQuery = query,
            isDarkTheme = meta.isDarkTheme,
            isAdminAuthenticated = meta.isAdminAuthenticated,
            currentTab = meta.currentTab,
            selectedFetchMethod = meta.fetchMethod,
            lastFetchEngineReport = meta.fetchReport,
            isSyncing = meta.isSyncing,
            statusBannerMessage = meta.statusBannerMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StreamUiState()
    )

    private data class AdminAndThemeMeta(
        val isDarkTheme: Boolean,
        val isAdminAuthenticated: Boolean,
        val currentTab: BottomNavTab,
        val isSyncing: Boolean,
        val fetchMethod: YouTubeFetchMethod,
        val fetchReport: String,
        val statusBannerMessage: String?
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
            val savedMethod = repository.getSavedFetchMethod()
            _selectedFetchMethod.value = savedMethod
            try {
                val (_, report) = repository.syncAllChannels(savedMethod)
                _lastFetchEngineReport.value = report
            } catch (_: Exception) {
            }
        }

        viewModelScope.launch {
            BackgroundPlaybackBridge.commands.collect { command ->
                when (command) {
                    MediaServiceCommand.TOGGLE_PLAY_PAUSE -> togglePlayPause()
                    MediaServiceCommand.STOP_PLAYBACK -> stopAndClosePlayer()
                }
            }
        }
    }

    private fun syncForegroundServiceWithState(state: PlaybackState) {
        val video = state.activeVideo
        if (video != null && state.isBackgroundPlayEnabled) {
            BackgroundMediaService.updatePlaybackService(
                context = appContext,
                title = video.title,
                channelTitle = video.channelTitle,
                isPlaying = state.isPlaying,
                isAudioOnly = state.isAudioOnly
            )
        } else {
            BackgroundMediaService.stopPlaybackService(appContext)
        }
    }

    fun selectTab(tab: BottomNavTab) {
        _currentTab.value = tab
        if (tab == BottomNavTab.AUDIO_PLAYER && _playbackState.value.activeVideo != null) {
            _playbackState.update { it.copy(isExpanded = true) }
        } else {
            _playbackState.update { it.copy(isExpanded = false) }
        }
    }

    fun selectFetchMethod(method: YouTubeFetchMethod) {
        viewModelScope.launch {
            _selectedFetchMethod.value = method
            repository.setSavedFetchMethod(method)
            _lastFetchEngineReport.value = "Selected: ${method.title}"
            _statusBannerMessage.value = "Fetch engine: ${method.title}"
        }
    }

    fun selectChannelFilter(channelId: String?) {
        _selectedChannelId.value = channelId
    }

    fun openChannelVideosInHome(channelId: String) {
        _selectedChannelId.value = channelId
        _currentTab.value = BottomNavTab.HOME
        _playbackState.update { it.copy(isExpanded = false) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleDarkTheme() {
        _isDarkTheme.update { !it }
    }

    fun clearStatusMessage() {
        _statusBannerMessage.value = null
    }

    // --- Playback Actions ---

    fun startPlayback(video: VideoEntity, playAsAudio: Boolean, expandImmediately: Boolean = true) {
        val isSameVideo = _playbackState.value.activeVideo?.id == video.id
        _playbackState.update { current ->
            val updated = if (isSameVideo) {
                current.copy(
                    isAudioOnly = playAsAudio,
                    isExpanded = expandImmediately,
                    isPlaying = true
                )
            } else {
                PlaybackState(
                    activeVideo = video,
                    isAudioOnly = playAsAudio,
                    isExpanded = expandImmediately,
                    isPlaying = true,
                    isBackgroundPlayEnabled = current.isBackgroundPlayEnabled,
                    currentSec = 0f,
                    durationSec = 1f,
                    playbackSpeed = current.playbackSpeed
                )
            }
            syncForegroundServiceWithState(updated)
            updated
        }
        if (expandImmediately) {
            _currentTab.value = BottomNavTab.AUDIO_PLAYER
        }
    }

    fun toggleAudioVideoMode(audioOnly: Boolean) {
        _playbackState.update {
            val updated = it.copy(isAudioOnly = audioOnly)
            syncForegroundServiceWithState(updated)
            updated
        }
    }

    fun toggleBackgroundPlayEnabled() {
        _playbackState.update {
            val updated = it.copy(isBackgroundPlayEnabled = !it.isBackgroundPlayEnabled)
            syncForegroundServiceWithState(updated)
            updated
        }
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _playbackState.update { it.copy(isExpanded = expanded) }
        if (!expanded && _currentTab.value == BottomNavTab.AUDIO_PLAYER) {
            _currentTab.value = BottomNavTab.HOME
        }
    }

    fun togglePlayPause() {
        _playbackState.update {
            val updated = it.copy(isPlaying = !it.isPlaying)
            syncForegroundServiceWithState(updated)
            updated
        }
    }

    fun setPlayingStateFromPlayer(playing: Boolean) {
        _playbackState.update {
            if (it.isPlaying == playing) return@update it
            val updated = it.copy(isPlaying = playing)
            syncForegroundServiceWithState(updated)
            updated
        }
    }

    fun seekToSecond(targetSec: Float) {
        val coerced = targetSec.coerceIn(0f, _playbackState.value.durationSec.coerceAtLeast(1f))
        _playbackState.update {
            it.copy(
                currentSec = coerced,
                pendingSeekSec = coerced
            )
        }
    }

    fun skipSeconds(deltaSec: Float) {
        val current = _playbackState.value
        val next = (current.currentSec + deltaSec).coerceIn(0f, current.durationSec.coerceAtLeast(1f))
        seekToSecond(next)
    }

    fun consumePendingSeek() {
        _playbackState.update { it.copy(pendingSeekSec = null) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    fun onPlayerProgressUpdate(currentSec: Float, durationSec: Float) {
        _playbackState.update { state ->
            state.copy(
                currentSec = currentSec.coerceAtLeast(0f),
                durationSec = if (durationSec > 1f) durationSec else state.durationSec
            )
        }
    }

    fun stopAndClosePlayer() {
        BackgroundMediaService.stopPlaybackService(appContext)
        _playbackState.value = PlaybackState()
        if (_currentTab.value == BottomNavTab.AUDIO_PLAYER) {
            _currentTab.value = BottomNavTab.HOME
        }
    }

    // --- Offline Admin Auth & Multi-Engine Channel / Video CRUD ---

    fun authenticateAdmin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val valid = repository.verifyAdminPin(pin)
            if (valid) {
                _isAdminAuthenticated.value = true
                _currentTab.value = BottomNavTab.ADMIN
                _playbackState.update { it.copy(isExpanded = false) }
                _statusBannerMessage.value = "Admin Mode unlocked"
            }
            onResult(valid)
        }
    }

    fun logoutAdmin() {
        _isAdminAuthenticated.value = false
        _currentTab.value = BottomNavTab.HOME
        _statusBannerMessage.value = "Returned to User View"
    }

    fun updateAdminPin(newPin: String) {
        viewModelScope.launch {
            repository.updateAdminPin(newPin)
            _statusBannerMessage.value = "Offline Admin PIN updated"
        }
    }

    fun addChannelByLink(
        urlOrHandle: String,
        category: String,
        overrideMethod: YouTubeFetchMethod? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val methodToUse = overrideMethod ?: _selectedFetchMethod.value
            val result = repository.addOrSyncChannelByLink(urlOrHandle, category, methodToUse)
            _isSyncing.value = false
            result.onSuccess { res ->
                _lastFetchEngineReport.value = res.engineReport
                val msg = "Added '${res.channel.title}' (${res.engineReport})"
                _statusBannerMessage.value = msg
                onComplete(true, msg)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to resolve channel link"
                onComplete(false, msg)
            }
        }
    }

    fun addDirectVideoToChannel(
        channel: ChannelEntity,
        videoOrPlaylistUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.addDirectVideosToChannel(channel, videoOrPlaylistUrl)
            _isSyncing.value = false
            result.onSuccess { addedVideos ->
                val msg = "Added ${addedVideos.size} video(s) to '${channel.title}'"
                _lastFetchEngineReport.value = "Direct Video/Playlist Injection • ${addedVideos.size} added"
                _statusBannerMessage.value = msg
                onComplete(true, msg)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Invalid video or playlist URL"
                onComplete(false, msg)
            }
        }
    }

    fun syncSingleChannel(channel: ChannelEntity, method: YouTubeFetchMethod) {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.syncSingleChannel(channel, method)
            _isSyncing.value = false
            res.onSuccess { data ->
                _lastFetchEngineReport.value = data.engineReport
                _statusBannerMessage.value = "Synced '${channel.title}' via ${data.engineReport}"
            }.onFailure {
                _statusBannerMessage.value = "Sync failed for '${channel.title}'"
            }
        }
    }

    fun editChannel(channelId: String, title: String, category: String, description: String) {
        viewModelScope.launch {
            repository.updateChannelDetails(channelId, title, category, description)
            _statusBannerMessage.value = "Updated '${title}'"
        }
    }

    fun deleteChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            if (_playbackState.value.activeVideo?.channelId == channel.id) {
                stopAndClosePlayer()
            }
            repository.deleteChannel(channel.id)
            _statusBannerMessage.value = "Removed '${channel.title}'"
        }
    }

    fun syncAllChannels() {
        viewModelScope.launch {
            _isSyncing.value = true
            val method = _selectedFetchMethod.value
            val (count, report) = repository.syncAllChannels(method)
            _isSyncing.value = false
            _lastFetchEngineReport.value = report
            _statusBannerMessage.value = "Synced $count channels ($report)"
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            val db = AppDatabase.getInstance(appCtx)
            val repo = StreamRepository(db.streamDao())
            return StreamViewModel(appCtx, repo) as T
        }
    }
}
