package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MainTab
import com.example.model.WatchStatus
import com.example.ui.components.BihiStar6VideoPlayerCard
import com.example.ui.components.NetworkWhitelistTopBar
import com.example.ui.screens.AdminFacebookStudioSheet
import com.example.ui.screens.AnimeDetailScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.WebReaderScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CrimsonNeon
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AnimeBihiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel = remember {
                val prefs = context.getSharedPreferences("anime_bihi_prefs", Context.MODE_PRIVATE)
                AnimeBihiViewModel(prefs = prefs)
            }
            MyApplicationTheme {
                AnimeBihiApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AnimeBihiApp(viewModel: AnimeBihiViewModel) {
    val state by viewModel.uiState.collectAsState()

    BackHandler(enabled = state.isAdminStudioOpen || state.selectedAnimeId != null || state.activeTab != MainTab.HOME) {
        when {
            state.isAdminStudioOpen -> viewModel.setAdminStudioOpen(false)
            state.selectedAnimeId != null -> viewModel.closeAnimeDetails()
            state.activeTab != MainTab.HOME -> viewModel.selectTab(MainTab.HOME)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AmoledBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            NetworkWhitelistTopBar(
                networkMode = state.networkMode,
                isLiveFbConnected = state.facebookConfig.isLiveConnected,
                onToggleNetworkMode = { viewModel.toggleNetworkMode() },
                onOpenAdminStudio = { viewModel.setAdminStudioOpen(!state.isAdminStudioOpen) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = GlassBorder)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = !state.isAdminStudioOpen && state.selectedAnimeId == null && state.activeTab == tab
                    val icon = when (tab) {
                        MainTab.HOME -> Icons.Default.Home
                        MainTab.SCHEDULE -> Icons.Default.CalendarMonth
                        MainTab.EXPLORE -> Icons.Default.Explore
                        MainTab.WEB_READER -> Icons.Default.Language
                        MainTab.LIBRARY -> Icons.Default.VideoLibrary
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.titleAr
                            )
                        },
                        label = {
                            Text(
                                text = tab.titleAr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = CrimsonNeon,
                            indicatorColor = CrimsonNeon.copy(alpha = 0.25f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val playerAnime = state.activePlayerAnime
            val playerEpisode = state.activePlayerEpisode
            AnimatedVisibility(visible = playerAnime != null && playerEpisode != null) {
                if (playerAnime != null && playerEpisode != null) {
                    val sortedEps = playerAnime.episodes.sortedBy { it.number }
                    val epIdx = sortedEps.indexOfFirst { it.id == playerEpisode.id }
                    BihiStar6VideoPlayerCard(
                        anime = playerAnime,
                        episode = playerEpisode,
                        resolvedStream = state.resolvedStream,
                        selectedQuality = state.selectedQuality,
                        playbackSpeed = state.playbackSpeed,
                        initialPositionSec = state.resumePositions[playerEpisode.id] ?: 0,
                        hasPreviousEpisode = epIdx > 0,
                        hasNextEpisode = epIdx >= 0 && epIdx < sortedEps.size - 1,
                        onSelectQuality = { viewModel.selectStreamQuality(it) },
                        onSelectSpeed = { viewModel.selectPlaybackSpeed(it) },
                        onSaveProgress = { viewModel.saveEpisodeProgress(it) },
                        onPreviousEpisode = { viewModel.playAdjacentEpisode(next = false) },
                        onNextEpisode = { viewModel.playAdjacentEpisode(next = true) },
                        onClosePlayer = { viewModel.closeVideoPlayer() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                val detailAnime = state.selectedAnime
                when {
                    state.isAdminStudioOpen -> {
                        AdminFacebookStudioSheet(
                            config = state.facebookConfig,
                            lastTokenValidation = state.lastTokenValidation,
                            isSyncing = state.isSyncingFacebook,
                            generatedJsonBlock = state.generatedJsonBlock,
                            onUpdateConfig = { pageId, token, postId, mode ->
                                viewModel.updateFacebookConfig(pageId, token, postId, mode)
                            },
                            onValidateTokenOnly = { viewModel.validateAccessTokenOnly() },
                            onTestAndSyncNow = { viewModel.testAndSyncFacebookNow() },
                            onImportRawCatalogJson = { rawJson ->
                                viewModel.importRawMasterCatalogJson(rawJson)
                            },
                            onClearAllCatalogData = {
                                viewModel.clearAllCatalogData()
                            },
                            onGenerateEpisodeJson = { title, story, genres, status, epNum, filler, vidId, directUrl ->
                                viewModel.generateAdminEpisodeJson(
                                    title,
                                    story,
                                    genres,
                                    status,
                                    epNum,
                                    filler,
                                    vidId,
                                    directUrl
                                )
                            },
                            onPublishGeneratedToLocalCatalog = { title, story, genres, status, epNum, filler, vidId, directUrl ->
                                viewModel.publishGeneratedEpisodeToCatalog(
                                    title,
                                    story,
                                    genres,
                                    status,
                                    epNum,
                                    filler,
                                    vidId,
                                    directUrl
                                )
                            },
                            onClose = { viewModel.setAdminStudioOpen(false) }
                        )
                    }

                    detailAnime != null -> {
                        AnimeDetailScreen(
                            anime = detailAnime,
                            currentWatchStatus = state.watchStatusMap[detailAnime.id] ?: WatchStatus.NONE,
                            watchedEpisodeIds = state.watchedEpisodeIds,
                            downloadedEpisodeIds = state.downloads.map { it.episodeId }.toSet(),
                            resumePositions = state.resumePositions,
                            onBack = { viewModel.closeAnimeDetails() },
                            onUpdateWatchStatus = { status ->
                                viewModel.updateAnimeWatchStatus(detailAnime.id, status)
                            },
                            onToggleEpisodeWatched = { epId ->
                                viewModel.toggleEpisodeWatched(epId)
                            },
                            onDownloadEpisode = { anime, ep ->
                                viewModel.downloadEpisodeOnStar6(anime, ep)
                            },
                            onPlayEpisode = { anime, ep ->
                                viewModel.playEpisode(anime, ep)
                            }
                        )
                    }

                    else -> {
                        when (state.activeTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    catalog = state.catalog,
                                    isLiveConnected = state.facebookConfig.isLiveConnected,
                                    connectedPageName = state.facebookConfig.connectedPageName,
                                    onSelectAnime = { viewModel.openAnimeDetails(it) },
                                    onPlayEpisode = { anime, ep ->
                                        viewModel.openAnimeDetails(anime)
                                        viewModel.playEpisode(anime, ep)
                                    },
                                    onOpenAdminStudio = { viewModel.setAdminStudioOpen(true) },
                                    onOpenWebReader = { viewModel.selectTab(MainTab.WEB_READER) }
                                )
                            }

                            MainTab.SCHEDULE -> {
                                ScheduleScreen(
                                    catalog = state.catalog,
                                    selectedDayAr = state.selectedScheduleDayAr,
                                    onSelectDay = { viewModel.selectScheduleDay(it) },
                                    onSelectAnime = { viewModel.openAnimeDetails(it) },
                                    onPlayEpisode = { anime, ep ->
                                        viewModel.openAnimeDetails(anime)
                                        viewModel.playEpisode(anime, ep)
                                    },
                                    onOpenAdminStudio = { viewModel.setAdminStudioOpen(true) }
                                )
                            }

                            MainTab.EXPLORE -> {
                                ExploreScreen(
                                    catalog = state.catalog,
                                    searchQuery = state.searchQuery,
                                    selectedGenre = state.selectedGenre,
                                    selectedStatus = state.selectedStatus,
                                    selectedType = state.selectedType,
                                    selectedSort = state.selectedSort,
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                    onSelectGenre = { viewModel.selectGenreFilter(it) },
                                    onSelectStatus = { viewModel.selectStatusFilter(it) },
                                    onSelectType = { viewModel.selectTypeFilter(it) },
                                    onSelectSort = { viewModel.selectSortOption(it) },
                                    onSelectAnime = { viewModel.openAnimeDetails(it) },
                                    onOpenAdminStudio = { viewModel.setAdminStudioOpen(true) }
                                )
                            }

                            MainTab.WEB_READER -> {
                                WebReaderScreen(
                                    customSources = state.customScraperSources,
                                    scrapedResult = state.scrapedResult,
                                    isScraping = state.isScraping,
                                    statusBanner = state.scraperStatusBanner,
                                    errorBanner = state.scraperErrorBanner,
                                    onRunScraper = { url, cardSel, epSel, streamSel ->
                                        viewModel.runWebScraper(url, cardSel, epSel, streamSel)
                                    },
                                    onSaveCustomSource = { name, url, cardSel, epSel, streamSel ->
                                        viewModel.saveCustomScraperSource(name, url, cardSel, epSel, streamSel)
                                    },
                                    onImportScrapedAnimeToApp = { scraped ->
                                        viewModel.importScrapedAnimeIntoCatalog(scraped)
                                    }
                                )
                            }

                            MainTab.LIBRARY -> {
                                LibraryScreen(
                                    catalog = state.catalog,
                                    activeSubTab = state.activeLibrarySubTab,
                                    watchStatusMap = state.watchStatusMap,
                                    watchHistory = state.watchHistory,
                                    downloads = state.downloads,
                                    onSelectSubTab = { viewModel.selectLibrarySubTab(it) },
                                    onSelectAnime = { viewModel.openAnimeDetails(it) },
                                    onResumeHistoryEntry = { viewModel.resumeFromHistory(it) },
                                    onPlayDownloadRecord = { viewModel.playFromDownload(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
