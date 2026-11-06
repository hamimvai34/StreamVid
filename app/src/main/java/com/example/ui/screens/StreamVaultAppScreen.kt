package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.ChannelEntity
import com.example.data.local.VideoEntity
import com.example.ui.BottomNavTab
import com.example.ui.PlaybackState
import com.example.ui.StreamUiState
import com.example.ui.StreamViewModel
import com.example.ui.components.ChannelSelectorRow
import com.example.ui.components.CuratedVideoCard
import com.example.ui.components.FeaturedHeroBanner
import com.example.ui.components.PersistentMiniPlayerBar
import com.example.ui.components.SandboxedMediaEngine
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldAudio
import kotlinx.coroutines.delay

@Composable
fun StreamVaultAppScreen(
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var showAdminPinModal by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(3500)
            viewModel.clearStatusMessage()
        }
    }

    // Single persistent movableContentOf slot so the WebView audio/video engine never stops when switching tabs or minimizing
    val persistentMediaSlot = remember {
        movableContentOf { state: PlaybackState ->
            SandboxedMediaEngine(
                playbackState = state,
                onProgress = viewModel::onPlayerProgressUpdate,
                onPlayingChanged = viewModel::setPlayingStateFromPlayer,
                onSeekConsumed = viewModel::consumePendingSeek
            )
        }
    }

    val onNavigateTab: (BottomNavTab) -> Unit = { tab ->
        if (tab == BottomNavTab.ADMIN && !uiState.isAdminAuthenticated) {
            showAdminPinModal = true
        } else {
            viewModel.selectTab(tab)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp
        val isPlayerScreenVisible = playbackState.isExpanded || uiState.currentTab == BottomNavTab.AUDIO_PLAYER

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StreamVaultSimpleTopBar(
                    isDarkTheme = uiState.isDarkTheme,
                    isBackgroundPlayOn = playbackState.isBackgroundPlayEnabled,
                    onToggleTheme = viewModel::toggleDarkTheme
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Persistent Mini-Player Bar above the Bottom Navigation Bar when playing & not on full Player tab
                    if (playbackState.activeVideo != null && !isPlayerScreenVisible) {
                        PersistentMiniPlayerBar(
                            playbackState = playbackState,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipForward = { viewModel.skipSeconds(10f) },
                            onExpand = { viewModel.selectTab(BottomNavTab.AUDIO_PLAYER) },
                            onClose = viewModel::stopAndClosePlayer
                        )
                    }

                    // Standard Material 3 Bottom Navigation Bar on phones and medium screens
                    if (!useNavigationRail) {
                        StreamVaultBottomNavigationBar(
                            currentTab = if (playbackState.isExpanded) BottomNavTab.AUDIO_PLAYER else uiState.currentTab,
                            hasActiveMedia = playbackState.activeVideo != null,
                            isAudioOnly = playbackState.isAudioOnly,
                            onSelectTab = onNavigateTab
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Adaptive NavigationRail for large / landscape / tablet displays so any screen size fits cleanly
                if (useNavigationRail) {
                    StreamVaultSideNavigationRail(
                        currentTab = if (playbackState.isExpanded) BottomNavTab.AUDIO_PLAYER else uiState.currentTab,
                        hasActiveMedia = playbackState.activeVideo != null,
                        isAudioOnly = playbackState.isAudioOnly,
                        onSelectTab = onNavigateTab
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when {
                        isPlayerScreenVisible -> {
                            val activeVideo = playbackState.activeVideo
                            val sameChannelVideos = remember(uiState.allWhitelistedVideos, activeVideo?.id, activeVideo?.channelId) {
                                if (activeVideo == null) emptyList()
                                else uiState.allWhitelistedVideos.filter {
                                    it.channelId == activeVideo.channelId && it.id != activeVideo.id
                                }
                            }

                            ExpandedPlayerOverlayContent(
                                playbackState = playbackState,
                                sameChannelVideos = sameChannelVideos,
                                firstAvailableVideo = uiState.allWhitelistedVideos.firstOrNull(),
                                mediaEngineSlot = {
                                    if (playbackState.activeVideo != null) {
                                        persistentMediaSlot(playbackState)
                                    }
                                },
                                onMinimizeToMiniPlayer = {
                                    viewModel.setPlayerExpanded(false)
                                },
                                onClosePlayer = viewModel::stopAndClosePlayer,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSeek = viewModel::seekToSecond,
                                onSkipSeconds = viewModel::skipSeconds,
                                onToggleAudioMode = viewModel::toggleAudioVideoMode,
                                onToggleBackgroundPlay = viewModel::toggleBackgroundPlayEnabled,
                                onSelectSpeed = viewModel::setPlaybackSpeed,
                                onPlayAnotherChannelVideo = { nextVideo, asAudio ->
                                    viewModel.startPlayback(nextVideo, playAsAudio = asAudio, expandImmediately = true)
                                }
                            )
                        }

                        uiState.currentTab == BottomNavTab.CHANNELS -> {
                            ApprovedChannelsDirectoryScreen(
                                channels = uiState.channels,
                                allVideos = uiState.allWhitelistedVideos,
                                onBrowseChannelVideos = { channelId ->
                                    viewModel.openChannelVideosInHome(channelId)
                                },
                                onListenChannelLatestAudio = { channelId ->
                                    val latestVideo = uiState.allWhitelistedVideos.firstOrNull { it.channelId == channelId }
                                    if (latestVideo != null) {
                                        viewModel.startPlayback(latestVideo, playAsAudio = true, expandImmediately = true)
                                    } else {
                                        viewModel.openChannelVideosInHome(channelId)
                                    }
                                }
                            )
                        }

                        uiState.currentTab == BottomNavTab.ADMIN && uiState.isAdminAuthenticated -> {
                            AdminDashboardScreen(
                                channels = uiState.channels,
                                allVideos = uiState.allWhitelistedVideos,
                                selectedFetchMethod = uiState.selectedFetchMethod,
                                lastFetchEngineReport = uiState.lastFetchEngineReport,
                                isSyncing = uiState.isSyncing,
                                onSelectFetchMethod = viewModel::selectFetchMethod,
                                onAddChannel = { link, cat, method, cb ->
                                    viewModel.addChannelByLink(link, cat, method, cb)
                                },
                                onAddDirectVideoToChannel = viewModel::addDirectVideoToChannel,
                                onSyncSingleChannel = viewModel::syncSingleChannel,
                                onEditChannel = viewModel::editChannel,
                                onDeleteChannel = viewModel::deleteChannel,
                                onSyncAll = viewModel::syncAllChannels,
                                onUpdateAdminPin = viewModel::updateAdminPin,
                                onExitAdminMode = viewModel::logoutAdmin
                            )
                        }

                        else -> {
                            UserCatalogHubContent(
                                uiState = uiState,
                                activeVideoId = playbackState.activeVideo?.id,
                                isAudioMode = playbackState.isAudioOnly,
                                onSearchQueryChanged = viewModel::updateSearchQuery,
                                onSelectChannel = viewModel::selectChannelFilter,
                                onWatchVideo = { video ->
                                    viewModel.startPlayback(video, playAsAudio = false, expandImmediately = true)
                                },
                                onListenAudio = { video ->
                                    viewModel.startPlayback(video, playAsAudio = true, expandImmediately = true)
                                }
                            )
                        }
                    }

                    // Keep the active WebView mounted when minimized on Home/Channels/Admin tabs so audio never interrupts
                    if (playbackState.activeVideo != null && !isPlayerScreenVisible) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(width = 1.dp, height = 1.dp)
                        ) {
                            persistentMediaSlot(playbackState)
                        }
                    }

                    // Friendly Status Toast Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.statusBannerMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Surface(
                            color = EmeraldAudio,
                            shape = RoundedCornerShape(50),
                            tonalElevation = 6.dp
                        ) {
                            Text(
                                text = uiState.statusBannerMessage.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdminPinModal) {
        AdminPasscodeDialog(
            onDismiss = { showAdminPinModal = false },
            onSubmitPin = { pin, onResult ->
                viewModel.authenticateAdmin(pin) { success ->
                    onResult(success)
                    if (success) {
                        showAdminPinModal = false
                    }
                }
            }
        )
    }
}

@Composable
private fun StreamVaultSimpleTopBar(
    isDarkTheme: Boolean,
    isBackgroundPlayOn: Boolean,
    onToggleTheme: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrimsonPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "StreamVault",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = EmeraldAudio.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isBackgroundPlayOn) "BG AUDIO ON" else "CURATED",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAudio,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Approved Channels Only • No Recommendations",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark or Light Mode",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StreamVaultBottomNavigationBar(
    currentTab: BottomNavTab,
    hasActiveMedia: Boolean,
    isAudioOnly: Boolean,
    onSelectTab: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_navigation_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == BottomNavTab.HOME,
            onClick = { onSelectTab(BottomNavTab.HOME) },
            icon = {
                Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = CrimsonPrimary,
                indicatorColor = CrimsonPrimary
            ),
            modifier = Modifier.testTag("nav_tab_home")
        )

        NavigationBarItem(
            selected = currentTab == BottomNavTab.CHANNELS,
            onClick = { onSelectTab(BottomNavTab.CHANNELS) },
            icon = {
                Icon(imageVector = Icons.Default.Subscriptions, contentDescription = "Channels")
            },
            label = { Text("Channels") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = CrimsonPrimary,
                indicatorColor = CrimsonPrimary
            ),
            modifier = Modifier.testTag("nav_tab_channels")
        )

        NavigationBarItem(
            selected = currentTab == BottomNavTab.AUDIO_PLAYER,
            onClick = { onSelectTab(BottomNavTab.AUDIO_PLAYER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Audio & Video Player"
                )
            },
            label = {
                Text(
                    if (hasActiveMedia && isAudioOnly) "Audio Playing" else "Player"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = EmeraldAudio,
                indicatorColor = EmeraldAudio
            ),
            modifier = Modifier.testTag("nav_tab_player")
        )

        NavigationBarItem(
            selected = currentTab == BottomNavTab.ADMIN,
            onClick = { onSelectTab(BottomNavTab.ADMIN) },
            icon = {
                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Admin")
            },
            label = { Text("Admin") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = CrimsonPrimary,
                indicatorColor = CrimsonPrimary
            ),
            modifier = Modifier.testTag("nav_tab_admin")
        )
    }
}

@Composable
private fun StreamVaultSideNavigationRail(
    currentTab: BottomNavTab,
    hasActiveMedia: Boolean,
    isAudioOnly: Boolean,
    onSelectTab: (BottomNavTab) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .testTag("side_navigation_rail")
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        NavigationRailItem(
            selected = currentTab == BottomNavTab.HOME,
            onClick = { onSelectTab(BottomNavTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationRailItem(
            selected = currentTab == BottomNavTab.CHANNELS,
            onClick = { onSelectTab(BottomNavTab.CHANNELS) },
            icon = { Icon(Icons.Default.Subscriptions, contentDescription = "Channels") },
            label = { Text("Channels") }
        )
        NavigationRailItem(
            selected = currentTab == BottomNavTab.AUDIO_PLAYER,
            onClick = { onSelectTab(BottomNavTab.AUDIO_PLAYER) },
            icon = { Icon(Icons.Default.Headphones, contentDescription = "Player") },
            label = { Text(if (hasActiveMedia && isAudioOnly) "Audio" else "Player") }
        )
        NavigationRailItem(
            selected = currentTab == BottomNavTab.ADMIN,
            onClick = { onSelectTab(BottomNavTab.ADMIN) },
            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
            label = { Text("Admin") }
        )
    }
}

@Composable
private fun ApprovedChannelsDirectoryScreen(
    channels: List<ChannelEntity>,
    allVideos: List<VideoEntity>,
    onBrowseChannelVideos: (String) -> Unit,
    onListenChannelLatestAudio: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("channels_directory_grid"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    text = "Approved Streaming Channels (${channels.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap any channel to browse its exclusive video uploads or listen directly in Audio Mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(channels, key = { it.id }) { channel ->
            val videoCount = allVideos.count { it.channelId == channel.id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBrowseChannelVideos(channel.id) }
                    .testTag("directory_channel_card_${channel.id}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = channel.avatarUrl,
                            contentDescription = channel.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${channel.handle} • $videoCount Videos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldAudio,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = channel.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onBrowseChannelVideos(channel.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Videos", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = { onListenChannelLatestAudio(channel.id) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = EmeraldAudio.copy(alpha = 0.12f),
                                contentColor = EmeraldAudio
                            ),
                            border = BorderStroke(1.5.dp, EmeraldAudio),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Audio", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCatalogHubContent(
    uiState: StreamUiState,
    activeVideoId: String?,
    isAudioMode: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSelectChannel: (String?) -> Unit,
    onWatchVideo: (VideoEntity) -> Unit,
    onListenAudio: (VideoEntity) -> Unit
) {
    val featuredVideo = uiState.filteredVideos.firstOrNull() ?: uiState.allWhitelistedVideos.firstOrNull()
    val selectedChannelObj = uiState.channels.firstOrNull { it.id == uiState.selectedChannelId }

    // Adaptive grid automatically fits any display size (1 column on phones, 2 on foldables/landscape, 3+ on tablets)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 310.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_video_grid"),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Bar
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search approved videos or channels…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChanged("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_videos_input")
                )
            }
        }

        // Hero Banner
        if (uiState.searchQuery.isBlank() && selectedChannelObj == null && featuredVideo != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedHeroBanner(
                    featuredVideo = featuredVideo,
                    totalChannelsCount = uiState.channels.size,
                    onWatchVideo = onWatchVideo,
                    onListenAudio = onListenAudio,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Horizontal Approved Channels Filter Row
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Approved Channels",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldAudio,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Curated Feed",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldAudio
                        )
                    }
                }

                ChannelSelectorRow(
                    channels = uiState.channels,
                    selectedChannelId = uiState.selectedChannelId,
                    onSelectChannel = onSelectChannel
                )
            }
        }

        if (selectedChannelObj != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedChannelObj.title} (${selectedChannelObj.handle})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedChannelObj.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable { onSelectChannel(null) }
                        ) {
                            Text(
                                text = "Show All",
                                style = MaterialTheme.typography.labelLarge,
                                color = CrimsonPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedChannelObj != null) {
                        "${selectedChannelObj.title} Uploads (${uiState.filteredVideos.size})"
                    } else {
                        "Channel Uploads (${uiState.filteredVideos.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Watch or Play as Audio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.filteredVideos.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching whitelisted videos found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try clearing your search filter or use the Admin tab to add more channels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(uiState.filteredVideos, key = { it.id }) { video ->
                CuratedVideoCard(
                    video = video,
                    isCurrentlyPlaying = activeVideoId == video.id,
                    isPlayingAsAudio = isAudioMode,
                    onWatchVideo = { onWatchVideo(video) },
                    onListenAudio = { onListenAudio(video) },
                    onFilterByThisChannel = { onSelectChannel(video.channelId) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
