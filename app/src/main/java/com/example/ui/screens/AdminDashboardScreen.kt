package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ChannelEntity
import com.example.data.local.VideoEntity
import com.example.data.remote.YouTubeFetchMethod
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldAudio

@Composable
fun AdminPasscodeDialog(
    onDismiss: () -> Unit,
    onSubmitPin: (String, (Boolean) -> Unit) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = CrimsonPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Offline Admin Authentication",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Only administrators can add, edit, or remove approved YouTube channels and configure bot-bypass fetching methods.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Default Offline PIN: 1234",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldAudio,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        errorMessage = null
                    },
                    label = { Text("Admin PIN Code") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitPin(pinInput) { success ->
                        if (!success) {
                            errorMessage = "Incorrect Admin PIN. Try 1234."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("admin_pin_unlock_button")
            ) {
                Text("Unlock Admin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    channels: List<ChannelEntity>,
    allVideos: List<VideoEntity>,
    selectedFetchMethod: YouTubeFetchMethod,
    lastFetchEngineReport: String,
    isSyncing: Boolean,
    onSelectFetchMethod: (YouTubeFetchMethod) -> Unit,
    onAddChannel: (String, String, YouTubeFetchMethod, (Boolean, String) -> Unit) -> Unit,
    onAddDirectVideoToChannel: (ChannelEntity, String, (Boolean, String) -> Unit) -> Unit,
    onSyncSingleChannel: (ChannelEntity, YouTubeFetchMethod) -> Unit,
    onEditChannel: (String, String, String, String) -> Unit,
    onDeleteChannel: (ChannelEntity) -> Unit,
    onSyncAll: () -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onExitAdminMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onExitAdminMode()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var deletingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var addingVideoToChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showFetchMethodExpand by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Admin Header Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CrimsonPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = "Admin Curation Portal",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${channels.size} Approved Channels • ${allVideos.size} Cached Videos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldAudio
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onExitAdminMode,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("exit_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_add_channel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Channel / Playlist")
                        }

                        OutlinedButton(
                            onClick = onSyncAll,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("admin_sync_all_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync All")
                        }

                        IconButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("admin_change_pin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Change Offline Admin PIN",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Multi-Engine YouTube Fetcher & Bot-Detection Bypass Section
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EmeraldAudio.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_fetch_method_section")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFetchMethodExpand = !showFetchMethodExpand },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldAudio.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = EmeraldAudio
                                )
                            }
                            Column {
                                Text(
                                    text = "YouTube Fetching Engine (Bot-Bypass)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = lastFetchEngineReport,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldAudio,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        TextButton(onClick = { showFetchMethodExpand = !showFetchMethodExpand }) {
                            Text(if (showFetchMethodExpand) "Hide" else "Change")
                        }
                    }

                    if (showFetchMethodExpand) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Switch fetching method below if YouTube bot detection blocks a specific route:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            YouTubeFetchMethod.entries.forEach { method ->
                                val isSelected = selectedFetchMethod == method
                                Surface(
                                    color = if (isSelected) {
                                        EmeraldAudio.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) EmeraldAudio else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectFetchMethod(method) }
                                        .testTag("fetch_method_${method.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) EmeraldAudio else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = method.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSelected) EmeraldAudio else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = method.subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Approved Channels List with Per-Channel Sync & Direct Video Add
        item {
            Text(
                text = "Approved Channels (${channels.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(channels, key = { it.id }) { channel ->
            val videoCount = allVideos.count { it.channelId == channel.id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_channel_card_${channel.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = channel.avatarUrl,
                            contentDescription = channel.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = channel.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = EmeraldAudio.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = channel.category,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldAudio,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${channel.handle} • $videoCount whitelisted videos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { editingChannel = channel },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("edit_channel_${channel.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Channel",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { deletingChannel = channel },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_channel_${channel.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Channel",
                                tint = CrimsonPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = channel.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Per-Channel Bot-Bypass Actions: Sync Channel OR Inject Video/Playlist URL Directly
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSyncSingleChannel(channel, selectedFetchMethod) },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync via Active Engine", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = { addingVideoToChannel = channel },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldAudio),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("add_direct_video_${channel.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCall,
                                contentDescription = null,
                                tint = EmeraldAudio,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ Add Video / Playlist URL",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldAudio
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddChannelLinkDialog(
            initialFetchMethod = selectedFetchMethod,
            isSyncing = isSyncing,
            onDismiss = { showAddDialog = false },
            onAdd = { link, category, chosenMethod, cb ->
                onAddChannel(link, category, chosenMethod) { ok, msg ->
                    cb(ok, msg)
                    if (ok) showAddDialog = false
                }
            }
        )
    }

    addingVideoToChannel?.let { targetChannel ->
        AddDirectVideoToChannelDialog(
            channel = targetChannel,
            isSyncing = isSyncing,
            onDismiss = { addingVideoToChannel = null },
            onAddVideo = { url, cb ->
                onAddDirectVideoToChannel(targetChannel, url) { ok, msg ->
                    cb(ok, msg)
                    if (ok) addingVideoToChannel = null
                }
            }
        )
    }

    editingChannel?.let { channel ->
        EditChannelDialog(
            channel = channel,
            onDismiss = { editingChannel = null },
            onSave = { title, cat, desc ->
                onEditChannel(channel.id, title, cat, desc)
                editingChannel = null
            }
        )
    }

    deletingChannel?.let { channel ->
        AlertDialog(
            onDismissRequest = { deletingChannel = null },
            title = { Text("Remove '${channel.title}'?") },
            text = {
                Text("This will immediately remove '${channel.title}' and all of its cached videos from the User Hub.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChannel(channel)
                        deletingChannel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_delete_channel_button")
                ) {
                    Text("Delete Channel")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingChannel = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldAudio) },
            title = { Text("Change Offline Admin PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN (min 4 digits)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.trim().length >= 4) {
                            onUpdateAdminPin(newPin.trim())
                            showChangePinDialog = false
                        }
                    },
                    enabled = newPin.trim().length >= 4
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddChannelLinkDialog(
    initialFetchMethod: YouTubeFetchMethod,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String, YouTubeFetchMethod, (Boolean, String) -> Unit) -> Unit
) {
    var linkInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Education") }
    var chosenMethod by remember { mutableStateOf(initialFetchMethod) }
    var feedbackError by remember { mutableStateOf<String?>(null) }

    val quickChannelPresets = listOf(
        "https://www.youtube.com/@veritasium" to "Science",
        "https://www.youtube.com/@TEDEd" to "Education",
        "https://www.youtube.com/@Fireship" to "Programming"
    )

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        title = { Text("Add Channel, Playlist, or Video Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste a YouTube channel URL (@handle or /channel/UC...), playlist URL (?list=PL...), or video URL (youtu.be/...):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = linkInput,
                    onValueChange = {
                        linkInput = it
                        feedbackError = null
                    },
                    label = { Text("YouTube Channel / Playlist / Video URL") },
                    placeholder = { Text("https://www.youtube.com/@veritasium") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_channel_url_input")
                )

                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text("Category Label") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_channel_category_input")
                )

                Text(
                    text = "Fetch Method for this URL:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    YouTubeFetchMethod.entries.forEach { method ->
                        val selected = chosenMethod == method
                        val shortLabel = when (method) {
                            YouTubeFetchMethod.AUTO_CASCADE -> "Auto Cascade"
                            YouTubeFetchMethod.YOUTUBE_RSS_XML -> "RSS XML"
                            YouTubeFetchMethod.INNERTUBE_WEB_JSON -> "InnerTube JSON"
                            YouTubeFetchMethod.PUBLIC_RELAY_MIRROR -> "Bot-Bypass Relay"
                            YouTubeFetchMethod.OEMBED_DIRECT_VIDEO -> "oEmbed / Direct"
                        }
                        Surface(
                            color = if (selected) EmeraldAudio.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            border = if (selected) BorderStroke(1.dp, EmeraldAudio) else null,
                            modifier = Modifier.clickable { chosenMethod = method }
                        ) {
                            Text(
                                text = shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) EmeraldAudio else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Quick Preset Channels:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickChannelPresets.forEach { (presetUrl, presetCat) ->
                        val label = presetUrl.substringAfterLast("/")
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable {
                                linkInput = presetUrl
                                categoryInput = presetCat
                                feedbackError = null
                            }
                        ) {
                            Text(
                                text = "+ $label",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAudio,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (feedbackError != null) {
                    Text(
                        text = feedbackError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(linkInput, categoryInput, chosenMethod) { ok, msg ->
                        if (!ok) feedbackError = msg
                    }
                },
                enabled = !isSyncing && linkInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("confirm_add_channel_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching…")
                } else {
                    Text("Fetch & Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSyncing) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddDirectVideoToChannelDialog(
    channel: ChannelEntity,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onAddVideo: (String, (Boolean, String) -> Unit) -> Unit
) {
    var videoUrl by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        title = { Text("Add Video or Playlist to ${channel.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Bypass channel scraping completely by pasting any individual YouTube video link (watch?v=... or youtu.be/...) or playlist link (?list=PL...):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = {
                        videoUrl = it
                        errorMsg = null
                    },
                    label = { Text("YouTube Video or Playlist URL") },
                    placeholder = { Text("https://youtu.be/dQw4w9WgXcQ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddVideo(videoUrl) { ok, msg ->
                        if (!ok) errorMsg = msg
                    }
                },
                enabled = !isSyncing && videoUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAudio, contentColor = Color.Black)
            ) {
                Text("Add to Channel", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSyncing) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditChannelDialog(
    channel: ChannelEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(channel.title) }
    var category by remember { mutableStateOf(channel.category) }
    var description by remember { mutableStateOf(channel.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Channel Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Channel Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, category, description) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
