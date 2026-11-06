package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.local.VideoEntity
import com.example.ui.PlaybackState
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldAudio
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Custom WebView that keeps window visibility reported as View.VISIBLE even when the app
 * is minimized or the screen is turned off, allowing continuous background audio playback.
 */
private class BackgroundPlayWebView(context: Context) : WebView(context) {
    var keepPlayingInBackground: Boolean = true

    override fun onWindowVisibilityChanged(visibility: Int) {
        if (keepPlayingInBackground) {
            super.onWindowVisibilityChanged(View.VISIBLE)
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun SandboxedMediaEngine(
    playbackState: PlaybackState,
    onProgress: (Float, Float) -> Unit,
    onPlayingChanged: (Boolean) -> Unit,
    onSeekConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeVideo = playbackState.activeVideo ?: return
    val context = LocalContext.current
    var loadedVideoId by remember { mutableStateOf<String?>(null) }
    var jsPlayerReportedProgress by remember(activeVideo.id) { mutableStateOf(false) }

    val webView = remember {
        BackgroundPlayWebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(AndroidColor.BLACK)
            // Intercept touch events so users can never click YouTube watermarks, channel links, or recommendations
            setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_MOVE ||
                    event.action == MotionEvent.ACTION_DOWN ||
                    event.action == MotionEvent.ACTION_UP
            }
            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
                loadsImagesAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    return shouldBlockNavigationUrl(url, request?.isForMainFrame == true)
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return shouldBlockNavigationUrl(url.orEmpty(), isMainFrame = false)
                }
            }
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onPlayerProgress(currentSec: Float, durationSec: Float) {
                        if (currentSec > 0f || durationSec > 1f) {
                            jsPlayerReportedProgress = true
                        }
                        onProgress(currentSec, durationSec)
                    }

                    @JavascriptInterface
                    fun onPlayerState(stateCode: Int) {
                        if (stateCode == 1) {
                            onPlayingChanged(true)
                        } else if (stateCode == 2 || stateCode == 0) {
                            onPlayingChanged(false)
                        }
                    }
                },
                "AndroidBridge"
            )
        }
    }

    LaunchedEffect(playbackState.isBackgroundPlayEnabled) {
        webView.keepPlayingInBackground = playbackState.isBackgroundPlayEnabled
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    LaunchedEffect(activeVideo.id) {
        if (loadedVideoId != activeVideo.id) {
            loadedVideoId = activeVideo.id
            val html = buildSandboxedYouTubeHtml(activeVideo.id)
            webView.loadDataWithBaseURL(
                "https://www.youtube.com",
                html,
                "text/html",
                "utf-8",
                null
            )
        }
    }

    LaunchedEffect(playbackState.isPlaying, loadedVideoId) {
        val js = if (playbackState.isPlaying) "playMedia();" else "pauseMedia();"
        webView.evaluateJavascript(js, null)
    }

    LaunchedEffect(playbackState.pendingSeekSec) {
        val seekTarget = playbackState.pendingSeekSec
        if (seekTarget != null) {
            webView.evaluateJavascript("seekMedia($seekTarget);", null)
            onSeekConsumed()
        }
    }

    LaunchedEffect(playbackState.playbackSpeed) {
        webView.evaluateJavascript("setSpeed(${playbackState.playbackSpeed});", null)
    }

    LaunchedEffect(activeVideo.id, playbackState.isPlaying, playbackState.playbackSpeed, jsPlayerReportedProgress) {
        val estimatedTotalSec = parseDurationToSeconds(activeVideo.durationText)
        if (!jsPlayerReportedProgress && playbackState.durationSec <= 1f) {
            onProgress(playbackState.currentSec, estimatedTotalSec)
        }
        while (playbackState.isPlaying && !jsPlayerReportedProgress) {
            delay(500)
            val total = if (playbackState.durationSec > 1f) playbackState.durationSec else estimatedTotalSec
            val next = (playbackState.currentSec + (0.5f * playbackState.playbackSpeed)).coerceAtMost(total)
            onProgress(next, total)
            if (next >= total) {
                onPlayingChanged(false)
                break
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        // Transparent touch-blocking shield over the video frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = true, onClick = { onPlayingChanged(!playbackState.isPlaying) })
        )

        if (playbackState.isAudioOnly) {
            AudioOnlyStudioCard(
                video = activeVideo,
                isPlaying = playbackState.isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun shouldBlockNavigationUrl(url: String, isMainFrame: Boolean): Boolean {
    if (url.isBlank()) return true
    if (url.startsWith("about:blank") || url.startsWith("javascript:") || url.startsWith("data:")) {
        return false
    }
    val isAllowedEmbedResource =
        (url.contains("youtube.com/embed/") ||
            url.contains("youtube-nocookie.com/embed/") ||
            url.contains("youtube.com/iframe_api") ||
            url.contains("ytimg.com")) &&
            !url.contains("youtube.com/watch") &&
            !url.contains("youtube.com/channel") &&
            !url.contains("youtube.com/@") &&
            !url.contains("youtube.com/user")

    if (isAllowedEmbedResource && !isMainFrame) {
        return false
    }
    return true
}

private fun parseDurationToSeconds(durationText: String): Float {
    val parts = durationText.trim().split(":")
    if (parts.size == 2) {
        val min = parts[0].toIntOrNull() ?: 10
        val sec = parts[1].toIntOrNull() ?: 0
        return (min * 60 + sec).toFloat().coerceAtLeast(60f)
    }
    return 600f
}

@Composable
private fun AudioOnlyStudioCard(
    video: VideoEntity,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_anim"
    )

    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF064E3B),
                        Color(0xFF0B0E14)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, EmeraldAudio.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Audio Mode",
                        tint = EmeraldAudio,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = EmeraldAudio.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, EmeraldAudio.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = EmeraldAudio,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AUDIO-ONLY • BACKGROUND READY",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldAudio,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = video.channelTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(20.dp)
                ) {
                    val multipliers = listOf(0.5f, 0.9f, 0.6f, 1.0f, 0.75f, 0.4f, 0.85f, 0.65f, 0.95f, 0.55f)
                    multipliers.forEachIndexed { idx, mult ->
                        val activeFraction = if (isPlaying) {
                            ((wavePhase * mult) + (idx % 3) * 0.15f).coerceIn(0.2f, 1f)
                        } else {
                            0.2f
                        }
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height((20f * activeFraction).dp)
                                .clip(RoundedCornerShape(50))
                                .background(EmeraldAudio)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NativePlaybackControlPanel(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipSeconds: (Float) -> Unit,
    onToggleAudioMode: (Boolean) -> Unit,
    onToggleBackgroundPlay: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (playbackState.isAudioOnly) EmeraldAudio else CrimsonPrimary
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Responsive FlowRow so controls fit any display width cleanly without clipping
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Video vs Play as Audio Switch
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val videoSelected = !playbackState.isAudioOnly
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (videoSelected) CrimsonPrimary else Color.Transparent)
                        .clickable { onToggleAudioMode(false) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("switch_video_mode_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Mode",
                        tint = if (videoSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Video",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (videoSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val audioSelected = playbackState.isAudioOnly
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (audioSelected) EmeraldAudio else Color.Transparent)
                        .clickable { onToggleAudioMode(true) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("switch_audio_mode_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Play as Audio",
                        tint = if (audioSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Play as Audio",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (audioSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Background Play When Minimized Toggle Pill
            Surface(
                color = if (playbackState.isBackgroundPlayEnabled) EmeraldAudio.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    if (playbackState.isBackgroundPlayEnabled) EmeraldAudio else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onToggleBackgroundPlay() }
                    .testTag("toggle_background_play_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhonelinkLock,
                        contentDescription = "Background Play",
                        tint = if (playbackState.isBackgroundPlayEnabled) EmeraldAudio else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (playbackState.isBackgroundPlayEnabled) "Background Play: ON" else "Background Play: OFF",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (playbackState.isBackgroundPlayEnabled) EmeraldAudio else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val safeDuration = playbackState.durationSec.coerceAtLeast(1f)
        val safeCurrent = playbackState.currentSec.coerceIn(0f, safeDuration)

        Slider(
            value = safeCurrent,
            onValueChange = { onSeek(it) },
            valueRange = 0f..safeDuration,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_seek_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTimeSeconds(safeCurrent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTimeSeconds(safeDuration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onSkipSeconds(-10f) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("player_rewind_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = if (playbackState.isAudioOnly) Color.Black else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = { onSkipSeconds(10f) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("player_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                speeds.forEach { spd ->
                    val isSelected = playbackState.playbackSpeed == spd
                    Surface(
                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) BorderStroke(1.dp, accentColor) else null,
                        modifier = Modifier.clickable { onSelectSpeed(spd) }
                    ) {
                        Text(
                            text = "${spd}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeSeconds(sec: Float): String {
    val totalSec = sec.toInt().coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun buildSandboxedYouTubeHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background: #000; overflow: hidden; user-select: none; }
                html, body { width: 100%; height: 100%; background: #000; }
                #player-wrapper { position: relative; width: 100%; height: 100%; }
                iframe { width: 100%; height: 100%; border: 0; pointer-events: none; }
                #shield { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 9999; background: transparent; }
            </style>
            <script>
                // Override Page Visibility API so backgrounding/minimizing the app never pauses audio playback
                try {
                    Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                    Object.defineProperty(document, 'webkitHidden', { get: function() { return false; }, configurable: true });
                    Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                    window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                    window.addEventListener('webkitvisibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                    window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);
                } catch (e) {}
            </script>
        </head>
        <body>
            <div id="player-wrapper">
                <div id="player"></div>
                <div id="shield"></div>
            </div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                var progressTimer;
                var isReady = false;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'controls': 0,
                            'rel': 0,
                            'modestbranding': 1,
                            'iv_load_policy': 3,
                            'disablekb': 1,
                            'fs': 0,
                            'playsinline': 1,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange
                        }
                    });
                }

                function onPlayerReady(event) {
                    isReady = true;
                    event.target.playVideo();
                    startProgressPolling();
                }

                function onPlayerStateChange(event) {
                    if (window.AndroidBridge && window.AndroidBridge.onPlayerState) {
                        window.AndroidBridge.onPlayerState(event.data);
                    }
                }

                function startProgressPolling() {
                    if (progressTimer) clearInterval(progressTimer);
                    progressTimer = setInterval(function() {
                        if (player && isReady && player.getCurrentTime && player.getDuration) {
                            var cur = player.getCurrentTime() || 0;
                            var dur = player.getDuration() || 0;
                            if (dur > 5 && cur >= dur - 1.2) {
                                player.pauseVideo();
                                player.seekTo(0, true);
                                if (window.AndroidBridge) window.AndroidBridge.onPlayerState(2);
                            }
                            if (dur > 1 && window.AndroidBridge && window.AndroidBridge.onPlayerProgress) {
                                window.AndroidBridge.onPlayerProgress(cur, dur);
                            }
                        }
                    }, 500);
                }

                function playMedia() {
                    if (player && isReady && player.playVideo) player.playVideo();
                }

                function pauseMedia() {
                    if (player && isReady && player.pauseVideo) player.pauseVideo();
                }

                function seekMedia(sec) {
                    if (player && isReady && player.seekTo) player.seekTo(sec, true);
                }

                function setSpeed(rate) {
                    if (player && isReady && player.setPlaybackRate) player.setPlaybackRate(rate);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
