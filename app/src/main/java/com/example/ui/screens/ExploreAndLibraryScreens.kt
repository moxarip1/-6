package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.remember
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
import com.example.model.DownloadRecord
import com.example.model.LibrarySubTab
import com.example.model.WatchHistoryEntry
import com.example.model.WatchStatus
import com.example.ui.components.AnimeZeroRatedPoster
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.MalRatingBadge
import com.example.ui.components.Star6StreamBadge
import com.example.ui.components.formatSeconds
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
fun ExploreScreen(
    catalog: List<AnimeSeries>,
    searchQuery: String,
    selectedGenre: String,
    selectedStatus: String,
    selectedType: String,
    selectedSort: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onSelectStatus: (String) -> Unit,
    onSelectType: (String) -> Unit,
    onSelectSort: (String) -> Unit,
    onSelectAnime: (AnimeSeries) -> Unit,
    onOpenAdminStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredList = remember(
        catalog,
        searchQuery,
        selectedGenre,
        selectedStatus,
        selectedType,
        selectedSort
    ) {
        catalog.filter { anime ->
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                anime.titleAr.contains(q, ignoreCase = true) ||
                anime.titleEn.contains(q, ignoreCase = true) ||
                anime.synopsisAr.contains(q, ignoreCase = true) ||
                anime.studio.contains(q, ignoreCase = true)

            val matchesGenre = selectedGenre == "الكل" || anime.genres.contains(selectedGenre)
            val matchesStatus = selectedStatus == "الكل" || anime.statusAr == selectedStatus
            val matchesType = selectedType == "الكل" || anime.typeAr.equals(selectedType, ignoreCase = true)

            matchesQuery && matchesGenre && matchesStatus && matchesType
        }.let { list ->
            when (selectedSort) {
                "الأحدث" -> list.sortedByDescending { it.releaseYear }
                "الحلقات" -> list.sortedByDescending { it.episodes.size }
                else -> list.sortedByDescending { it.malScore }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassSurfaceCard(
                cornerRadius = 18.dp,
                borderColor = CrimsonNeon.copy(alpha = 0.45f),
                containerColor = ObsidianSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = "ابحث عن أنمي، قصة، أو استوديو...",
                                color = TextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = CrimsonNeon
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح البحث",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = ObsidianElevated,
                            unfocusedContainerColor = ObsidianElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explore_search_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = FacebookBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "فلترة أنمي سلاير المتقدمة (التصنيف • الحالة • النوع • الترتيب)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DemoAnimeCatalog.allGenresAr) { genre ->
                            FilterChipPill(
                                label = genre,
                                selected = selectedGenre == genre,
                                activeColor = CrimsonNeon,
                                onClick = { onSelectGenre(genre) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("الكل", "مستمر", "مكتمل").forEach { st ->
                            FilterChipPill(
                                label = "الحالة: $st",
                                selected = selectedStatus == st,
                                activeColor = EmeraldStar6,
                                onClick = { onSelectStatus(st) }
                            )
                        }
                        listOf("الكل", "TV", "Movie", "OVA").forEach { tp ->
                            FilterChipPill(
                                label = "النوع: $tp",
                                selected = selectedType == tp,
                                activeColor = RoyalViolet,
                                onClick = { onSelectType(tp) }
                            )
                        }
                        listOf("الأعلى تقييماً", "الأحدث", "الحلقات").forEach { srt ->
                            FilterChipPill(
                                label = "ترتيب: $srt",
                                selected = selectedSort == srt,
                                activeColor = FacebookBlue,
                                onClick = { onSelectSort(srt) }
                            )
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
                            text = "الاستكشاف بانتظار مزامنة السيرفر",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لا توجد أنميات مضافة بعد. اربط صفحة فيسبوك *6 لسحب مكتبة الأنمي الخاصة بك.",
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
        } else {
            item {
                Text(
                    text = "نتائج البحث والفلترة (${filteredList.size} أنمي)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            items(filteredList, key = { it.id }) { anime ->
                GlassSurfaceCard(
                    cornerRadius = 16.dp,
                    onClick = { onSelectAnime(anime) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_result_${anime.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 78.dp, height = 106.dp)
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
                                MalRatingBadge(score = anime.malScore)
                                Star6StreamBadge(text = "${anime.typeAr} • ${anime.releaseYear}")
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ObsidianElevated
                                ) {
                                    Text(
                                        text = anime.statusAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldStar6,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = anime.titleAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = anime.synopsisAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${anime.genres.joinToString(" • ")} | ${anime.episodes.size} حلقات",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonNeon
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeColor.copy(alpha = 0.25f) else ObsidianElevated,
        border = BorderStroke(1.dp, if (selected) activeColor else GlassBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun LibraryScreen(
    catalog: List<AnimeSeries>,
    activeSubTab: LibrarySubTab,
    watchStatusMap: Map<String, WatchStatus>,
    watchHistory: List<WatchHistoryEntry>,
    downloads: List<DownloadRecord>,
    onSelectSubTab: (LibrarySubTab) -> Unit,
    onSelectAnime: (AnimeSeries) -> Unit,
    onResumeHistoryEntry: (WatchHistoryEntry) -> Unit,
    onPlayDownloadRecord: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassSurfaceCard(
                cornerRadius = 18.dp,
                borderColor = RoyalViolet.copy(alpha = 0.45f),
                containerColor = ObsidianSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "مكتبتي وسجل المشاهدة والتحميلات (*6)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LibrarySubTab.entries) { subTab ->
                            val selected = subTab == activeSubTab
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) CrimsonNeon else ObsidianElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) CrimsonNeon else GlassBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectSubTab(subTab) }
                                    .testTag("library_subtab_${subTab.name}")
                            ) {
                                Text(
                                    text = subTab.titleAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        when (activeSubTab) {
            LibrarySubTab.WATCHING,
            LibrarySubTab.COMPLETED,
            LibrarySubTab.FAVORITES -> {
                val targetStatus = when (activeSubTab) {
                    LibrarySubTab.WATCHING -> WatchStatus.WATCHING
                    LibrarySubTab.COMPLETED -> WatchStatus.COMPLETED
                    else -> WatchStatus.FAVORITE
                }
                val matchingAnime = catalog.filter { watchStatusMap[it.id] == targetStatus }

                if (matchingAnime.isEmpty()) {
                    item {
                        EmptyLibraryNotice(
                            title = "قائمة (${activeSubTab.titleAr}) فارغة حالياً",
                            subtitle = "بعد ربط السيرفر وإضافة الأنميات، افتح أي أنمي وحدد حالته ليظهر هنا."
                        )
                    }
                } else {
                    items(matchingAnime, key = { it.id }) { anime ->
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
                                        .size(width = 66.dp, height = 88.dp)
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
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${anime.episodes.size} حلقات • ${anime.studio}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MalRatingBadge(score = anime.malScore)
                                }
                            }
                        }
                    }
                }
            }

            LibrarySubTab.HISTORY -> {
                if (watchHistory.isEmpty()) {
                    item {
                        EmptyLibraryNotice(
                            title = "لا يوجد سجل مشاهدة بعد",
                            subtitle = "شغّل أي حلقة بعد ربط السيرفر ليتم حفظ نقطة التوقف تلقائياً."
                        )
                    }
                } else {
                    items(watchHistory, key = { it.episodeId }) { entry ->
                        GlassSurfaceCard(
                            cornerRadius = 16.dp,
                            onClick = { onResumeHistoryEntry(entry) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.animeTitleAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الحلقة ${entry.episodeNumber}: ${entry.episodeTitleAr}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "توقفت عند ${formatSeconds(entry.lastPositionSec)} من ${formatSeconds(entry.durationSec)} • ${entry.updatedAtText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldStar6
                                    )
                                }
                                Button(
                                    onClick = { onResumeHistoryEntry(entry) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CrimsonNeon,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("استئناف", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            LibrarySubTab.DOWNLOADS -> {
                if (downloads.isEmpty()) {
                    item {
                        EmptyLibraryNotice(
                            title = "لا توجد تحميلات *6 حالياً",
                            subtitle = "يمكنك تحميل أي حلقة مباشرة بصيغة MP4 من شاشة تفاصيل الأنمي."
                        )
                    }
                } else {
                    items(downloads, key = { it.id }) { dl ->
                        GlassSurfaceCard(
                            cornerRadius = 16.dp,
                            onClick = { onPlayDownloadRecord(dl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = EmeraldStar6,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${dl.animeTitleAr} • الحلقة ${dl.episodeNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${dl.qualityLabel} • الحجم: ${dl.fileSizeMb} • ${dl.stateLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldStar6
                                    )
                                    Text(
                                        text = dl.fbcdnSourceUrl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = { onPlayDownloadRecord(dl) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FacebookBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تشغيل", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryNotice(title: String, subtitle: String) {
    GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
