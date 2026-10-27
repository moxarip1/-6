package com.example.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BihiWebScraperService
import com.example.data.DemoAnimeCatalog
import com.example.data.FacebookGraphService
import com.example.model.AnimeSeries
import com.example.model.AnimeVisualTheme
import com.example.model.CustomScraperSource
import com.example.model.DownloadRecord
import com.example.model.EpisodeItem
import com.example.model.FacebookConfig
import com.example.model.FacebookSyncMode
import com.example.model.LibrarySubTab
import com.example.model.MainTab
import com.example.model.NetworkMode
import com.example.model.PosterEmblemStyle
import com.example.model.ResolvedStreamResult
import com.example.model.ScrapedAnimeResult
import com.example.model.StreamQuality
import com.example.model.TokenValidationResult
import com.example.model.WatchHistoryEntry
import com.example.model.WatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnimeBihiUiState(
    val networkMode: NetworkMode = NetworkMode.STAR6_FBCDN,
    val activeTab: MainTab = MainTab.HOME,
    // Catalog remains strictly empty until user authenticates via Admin Dashboard
    val catalog: List<AnimeSeries> = emptyList(),
    val selectedAnimeId: String? = null,
    val isAdminStudioOpen: Boolean = false,
    // Schedule state
    val selectedScheduleDayAr: String = "كل الأيام",
    // Explore filter state
    val searchQuery: String = "",
    val selectedGenre: String = "الكل",
    val selectedStatus: String = "الكل",
    val selectedType: String = "الكل",
    val selectedSort: String = "الأعلى تقييماً",
    // Library state
    val activeLibrarySubTab: LibrarySubTab = LibrarySubTab.WATCHING,
    val watchStatusMap: Map<String, WatchStatus> = emptyMap(),
    val watchedEpisodeIds: Set<String> = emptySet(),
    val resumePositions: Map<String, Int> = emptyMap(),
    val watchHistory: List<WatchHistoryEntry> = emptyList(),
    val downloads: List<DownloadRecord> = emptyList(),
    // Built-in *6 Video Player state
    val activePlayerAnimeId: String? = null,
    val activePlayerEpisodeId: String? = null,
    val resolvedStream: ResolvedStreamResult? = null,
    val selectedQuality: StreamQuality = StreamQuality.SD_480P,
    val playbackSpeed: Float = 1.0f,
    // Web-Reader Scraper state
    val customScraperSources: List<CustomScraperSource> = emptyList(),
    val scrapedResult: ScrapedAnimeResult? = null,
    val isScraping: Boolean = false,
    val scraperStatusBanner: String? = null,
    val scraperErrorBanner: String? = null,
    // Admin Studio & FacebookGraphService Authentication state
    val facebookConfig: FacebookConfig = FacebookConfig(),
    val lastTokenValidation: TokenValidationResult? = null,
    val isSyncingFacebook: Boolean = false,
    val generatedJsonBlock: String = ""
) {
    val selectedAnime: AnimeSeries?
        get() = catalog.find { it.id == selectedAnimeId }

    val activePlayerAnime: AnimeSeries?
        get() = catalog.find { it.id == activePlayerAnimeId }

    val activePlayerEpisode: EpisodeItem?
        get() = activePlayerAnime?.episodes?.find { it.id == activePlayerEpisodeId }
}

