package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DemoAnimeCatalog
import com.example.model.AnimeSeries
import com.example.model.EpisodeItem
import com.example.ui.components.AnimeZeroRatedPoster
import com.example.ui.components.FillerOrCanonBadge
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.MalRatingBadge
import com.example.ui.components.Star6StreamBadge
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

@Composable
fun HomeScreen(
    catalog: List<AnimeSeries>,
    isLiveConnected: Boolean,
    connectedPageName: String,
    onSelectAnime: (AnimeSeries) -> Unit,
    onPlayEpisode: (AnimeSeries, EpisodeItem) -> Unit,
    onOpenAdminStudio: () -> Unit,
    onOpenWebReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (catalog.isEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("home_screen_empty"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ServerAwaitingConnectionCard(
                    isLiveConnected = isLiveConnected,
                    connectedPageName = connectedPageName,
                    onOpenAdminStudio = onOpenAdminStudio,
                    onOpenWebReader = onOpenWebReader
                )
            }
        }
        return
    }

    val spotlightList = remember(catalog) {
        catalog.filter { it.isSpotlight }.ifEmpty { catalog.take(3) }
    }
    var spotlightIndex by remember { mutableIntStateOf(0) }
    val activeSpotlight = spotlightList.getOrNull(spotlightIndex % spotlightList.size.coerceAtLeast(1))
        ?: catalog.firstOrNull()

    val latestEpisodes = remember(catalog) {
        catalog.flatMap { anime ->
            anime.episodes.takeLast(2).map { ep -> anime to ep }
        }
    }
    val seasonalAnime = remember(catalog) {
        catalog.filter { it.isSeasonal }.ifEmpty { catalog }
    }
    val topRatedAnime = remember(catalog) {
        catalog.sortedByDescending { it.malScore }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (activeSpotlight != null) {
            item {
                HeroSpotlightBanner(
                    anime = activeSpotlight,
                    spotlightCount = spotlightList.size,
                    activeIndex = spotlightIndex % spotlightList.size.coerceAtLeast(1),
                    onSelectIndex = { spotlightIndex = it },
                    onWatchNow = {
                        activeSpotlight.episodes.firstOrNull()?.let { ep ->
                            onPlayEpisode(activeSpotlight, ep)
                        } ?: onSelectAnime(activeSpotlight)
                    },
                    onOpenDetails = { onSelectAnime(activeSpotlight) }
                )
            }
        }

        if (latestEpisodes.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "آخر الحلقات المضافة على *6",
                    subtitle = "روابط فيديو fbcdn.net فورية عبر Graph API",
                    accentColor = FacebookBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(latestEpisodes, key = { it.second.id }) { (anime, episode) ->
                        LatestEpisodeCard(
                            anime = anime,
                            episode = episode,
                            onClick = { onPlayEpisode(anime, episode) }
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "أنميات السيرفر المضافة",
                subtitle = "عروض الأنمي المسحوبة من صفحة فيسبوك *6",
                accentColor = CrimsonNeon
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(seasonalAnime, key = { it.id }) { anime ->
                    AnimePosterCard(
                        anime = anime,
                        onClick = { onSelectAnime(anime) }
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "الأعلى تقييماً",
                subtitle = "ترتيب الأنميات المضافة حسب التقييم",
                accentColor = AmberGold
            )
        }

        itemsIndexed(topRatedAnime, key = { _, item -> item.id }) { idx, anime ->
            TopRatedAnimeRowCard(
                rank = idx + 1,
                anime = anime,
                onClick = { onSelectAnime(anime) },
                onQuickPlay = {
                    anime.episodes.firstOrNull()?.let { ep -> onPlayEpisode(anime, ep) }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun ServerAwaitingConnectionCard(
    isLiveConnected: Boolean,
    connectedPageName: String,
    onOpenAdminStudio: () -> Unit,
    onOpenWebReader: () -> Unit
) {
    GlassSurfaceCard(
        cornerRadius = 22.dp,
        borderColor = FacebookBlue.copy(alpha = 0.65f),
        containerColor = ObsidianSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("awaiting_server_connection_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Star6StreamBadge(text = "Graph API v19.0 • نسخة الإنتاج الكاملة")
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(FacebookBlue.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = FacebookBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isLiveConnected) {
                    "متصل بـ ($connectedPageName) • لم يتم نشر حلقات بعد"
                } else {
                    "بانتظار ربط سيرفر صفحة فيسبوك *6"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "التطبيق يعمل بالنسخة الحقيقية الكاملة (بدون أي أنميات وهمية أو تجريبية). اربط صفحتك عبر (Page ID + Access Token) لسحب الفيديوهات والأنميات تلقائياً عبر *6، أو أضف الأنميات مباشرة من لوحة الإدارة أو قارئ المواقع.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onOpenAdminStudio,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FacebookBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connect_fb_server_cta")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فتح لوحة ربط فيسبوك *6 وإضافة الأنمي",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenWebReader,
                border = BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_web_reader_cta")
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = CrimsonNeon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "استيراد أنمي حقيقي عبر قارئ المواقع الشامل",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun HeroSpotlightBanner(
    anime: AnimeSeries,
    spotlightCount: Int,
    activeIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onWatchNow: () -> Unit,
    onOpenDetails: () -> Unit
) {
    GlassSurfaceCard(
        cornerRadius = 22.dp,
        borderColor = Color(anime.visualTheme.primaryColorHex).copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("hero_spotlight_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(275.dp)
        ) {
            AnimeZeroRatedPoster(
                visualTheme = anime.visualTheme,
                titleEn = anime.titleEn,
                posterFbcdnUrl = anime.posterFbcdnUrl,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Star6StreamBadge(text = "بث *6 المباشر • ${anime.episodes.size} حلقات")
                MalRatingBadge(score = anime.malScore)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    anime.genres.take(3).forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianElevated.copy(alpha = 0.85f),
                            border = BorderStroke(0.5.dp, GlassBorder)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (anime.studio.isNotBlank()) {
                        Text(
                            text = "• ${anime.studio} (${anime.releaseYear})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = anime.titleAr,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anime.synopsisAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onWatchNow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonNeon,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("hero_watch_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "شاهد على *6",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenDetails,
                            border = BorderStroke(1.dp, GlassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AmoledBlack.copy(alpha = 0.65f),
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("hero_details_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "التفاصيل",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 0 until spotlightCount) {
                            val selected = i == activeIndex
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 20.dp else 8.dp, 8.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) CrimsonNeon else TextSecondary.copy(alpha = 0.4f))
                                    .clickable { onSelectIndex(i) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestEpisodeCard(
    anime: AnimeSeries,
    episode: EpisodeItem,
    onClick: () -> Unit
) {
    GlassSurfaceCard(
        cornerRadius = 16.dp,
        onClick = onClick,
        modifier = Modifier
            .width(245.dp)
            .testTag("latest_ep_card_${episode.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
            ) {
                AnimeZeroRatedPoster(
                    visualTheme = anime.visualTheme,
                    titleEn = anime.titleEn,
                    posterFbcdnUrl = anime.posterFbcdnUrl,
                    modifier = Modifier.fillMaxSize()
                )
                FillerOrCanonBadge(
                    isFiller = episode.isFiller,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonNeon,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "الحلقة ${episode.number}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FacebookBlue.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل الحلقة",
                        tint = Color.White
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = anime.titleAr,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.titleAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (episode.facebookVideoId.isNotBlank()) "FB Video ID: ${episode.facebookVideoId}" else "رابط مباشر مضاف",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldStar6
                )
            }
        }
    }
}

@Composable
fun AnimePosterCard(
    anime: AnimeSeries,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurfaceCard(
        cornerRadius = 16.dp,
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .testTag("anime_card_${anime.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
            ) {
                AnimeZeroRatedPoster(
                    visualTheme = anime.visualTheme,
                    titleEn = anime.titleEn,
                    posterFbcdnUrl = anime.posterFbcdnUrl,
                    modifier = Modifier.fillMaxSize()
                )
                MalRatingBadge(
                    score = anime.malScore,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (anime.statusAr == "مستمر") EmeraldStar6.copy(alpha = 0.25f) else RoyalViolet.copy(alpha = 0.28f),
                    border = BorderStroke(
                        1.dp,
                        if (anime.statusAr == "مستمر") EmeraldStar6 else RoyalViolet
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = anime.statusAr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = anime.titleAr,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${anime.typeAr} • ${anime.episodes.size} حلقات • ${anime.studio}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TopRatedAnimeRowCard(
    rank: Int,
    anime: AnimeSeries,
    onClick: () -> Unit,
    onQuickPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurfaceCard(
        cornerRadius = 16.dp,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_rated_card_${anime.id}")
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
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (rank <= 3) CrimsonNeon.copy(alpha = 0.22f) else ObsidianElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (rank <= 3) CrimsonNeon else TextSecondary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 84.dp)
                    .clip(RoundedCornerShape(10.dp))
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
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anime.genres.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MalRatingBadge(score = anime.malScore)
                    Star6StreamBadge(text = "${anime.episodes.size} حلقات *6")
                }
            }

            Surface(
                shape = CircleShape,
                color = FacebookBlue.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, FacebookBlue),
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { onQuickPlay() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل سريع",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleScreen(
    catalog: List<AnimeSeries>,
    selectedDayAr: String,
    onSelectDay: (String) -> Unit,
    onSelectAnime: (AnimeSeries) -> Unit,
    onPlayEpisode: (AnimeSeries, EpisodeItem) -> Unit,
    onOpenAdminStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDays = remember { listOf("كل الأيام") + DemoAnimeCatalog.daysOfWeekAr }
    val filteredAnime = remember(catalog, selectedDayAr) {
        if (selectedDayAr == "كل الأيام") catalog
        else catalog.filter { it.airingDayAr == selectedDayAr }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("schedule_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassSurfaceCard(
                cornerRadius = 18.dp,
                borderColor = RoyalViolet.copy(alpha = 0.5f),
                containerColor = ObsidianSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CrimsonNeon
                        )
                        Text(
                            text = "جدول مواعيد عرض حلقات *6 الأسبوعي",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يعرض مواعيد الأنميات الحقيقية المضافة من سيرفر فيسبوك *6",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allDays) { day ->
                            val isSelected = day == selectedDayAr
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CrimsonNeon else ObsidianElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CrimsonNeon else GlassBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectDay(day) }
                                    .testTag("schedule_day_$day")
                            ) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (catalog.isEmpty()) {
            item {
                GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "جدول المواعيد بانتظار ربط السيرفر",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اربط صفحة فيسبوك *6 لسحب الأنميات ومواعيد عرضها تلقائياً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenAdminStudio,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FacebookBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("فتح لوحة ربط فيسبوك *6")
                        }
                    }
                }
            }
        } else if (filteredAnime.isEmpty()) {
            item {
                GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد عروض مسجلة ليوم $selectedDayAr",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "اختر يوماً آخر أو اضغط على (كل الأيام) لاستعراض الجدول الكامل.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredAnime, key = { it.id }) { anime ->
                GlassSurfaceCard(
                    cornerRadius = 16.dp,
                    onClick = { onSelectAnime(anime) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 74.dp, height = 98.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AnimeZeroRatedPoster(
                                visualTheme = anime.visualTheme,
                                titleEn = anime.titleEn,
                                posterFbcdnUrl = anime.posterFbcdnUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CrimsonNeon.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = anime.airingDayAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonNeon,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = anime.airingTimeAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = anime.titleAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = EmeraldStar6,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = anime.countdownText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldStar6,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                anime.episodes.lastOrNull()?.let { ep ->
                                    onPlayEpisode(anime, ep)
                                } ?: onSelectAnime(anime)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FacebookBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "ح ${anime.episodes.lastOrNull()?.number ?: 1}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 5.dp, height = 28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
