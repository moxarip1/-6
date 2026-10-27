package com.example.ui.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AnimeSeries
import com.example.model.EpisodeItem
import com.example.model.ResolvedStreamResult
import com.example.model.StreamQuality
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CrimsonNeon
import com.example.ui.theme.EmeraldStar6
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BihiStar6VideoPlayerCard(
    anime: AnimeSeries,
    episode: EpisodeItem,
    resolvedStream: ResolvedStreamResult?,
    selectedQuality: StreamQuality,
    playbackSpeed: Float,
    initialPositionSec: Int,
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    onSelectQuality: (StreamQuality) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSaveProgress: (Int) -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember(episode.id) { mutableStateOf(true) }
    var currentSec by remember(episode.id) {
        mutableIntStateOf(initialPositionSec.coerceIn(0, episode.durationSeconds.coerceAtLeast(1)))
    }
    var resumedBannerVisible by remember(episode.id) {
        mutableStateOf(initialPositionSec > 5)
    }
    var pipStatusMessage by remember { mutableStateOf<String?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var lastLoadedUrl by remember { mutableStateOf("") }

    val availableFormats = resolvedStream?.formats?.takeIf { it.isNotEmpty() } ?: episode.formats
    val activeFormat = availableFormats.find { it.quality == selectedQuality }
        ?: availableFormats.firstOrNull()
    val streamUrl = activeFormat?.mp4Url?.ifBlank { null }
        ?: resolvedStream?.defaultUrl?.ifBlank { null }
        ?: episode.defaultSourceUrl

    LaunchedEffect(isPlaying, playbackSpeed, episode.id, streamUrl) {
        while (isPlaying && streamUrl.isNotBlank()) {
            val tickMs = (1000f / playbackSpeed).toLong().coerceAtLeast(250L)
            delay(tickMs)
            val realPosSec = runCatching {
                videoViewRef?.takeIf { it.isPlaying }?.currentPosition?.div(1000)
            }.getOrNull()
            if (realPosSec != null && realPosSec > 0) {
                currentSec = realPosSec
            } else if (currentSec < episode.durationSeconds) {
                currentSec += 1
            }
            if (currentSec % 5 == 0) {
                onSaveProgress(currentSec)
            }
        }
    }

    GlassSurfaceCard(
        cornerRadius = 20.dp,
        borderColor = FacebookBlue.copy(alpha = 0.65f),
        containerColor = ObsidianSurface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("bihi_video_player_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Stream Resolver Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmoledBlack.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Star6StreamBadge(text = "مشغل *6 المدمج • ${selectedQuality.shortTag}")
                        FillerOrCanonBadge(isFiller = episode.isFiller)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${anime.titleAr} • الحلقة ${episode.number}: ${episode.titleAr}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val activity = context as? Activity
                            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                runCatching {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity.enterPictureInPictureMode(params)
                                }.onFailure {
                                    pipStatusMessage = "وضع النافذة العائمة (PiP) غير مدعوم على هذا الجهاز"
                                }
                            }
                        },
                        modifier = Modifier.testTag("player_pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "صورة داخل صورة (PiP)",
                            tint = FacebookBlue
                        )
                    }
                    IconButton(
                        onClick = {
                            onSaveProgress(currentSec)
                            onClosePlayer()
                        },
                        modifier = Modifier.testTag("player_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق المشغل",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Video Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(AmoledBlack)
            ) {
                if (streamUrl.isNotBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                videoViewRef = this
                                setOnPreparedListener { mp ->
                                    if (initialPositionSec > 0) {
                                        seekTo(initialPositionSec * 1000)
                                    }
                                    if (isPlaying) start()
                                }
                                setOnErrorListener { _, _, _ -> true }
                            }
                        },
                        update = { view ->
                            videoViewRef = view
                            if (lastLoadedUrl != streamUrl) {
                                lastLoadedUrl = streamUrl
                                runCatching {
                                    view.setVideoURI(Uri.parse(streamUrl))
                                    if (isPlaying) view.start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Waiting for Graph API resolution or missing token/stream
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (resolvedStream == null) {
                            CircularProgressIndicator(
                                color = FacebookBlue,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "جاري فك رابط البث المباشر من graph.facebook.com...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = resolvedStream.errorMessage
                                    ?: "لا يتوفر رابط بث مباشر (source) لهذه الحلقة حالياً.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberGold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Top-left Live fbcdn.net Resolver Telemetry Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmoledBlack.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = if (episode.facebookVideoId.isNotBlank()) {
                                "Graph API v19.0 • Video ID: ${episode.facebookVideoId}"
                            } else {
                                "بث مباشر • الحلقة ${episode.number}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldStar6,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المصدر: ${resolvedStream?.sourceHost ?: "جاري التحقق..."}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                if (resumedBannerVisible) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoyalViolet.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clickable { resumedBannerVisible = false }
                    ) {
                        Text(
                            text = "تم الاستئناف تلقائياً من ${formatSeconds(initialPositionSec)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Center Play/Pause + Prev/Next Controls
                if (streamUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = onPreviousEpisode,
                            enabled = hasPreviousEpisode,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AmoledBlack.copy(alpha = 0.7f))
                                .testTag("player_prev_episode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "الحلقة السابقة",
                                tint = if (hasPreviousEpisode) Color.White else TextSecondary.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = {
                                isPlaying = !isPlaying
                                runCatching {
                                    if (isPlaying) videoViewRef?.start() else videoViewRef?.pause()
                                }
                            },
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(CrimsonNeon)
                                .testTag("player_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(
                            onClick = onNextEpisode,
                            enabled = hasNextEpisode,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AmoledBlack.copy(alpha = 0.7f))
                                .testTag("player_next_episode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "الحلقة التالية",
                                tint = if (hasNextEpisode) Color.White else TextSecondary.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // Bottom-end Skip Intro (+85s تخطي المقدمة) Button
                    Button(
                        onClick = {
                            val jumped = (currentSec + 85).coerceAtMost(episode.durationSeconds)
                            currentSec = jumped
                            resumedBannerVisible = false
                            onSaveProgress(jumped)
                            runCatching { videoViewRef?.seekTo(jumped * 1000) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonNeon.copy(alpha = 0.92f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .testTag("skip_intro_85s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "+85s تخطي المقدمة",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Timeline Scrubber & Controls Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatSeconds(currentSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonNeon,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = currentSec.toFloat(),
                        onValueChange = {
                            currentSec = it.toInt()
                            onSaveProgress(currentSec)
                            runCatching { videoViewRef?.seekTo(currentSec * 1000) }
                        },
                        valueRange = 0f..episode.durationSeconds.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CrimsonNeon,
                            activeTrackColor = FacebookBlue,
                            inactiveTrackColor = ObsidianElevated
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    Text(
                        text = formatSeconds(episode.durationSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                pipStatusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StreamQuality.entries.forEach { quality ->
                        val isSelected = selectedQuality == quality
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) FacebookBlue.copy(alpha = 0.28f) else ObsidianElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) FacebookBlue else GlassBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectQuality(quality) }
                                .testTag("quality_chip_${quality.name}")
                        ) {
                            Text(
                                text = quality.labelAr,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { spd ->
                        val active = playbackSpeed == spd
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) RoyalViolet.copy(alpha = 0.3f) else ObsidianElevated,
                            border = BorderStroke(
                                1.dp,
                                if (active) RoyalViolet else GlassBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectSpeed(spd) }
                                .testTag("speed_chip_${spd}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = "${spd}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) Color.White else TextSecondary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatSeconds(totalSec: Int): String {
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