class AnimeBihiViewModel(
    private val prefs: SharedPreferences? = null,
    private val graphService: FacebookGraphService = FacebookGraphService(),
    private val scraperService: BihiWebScraperService = BihiWebScraperService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeBihiUiState())
    val uiState: StateFlow<AnimeBihiUiState> = _uiState.asStateFlow()

    init {
        val savedPageId = prefs?.getString("fb_page_id", "").orEmpty()
        val savedToken = prefs?.getString("fb_access_token", "").orEmpty()
        val savedPostId = prefs?.getString("fb_catalog_post_id", "").orEmpty()
        val savedModeStr = prefs?.getString("fb_sync_mode", FacebookSyncMode.AUTO_VIDEO_DESCRIPTIONS.name)
            ?: FacebookSyncMode.AUTO_VIDEO_DESCRIPTIONS.name
        val savedMode = runCatching { FacebookSyncMode.valueOf(savedModeStr) }
            .getOrDefault(FacebookSyncMode.AUTO_VIDEO_DESCRIPTIONS)

        val persistedWatchStatus = DemoAnimeCatalog.deserializeWatchStatuses(
            prefs?.getString("saved_watch_statuses", null)
        )
        val persistedWatchedEps = prefs?.getStringSet("saved_watched_eps", emptySet()).orEmpty()
        val persistedResumeMap = DemoAnimeCatalog.deserializeResumeMap(
            prefs?.getString("saved_resume_map", null)
        )
        val persistedHistory = DemoAnimeCatalog.deserializeHistory(
            prefs?.getString("saved_watch_history", null)
        )
        val persistedDownloads = DemoAnimeCatalog.deserializeDownloads(
            prefs?.getString("saved_downloads", null)
        )
        val persistedSources = DemoAnimeCatalog.deserializeCustomSources(
            prefs?.getString("saved_custom_sources", null)
        )

        // Note: Catalog starts strictly empty (emptyList()) until authenticated via FacebookGraphService
        _uiState.update { state ->
            state.copy(
                catalog = emptyList(),
                watchStatusMap = persistedWatchStatus,
                watchedEpisodeIds = persistedWatchedEps,
                resumePositions = persistedResumeMap,
                watchHistory = persistedHistory,
                downloads = persistedDownloads,
                customScraperSources = persistedSources,
                facebookConfig = state.facebookConfig.copy(
                    pageId = savedPageId,
                    accessToken = savedToken,
                    catalogPostId = savedPostId,
                    syncMode = savedMode,
                    isLiveConnected = false
                )
            )
        }
    }

    private fun persistCatalog(catalog: List<AnimeSeries>) {
        prefs?.edit()?.putString("saved_catalog_json", DemoAnimeCatalog.serializeCatalogToJson(catalog))?.apply()
    }

    fun toggleNetworkMode() {
        _uiState.update { state ->
            val nextMode = if (state.networkMode == NetworkMode.STAR6_FBCDN) {
                NetworkMode.STANDARD_WIFI
            } else {
                NetworkMode.STAR6_FBCDN
            }
            val nextQuality = if (nextMode == NetworkMode.STAR6_FBCDN) {
                StreamQuality.SD_480P
            } else {
                StreamQuality.HD_720P
            }
            state.copy(networkMode = nextMode, selectedQuality = nextQuality)
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                selectedAnimeId = null,
                isAdminStudioOpen = false
            )
        }
    }

    fun openAnimeDetails(anime: AnimeSeries) {
        _uiState.update {
            it.copy(
                selectedAnimeId = anime.id,
                isAdminStudioOpen = false
            )
        }
    }

    fun closeAnimeDetails() {
        _uiState.update { it.copy(selectedAnimeId = null) }
    }

    fun setAdminStudioOpen(open: Boolean) {
        _uiState.update { it.copy(isAdminStudioOpen = open) }
    }

    fun selectScheduleDay(dayAr: String) {
        _uiState.update { it.copy(selectedScheduleDayAr = dayAr) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectGenreFilter(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
    }

    fun selectStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun selectTypeFilter(type: String) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun selectSortOption(sort: String) {
        _uiState.update { it.copy(selectedSort = sort) }
    }

    fun selectLibrarySubTab(subTab: LibrarySubTab) {
        _uiState.update { it.copy(activeLibrarySubTab = subTab) }
    }

    fun updateAnimeWatchStatus(animeId: String, status: WatchStatus) {
        _uiState.update { state ->
            val nextMap = state.watchStatusMap.toMutableMap()
            if (status == WatchStatus.NONE) {
                nextMap.remove(animeId)
            } else {
                nextMap[animeId] = status
            }
            prefs?.edit()?.putString("saved_watch_statuses", DemoAnimeCatalog.serializeWatchStatuses(nextMap))?.apply()
            state.copy(watchStatusMap = nextMap)
        }
    }

    fun toggleEpisodeWatched(episodeId: String) {
        _uiState.update { state ->
            val nextSet = state.watchedEpisodeIds.toMutableSet()
            if (!nextSet.add(episodeId)) {
                nextSet.remove(episodeId)
            }
            prefs?.edit()?.putStringSet("saved_watched_eps", nextSet)?.apply()
            state.copy(watchedEpisodeIds = nextSet)
        }
    }

    fun downloadEpisodeOnStar6(anime: AnimeSeries, episode: EpisodeItem) {
        val state = _uiState.value
        if (state.downloads.any { it.episodeId == episode.id }) return

        viewModelScope.launch {
            val resolved = graphService.resolveVideoStream(episode, state.facebookConfig.accessToken)
            val directUrl = resolved.defaultUrl.ifBlank { episode.defaultSourceUrl }
            if (directUrl.isBlank()) return@launch

            _uiState.update { current ->
                val newRecord = DownloadRecord(
                    id = "dl_${episode.id}",
                    episodeId = episode.id,
                    animeId = anime.id,
                    animeTitleAr = anime.titleAr,
                    episodeNumber = episode.number,
                    qualityLabel = current.selectedQuality.labelAr,
                    fileSizeMb = if (current.selectedQuality == StreamQuality.SD_480P) "480p SD" else "HD MP4",
                    fbcdnSourceUrl = directUrl
                )
                val updated = listOf(newRecord) + current.downloads
                prefs?.edit()?.putString("saved_downloads", DemoAnimeCatalog.serializeDownloads(updated))?.apply()
                current.copy(downloads = updated)
            }
        }
    }

    fun playEpisode(anime: AnimeSeries, episode: EpisodeItem) {
        _uiState.update { state ->
            val nextWatched = state.watchedEpisodeIds + episode.id
            prefs?.edit()?.putStringSet("saved_watched_eps", nextWatched)?.apply()
            state.copy(
                activePlayerAnimeId = anime.id,
                activePlayerEpisodeId = episode.id,
                resolvedStream = null,
                watchedEpisodeIds = nextWatched
            )
        }
        viewModelScope.launch {
            val token = _uiState.value.facebookConfig.accessToken
            val resolved = graphService.resolveVideoStream(episode, token)
            _uiState.update { it.copy(resolvedStream = resolved) }
        }
    }

    fun saveEpisodeProgress(positionSec: Int) {
        val state = _uiState.value
        val anime = state.activePlayerAnime ?: return
        val ep = state.activePlayerEpisode ?: return

        _uiState.update { current ->
            val updatedPositions = current.resumePositions + (ep.id to positionSec)
            val newEntry = WatchHistoryEntry(
                animeId = anime.id,
                animeTitleAr = anime.titleAr,
                episodeId = ep.id,
                episodeNumber = ep.number,
                episodeTitleAr = ep.titleAr,
                lastPositionSec = positionSec,
                durationSec = ep.durationSeconds,
                updatedAtText = "عبر *6 (${current.selectedQuality.shortTag})"
            )
            val updatedHistory = listOf(newEntry) + current.watchHistory.filterNot { it.episodeId == ep.id }
            prefs?.edit()
                ?.putString("saved_resume_map", DemoAnimeCatalog.serializeResumeMap(updatedPositions))
                ?.putString("saved_watch_history", DemoAnimeCatalog.serializeHistory(updatedHistory))
                ?.apply()
            current.copy(
                resumePositions = updatedPositions,
                watchHistory = updatedHistory
            )
        }
    }

    fun selectStreamQuality(quality: StreamQuality) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun selectPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun playAdjacentEpisode(next: Boolean) {
        val state = _uiState.value
        val anime = state.activePlayerAnime ?: return
        val currentEp = state.activePlayerEpisode ?: return
        val sorted = anime.episodes.sortedBy { it.number }
        val idx = sorted.indexOfFirst { it.id == currentEp.id }
        if (idx == -1) return
        val target = if (next) sorted.getOrNull(idx + 1) else sorted.getOrNull(idx - 1)
        if (target != null) {
            playEpisode(anime, target)
        }
    }

    fun closeVideoPlayer() {
        _uiState.update {
            it.copy(
                activePlayerAnimeId = null,
                activePlayerEpisodeId = null,
                resolvedStream = null
            )
        }
    }

    fun resumeFromHistory(entry: WatchHistoryEntry) {
        val anime = _uiState.value.catalog.find { it.id == entry.animeId } ?: return
        val ep = anime.episodes.find { it.id == entry.episodeId } ?: anime.episodes.firstOrNull() ?: return
        openAnimeDetails(anime)
        playEpisode(anime, ep)
    }

    fun playFromDownload(record: DownloadRecord) {
        val anime = _uiState.value.catalog.find { it.id == record.animeId } ?: return
        val ep = anime.episodes.find { it.id == record.episodeId } ?: anime.episodes.firstOrNull() ?: return
        openAnimeDetails(anime)
        playEpisode(anime, ep)
    }

    // Core Engine 2: Web-Reader Actions
    fun runWebScraper(url: String, cardSel: String, epSel: String, streamSel: String) {
        _uiState.update {
            it.copy(
                isScraping = true,
                scraperStatusBanner = null,
                scraperErrorBanner = null
            )
        }
        viewModelScope.launch {
            when (val outcome = scraperService.scrapeAnimeUrl(url, cardSel, epSel, streamSel)) {
                is BihiWebScraperService.ScrapeOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            scrapedResult = outcome.result,
                            isScraping = false,
                            scraperErrorBanner = null
                        )
                    }
                }
                is BihiWebScraperService.ScrapeOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            isScraping = false,
                            scraperErrorBanner = outcome.message
                        )
                    }
                }
            }
        }
    }

    fun saveCustomScraperSource(
        name: String,
        url: String,
        cardSel: String,
        epSel: String,
        streamSel: String
    ) {
        val cleanName = name.trim().ifEmpty { "موقع مخصص" }
        val newSrc = CustomScraperSource(
            id = "custom_src_${System.currentTimeMillis()}",
            siteName = cleanName,
            baseUrl = url.trim(),
            cardSelector = cardSel.trim(),
            episodeSelector = epSel.trim(),
            streamSelector = streamSel.trim()
        )
        _uiState.update { state ->
            val updated = listOf(newSrc) + state.customScraperSources
            prefs?.edit()?.putString("saved_custom_sources", DemoAnimeCatalog.serializeCustomSources(updated))?.apply()
            state.copy(
                customScraperSources = updated,
                scraperStatusBanner = "تم حفظ ($cleanName) كبطاقة مصدر مخصصة."
            )
        }
    }

    fun importScrapedAnimeIntoCatalog(scraped: ScrapedAnimeResult) {
        val state = _uiState.value
        if (!state.facebookConfig.isLiveConnected) {
            _uiState.update {
                it.copy(
                    scraperErrorBanner = "الكتالوج مقفل: يجب مصادقة Page Access Token في لوحة الإدارة أولاً قبل إضافة الأنميات للكتالوج."
                )
            }
            return
        }

        _uiState.update { current ->
            val existingIdx = current.catalog.indexOfFirst {
                it.id == scraped.id || it.titleAr == scraped.titleAr
            }
            val newAnimeId = if (existingIdx >= 0) current.catalog[existingIdx].id else scraped.id
            val mappedEpisodes = scraped.episodes.map { sEp ->
                EpisodeItem(
                    id = "${newAnimeId}_ep_${sEp.number}",
                    animeId = newAnimeId,
                    number = sEp.number,
                    titleAr = sEp.title,
                    facebookVideoId = sEp.suggestedFbVideoId,
                    durationSeconds = 1430,
                    isFiller = false,
                    releaseDate = "2025",
                    defaultSourceUrl = sEp.streamUrl,
                    formats = DemoAnimeCatalog.buildFormatsFromSource(sEp.streamUrl)
                )
            }
            val newSeries = AnimeSeries(
                id = newAnimeId,
                titleAr = scraped.titleAr,
                titleEn = scraped.titleEn,
                synopsisAr = scraped.synopsisAr,
                genres = scraped.genres,
                studio = scraped.studio,
                releaseYear = scraped.releaseYear,
                statusAr = scraped.statusAr,
                typeAr = scraped.typeAr,
                malScore = scraped.malScore,
                airingDayAr = "السبت",
                airingTimeAr = "20:00 بتوقيت مكة",
                countdownText = "مستورد عبر قارئ المواقع",
                posterFbcdnUrl = scraped.posterUrl,
                visualTheme = AnimeVisualTheme(
                    primaryColorHex = 0xFF1877F2,
                    secondaryColorHex = 0xFF0A1124,
                    accentColorHex = 0xFFFF2A54,
                    emblemStyle = PosterEmblemStyle.CYBER_SHINOBI
                ),
                isSeasonal = true,
                isSpotlight = true,
                episodes = mappedEpisodes
            )
            val updatedCatalog = if (existingIdx >= 0) {
                current.catalog.toMutableList().apply { set(existingIdx, newSeries) }
            } else {
                listOf(newSeries) + current.catalog
            }
            persistCatalog(updatedCatalog)
            current.copy(
                catalog = updatedCatalog,
                scraperStatusBanner = "تم استيراد (${scraped.titleAr}) مع ${mappedEpisodes.size} حلقات إلى مكتبة التطبيق."
            )
        }
    }

    // Core Engine 1 & Admin Studio Actions (FacebookGraphService)
    fun updateFacebookConfig(
        pageId: String,
        accessToken: String,
        catalogPostId: String,
        syncMode: FacebookSyncMode
    ) {
        prefs?.edit()
            ?.putString("fb_page_id", pageId.trim())
            ?.putString("fb_access_token", accessToken.trim())
            ?.putString("fb_catalog_post_id", catalogPostId.trim())
            ?.putString("fb_sync_mode", syncMode.name)
            ?.apply()

        _uiState.update { state ->
            state.copy(
                facebookConfig = state.facebookConfig.copy(
                    pageId = pageId,
                    accessToken = accessToken,
                    catalogPostId = catalogPostId,
                    syncMode = syncMode
                )
            )
        }
    }

    /**
     * Validates only the Facebook Page Access Token via [FacebookGraphService.validatePageAccessToken].
     */
    fun validateAccessTokenOnly() {
        val currentConfig = _uiState.value.facebookConfig
        _uiState.update { it.copy(isSyncingFacebook = true) }

        viewModelScope.launch {
            val validation = graphService.validatePageAccessToken(
                pageId = currentConfig.pageId,
                accessToken = currentConfig.accessToken
            )
            _uiState.update { state ->
                state.copy(
                    isSyncingFacebook = false,
                    lastTokenValidation = validation,
                    catalog = if (validation.isValid) state.catalog else emptyList(),
                    facebookConfig = state.facebookConfig.copy(
                        connectedPageName = if (validation.isValid) {
                            validation.verifiedPageName
                        } else {
                            "غير مصادق (الكتالوج مقفل وفارغ)"
                        },
                        pageCategory = validation.pageCategory,
                        isLiveConnected = validation.isValid,
                        lastSyncTime = validation.validatedAt,
                        statusMessage = validation.messageAr
                    )
                )
            }
        }
    }

    /**
     * Authenticates the Page Access Token via [FacebookGraphService.validatePageAccessToken]
     * and, if valid, fetches the JSON catalog via [FacebookGraphService.fetchJsonCatalogData].
     * If authentication fails, the catalog remains strictly empty.
     */
    fun testAndSyncFacebookNow() {
        val currentConfig = _uiState.value.facebookConfig
        _uiState.update { it.copy(isSyncingFacebook = true) }

        viewModelScope.launch {
            val (validation, fetchResult) = graphService.authenticateAndSyncCatalog(currentConfig)
            if (validation.isValid) {
                prefs?.edit()?.putString("fb_page_name", validation.verifiedPageName)?.apply()
                persistCatalog(fetchResult.catalog)
            } else {
                prefs?.edit()?.remove("saved_catalog_json")?.apply()
            }

            _uiState.update { state ->
                state.copy(
                    isSyncingFacebook = false,
                    lastTokenValidation = validation,
                    catalog = if (validation.isValid) fetchResult.catalog else emptyList(),
                    facebookConfig = state.facebookConfig.copy(
                        connectedPageName = if (validation.isValid) {
                            validation.verifiedPageName
                        } else {
                            "غير مصادق (الكتالوج مقفل وفارغ)"
                        },
                        pageCategory = validation.pageCategory,
                        isLiveConnected = validation.isValid,
                        lastSyncTime = fetchResult.fetchedAt,
                        statusMessage = fetchResult.messageAr
                    )
                )
            }
        }
    }

    fun importRawMasterCatalogJson(rawJson: String) {
        if (!_uiState.value.facebookConfig.isLiveConnected || !graphService.isAuthenticated) {
            _uiState.update { state ->
                state.copy(
                    facebookConfig = state.facebookConfig.copy(
                        statusMessage = "رفض الاستيراد: يجب مصادقة Page Access Token عبر لوحة الإدارة أولاً قبل تحميل أي كتالوج JSON."
                    )
                )
            }
            return
        }
        val parsed = graphService.parseMasterPostJson(rawJson)
        if (parsed.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    facebookConfig = state.facebookConfig.copy(
                        statusMessage = "كود JSON المدخل غير صالح أو لا يحتوي على مصفوفة catalog."
                    )
                )
            }
            return
        }
        _uiState.update { state ->
            val merged = parsed + state.catalog.filterNot { old -> parsed.any { it.id == old.id } }
            persistCatalog(merged)
            state.copy(
                catalog = merged,
                facebookConfig = state.facebookConfig.copy(
                    statusMessage = "تم استيراد ${parsed.size} أنمي من كود JSON بعد التحقق من المصادقة."
                )
            )
        }
    }

    /**
     * Logs out from the Facebook Graph session and empties the catalog.
     */
    fun clearAllCatalogData() {
        graphService.clearAuthenticationSession()
        prefs?.edit()?.remove("saved_catalog_json")?.apply()
        _uiState.update { state ->
            state.copy(
                catalog = emptyList(),
                selectedAnimeId = null,
                activePlayerAnimeId = null,
                activePlayerEpisodeId = null,
                lastTokenValidation = null,
                facebookConfig = state.facebookConfig.copy(
                    connectedPageName = "غير مصادق (الكتالوج مقفل وفارغ)",
                    isLiveConnected = false,
                    statusMessage = "تم تسجيل الخروج وتصفير الكتالوج • التطبيق بانتظار المصادقة عبر لوحة الإدارة."
                )
            )
        }
    }

    fun generateAdminEpisodeJson(
        animeTitle: String,
        storyAr: String,
        genresCsv: String,
        statusAr: String,
        epNumber: Int,
        isFiller: Boolean,
        fbVideoId: String,
        directSourceUrl: String
    ) {
        val json = graphService.generateVideoDescriptionJson(
            animeTitle = animeTitle,
            storyAr = storyAr,
            genresCsv = genresCsv,
            statusAr = statusAr,
            episodeNumber = epNumber,
            isFiller = isFiller,
            facebookVideoId = fbVideoId,
            directSourceUrl = directSourceUrl
        )
        _uiState.update { it.copy(generatedJsonBlock = json) }
    }

    fun publishGeneratedEpisodeToCatalog(
        animeTitle: String,
        storyAr: String,
        genresCsv: String,
        statusAr: String,
        epNumber: Int,
        isFiller: Boolean,
        fbVideoId: String,
        directSourceUrl: String
    ) {
        if (!_uiState.value.facebookConfig.isLiveConnected || !graphService.isAuthenticated) {
            _uiState.update { state ->
                state.copy(
                    facebookConfig = state.facebookConfig.copy(
                        statusMessage = "الكتالوج مقفل: يرجى مصادقة Page Access Token أولاً لتفعيل إضافة الحلقات."
                    )
                )
            }
            return
        }

        val cleanTitle = animeTitle.trim()
        val cleanVidId = fbVideoId.trim()
        val cleanUrl = directSourceUrl.trim()
        if (cleanTitle.isEmpty() || (cleanVidId.isEmpty() && cleanUrl.isEmpty())) return

        generateAdminEpisodeJson(
            animeTitle,
            storyAr,
            genresCsv,
            statusAr,
            epNumber,
            isFiller,
            fbVideoId,
            directSourceUrl
        )

        _uiState.update { state ->
            val existingIdx = state.catalog.indexOfFirst {
                it.titleAr.equals(cleanTitle, ignoreCase = true)
            }
            val targetAnimeId = if (existingIdx >= 0) {
                state.catalog[existingIdx].id
            } else {
                "anime_fb_${cleanTitle.hashCode().toUInt()}"
            }
            val newEp = EpisodeItem(
                id = "${targetAnimeId}_ep_$epNumber",
                animeId = targetAnimeId,
                number = epNumber,
                titleAr = "الحلقة $epNumber",
                facebookVideoId = cleanVidId,
                durationSeconds = 1440,
                isFiller = isFiller,
                releaseDate = "2025",
                defaultSourceUrl = cleanUrl,
                formats = DemoAnimeCatalog.buildFormatsFromSource(cleanUrl)
            )

            val updatedCatalog = if (existingIdx >= 0) {
                val existingAnime = state.catalog[existingIdx]
                val mergedEps = (existingAnime.episodes.filterNot { it.number == epNumber } + newEp)
                    .sortedBy { it.number }
                state.catalog.toMutableList().apply {
                    set(existingIdx, existingAnime.copy(episodes = mergedEps))
                }
            } else {
                val genres = genresCsv.split("،", ",").map { it.trim() }.filter { it.isNotEmpty() }
                    .ifEmpty { listOf("أنمي") }
                val newSeries = AnimeSeries(
                    id = targetAnimeId,
                    titleAr = cleanTitle,
                    titleEn = cleanTitle,
                    synopsisAr = storyAr.trim(),
                    genres = genres,
                    studio = state.facebookConfig.connectedPageName,
                    releaseYear = 2025,
                    statusAr = statusAr.ifBlank { "مستمر" },
                    typeAr = "TV",
                    malScore = 8.5,
                    airingDayAr = "السبت",
                    airingTimeAr = "20:00 بتوقيت مكة",
                    countdownText = "مضاف على سيرفر *6",
                    visualTheme = AnimeVisualTheme(
                        primaryColorHex = 0xFFFF2A54,
                        secondaryColorHex = 0xFF10131C,
                        accentColorHex = 0xFF1877F2,
                        emblemStyle = PosterEmblemStyle.CRIMSON_KATANA
                    ),
                    isSeasonal = true,
                    isSpotlight = true,
                    episodes = listOf(newEp)
                )
                listOf(newSeries) + state.catalog
            }

            persistCatalog(updatedCatalog)
            state.copy(
                catalog = updatedCatalog,
                facebookConfig = state.facebookConfig.copy(
                    statusMessage = "تمت إضافة الحلقة $epNumber من ($cleanTitle) إلى الكتالوج الموثق."
                )
            )
        }
    }
}
