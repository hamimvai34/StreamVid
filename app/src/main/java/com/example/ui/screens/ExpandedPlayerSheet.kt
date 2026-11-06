package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.example.data.local.VideoEntity
import com.example.ui.PlaybackState
import com.example.ui.components.NativePlaybackControlPanel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldAudio

@Composable
fun ExpandedPlayerOverlayContent(
    playbackState: PlaybackState,
    sameChannelVideos: List<VideoEntity>,
    firstAvailableVideo: VideoEntity?,
    mediaEngineSlot: @Composable () -> Unit,
    onMinimizeToMiniPlayer: () -> Unit,
    onClosePlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipSeconds: (Float) -> Unit,
    onToggleAudioMode: (Boolean) -> Unit,
    onToggleBackgroundPlay: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onPlayAnotherChannelVideo: (VideoEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeVideo = playbackState.activeVideo

    if (activeVideo == null) {
        EmptyPlayerPromptScreen(
            firstAvailableVideo = firstAvailableVideo,
            onStartVideo = { video, asAudio ->
                onPlayAnotherChannelVideo(video, asAudio)
            },
            modifier = modifier
        )
        return
    }

    BackHandler {
        onMinimizeToMiniPlayer()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("expanded_player_screen")
    ) {
        val isWideScreen = maxWidth >= 700.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Simple, User-Friendly Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onMinimizeToMiniPlayer,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("minimize_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Minimize to Mini-Player",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldAudio,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activeVideo.channelTitle.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAudio,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (playbackState.isBackgroundPlayEnabled) {
                                "Plays in background when app is minimized"
                            } else {
                                "Curated Whitelisted Stream"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onClosePlayer,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop and Close Player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isWideScreen) {
                // Wide / Landscape / Tablet Layout: Left pane for Player + Controls, Right pane for Same-Channel Uploads
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        mediaEngineSlot()

                        NativePlaybackControlPanel(
                            playbackState = playbackState,
                            onTogglePlayPause = onTogglePlayPause,
                            onSeek = onSeek,
                            onSkipSeconds = onSkipSeconds,
                            onToggleAudioMode = onToggleAudioMode,
                            onToggleBackgroundPlay = onToggleBackgroundPlay,
                            onSelectSpeed = onSelectSpeed
                        )
                    }

                    SameChannelQueuePane(
                        activeVideo = activeVideo,
                        sameChannelVideos = sameChannelVideos,
                        onPlayAnotherChannelVideo = onPlayAnotherChannelVideo,
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    )
                }
            } else {
                // Compact / Portrait Phone Layout: Stacked cleanly
                mediaEngineSlot()

                NativePlaybackControlPanel(
                    playbackState = playbackState,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onSkipSeconds = onSkipSeconds,
                    onToggleAudioMode = onToggleAudioMode,
                    onToggleBackgroundPlay = onToggleBackgroundPlay,
                    onSelectSpeed = onSelectSpeed
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                SameChannelQueuePane(
                    activeVideo = activeVideo,
                    sameChannelVideos = sameChannelVideos,
                    onPlayAnotherChannelVideo = onPlayAnotherChannelVideo,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SameChannelQueuePane(
    activeVideo: VideoEntity,
    sameChannelVideos: List<VideoEntity>,
    onPlayAnotherChannelVideo: (VideoEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = activeVideo.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeVideo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More from ${activeVideo.channelTitle} (${sameChannelVideos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = EmeraldAudio.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Same Channel Only",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldAudio,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (sameChannelVideos.isEmpty()) {
            item {
                Text(
                    text = "No other videos cached for this channel yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(sameChannelVideos, key = { it.id }) { itemVideo ->
                SameChannelCompactVideoRow(
                    video = itemVideo,
                    onWatchVideo = { onPlayAnotherChannelVideo(itemVideo, false) },
                    onListenAudio = { onPlayAnotherChannelVideo(itemVideo, true) }
                )
            }
        }
    }
}

@Composable
private fun EmptyPlayerPromptScreen(
    firstAvailableVideo: VideoEntity?,
    onStartVideo: (VideoEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(EmeraldAudio.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = EmeraldAudio,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Audio & Video Player Ready",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select any approved channel video to watch distraction-free or listen in Audio-Only background mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (firstAvailableVideo != null) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartVideo(firstAvailableVideo, false) },
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
                            Text("Watch Featured")
                        }

                        Button(
                            onClick = { onStartVideo(firstAvailableVideo, true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAudio,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play as Audio", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SameChannelCompactVideoRow(
    video: VideoEntity,
    onWatchVideo: () -> Unit,
    onListenAudio: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWatchVideo() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${video.durationText} • ${video.publishedText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onWatchVideo,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CrimsonPrimary.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Watch Video",
                    tint = CrimsonPrimary
                )
            }

            IconButton(
                onClick = onListenAudio,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldAudio.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Play as Audio",
                    tint = EmeraldAudio
                )
            }
        }
    }
}
