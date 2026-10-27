package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AnimeSeries
import com.example.model.EpisodeItem
import com.example.model.WatchStatus
import com.example.ui.components.AnimeZeroRatedPoster
import com.example.ui.components.FillerOrCanonBadge
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.MalRatingBadge
import com.example.ui.components.Star6StreamBadge
import com.example.ui.components.formatSeconds
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    anime: AnimeSeries,
    currentWatchStatus: WatchStatus,
    watchedEpisodeIds: Set<String>,
    downloadedEpisodeIds: Set<String>,
    resumePositions: Map<String, Int>,
    onBack: () -> Unit,
    onUpdateWatchStatus: (WatchStatus) -> Unit,
    onToggleEpisodeWatched: (String) -> Unit,
    onDownloadEpisode: (AnimeSeries, EpisodeItem) -> Unit,
    onPlayEpisode: (AnimeSeries, EpisodeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var episodeSearchQuery by remember { mutableStateOf("") }
    var ascendingOrder by remember { mutableStateOf(true) }

    val displayedEpisodes = remember(anime.episodes, episodeSearchQuery, ascendingOrder) {
        val filtered = anime.episodes.filter { ep ->
            val q = episodeSearchQuery.trim()
            q.isEmpty() ||
                ep.number.toString().contains(q) ||
                ep.titleAr.contains(q, ignoreCase = true) ||
                ep.facebookVideoId.contains(q)
        }
        if (ascendingOrder) filtered.sortedBy { it.number }
        else filtered.sortedByDescending { it.number }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("anime_detail_screen"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Backdrop + Poster + Metadata
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimeZeroRatedPoster(
                    visualTheme = anime.visualTheme,
                    titleEn = anime.titleEn,
                    posterFbcdnUrl = anime.posterFbcdnUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                // Top Bar with Back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AmoledBlack.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onBack() }
                            .testTag("detail_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Star6StreamBadge(text = "سحابة *6 • ${anime.typeAr}")
                        MalRatingBadge(score = anime.malScore)
                    }
                }

                // Overlapping Info Card
                GlassSurfaceCard(
                    cornerRadius = 20.dp,
                    borderColor = Color(anime.visualTheme.primaryColorHex).copy(alpha = 0.5f),
                    containerColor = ObsidianSurface.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 145.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 88.dp, height = 122.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                AnimeZeroRatedPoster(
                                    visualTheme = anime.visualTheme,
                                    titleEn = anime.titleEn,
                                    posterFbcdnUrl = anime.posterFbcdnUrl,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.titleAr,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = anime.titleEn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "الاستوديو: ${anime.studio} • سنة الإنتاج: ${anime.releaseYear}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldStar6
                                )
                                Text(
                                    text = "الحالة: ${anime.statusAr} • الحلقات: ${anime.episodes.size} • الموعد: ${anime.airingDayAr}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Genres
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            anime.genres.forEach { g ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ObsidianElevated,
                                    border = BorderStroke(0.5.dp, GlassBorder)
                                ) {
                                    Text(
                                        text = g,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "قصة الأنمي:",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonNeon
                        )
                        Text(
                            text = anime.synopsisAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        // Watch Status Selector (Anime Slayer Style)
                        Text(
                            text = "حالة المشاهدة في مكتبتي:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                WatchStatus.WATCHING,
                                WatchStatus.COMPLETED,
                                WatchStatus.PLAN_TO_WATCH,
                                WatchStatus.FAVORITE
                            ).forEach { status ->
                                val selected = currentWatchStatus == status
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) CrimsonNeon else ObsidianElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) CrimsonNeon else GlassBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onUpdateWatchStatus(
                                                if (selected) WatchStatus.NONE else status
                                            )
                                        }
                                        .testTag("watch_status_chip_${status.name}")
                                ) {
                                    Text(
                                        text = status.labelAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White else TextSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Episodes Filter & Sort Bar
        item {
            GlassSurfaceCard(
                cornerRadius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قائمة حلقات فيسبوك *6 (${displayedEpisodes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, FacebookBlue.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { ascendingOrder = !ascendingOrder }
                                .testTag("episodes_sort_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (ascendingOrder) "تصاعدي (1 ← آخر)" else "تنازلي (الأحدث أولاً)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = episodeSearchQuery,
                        onValueChange = { episodeSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "ابحث برقم الحلقة أو عنوانها أو Video ID...",
                                color = TextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = FacebookBlue
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FacebookBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = ObsidianElevated,
                            unfocusedContainerColor = ObsidianElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("episode_search_input")
                    )
                }
            }
        }

        // Episodes List Items
        items(displayedEpisodes, key = { it.id }) { ep ->
            val isWatched = watchedEpisodeIds.contains(ep.id)
            val isDownloaded = downloadedEpisodeIds.contains(ep.id)
            val resumeSec = resumePositions[ep.id] ?: 0

            GlassSurfaceCard(
                cornerRadius = 16.dp,
                borderColor = if (isWatched) EmeraldStar6.copy(alpha = 0.45f) else GlassBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("episode_row_${ep.number}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Episode Number Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isWatched) EmeraldStar6.copy(alpha = 0.2f) else CrimsonNeon.copy(alpha = 0.2f),
                        border = BorderStroke(
                            1.dp,
                            if (isWatched) EmeraldStar6 else CrimsonNeon
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${ep.number}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Title + Canon/Filler + Video ID
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FillerOrCanonBadge(isFiller = ep.isFiller)
                            Text(
                                text = "ID: ${ep.facebookVideoId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FacebookBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = ep.titleAr,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val resumeText = if (resumeSec > 5) {
                            " • متوقف عند ${formatSeconds(resumeSec)}"
                        } else ""
                        Text(
                            text = "المدة: ${formatSeconds(ep.durationSeconds)} • ${ep.releaseDate}$resumeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (resumeSec > 5) EmeraldStar6 else TextSecondary
                        )
                    }

                    // Watched Eye Toggle
                    IconButton(
                        onClick = { onToggleEpisodeWatched(ep.id) },
                        modifier = Modifier.testTag("watched_toggle_${ep.number}")
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Default.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = "تمت المشاهدة",
                            tint = if (isWatched) EmeraldStar6 else TextSecondary
                        )
                    }

                    // Direct fbcdn.net Download Button
                    IconButton(
                        onClick = { onDownloadEpisode(anime, ep) },
                        modifier = Modifier.testTag("download_ep_${ep.number}")
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = "تحميل مباشر fbcdn.net",
                            tint = if (isDownloaded) EmeraldStar6 else FacebookBlue
                        )
                    }

                    // Play Episode on *6 Button
                    Button(
                        onClick = { onPlayEpisode(anime, ep) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonNeon,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("play_ep_${ep.number}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "تشغيل",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
