package com.example.data

import com.example.model.AnimeSeries
import com.example.model.AnimeVisualTheme
import com.example.model.CatalogFetchResult
import com.example.model.EpisodeItem
import com.example.model.FacebookConfig
import com.example.model.FacebookSyncMode
import com.example.model.PosterEmblemStyle
import com.example.model.ResolvedStreamResult
import com.example.model.StreamQuality
import com.example.model.TokenValidationResult
import com.example.model.VideoStreamFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Core Engine 1: Facebook *6 Graph API Backend Service.
 *
 * Strictly enforces an authentication gate:
 * - Starts unauthenticated (`isAuthenticated == false`) with an empty catalog (`emptyList()`).
 * - Provides [validatePageAccessToken] to verify the user's Facebook Page ID and Permanent Page Access Token
 *   against `https://graph.facebook.com/v19.0`.
 * - Provides [fetchJsonCatalogData] to fetch and parse the JSON anime catalog from either Page videos
 *   (`/{pageId}/videos`) or a Master Catalog Post (`/{catalogPostId}`), returning `emptyList()` whenever
 *   the session has not been authenticated via the Admin Dashboard.
 */
class FacebookGraphService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val GRAPH_API_BASE = "https://graph.facebook.com/v19.0"
    }

    @Volatile
    private var authenticatedPageId: String? = null

    @Volatile
    private var authenticatedAccessToken: String? = null

    @Volatile
    private var authenticatedPageName: String = ""

    private val _cachedCatalog = MutableStateFlow<List<AnimeSeries>>(emptyList())
    val cachedCatalog: StateFlow<List<AnimeSeries>> = _cachedCatalog.asStateFlow()

    val isAuthenticated: Boolean
        get() = !authenticatedPageId.isNullOrBlank() && !authenticatedAccessToken.isNullOrBlank()

    /**
     * Clears the authenticated session and resets the catalog to an empty state.
     */
    fun clearAuthenticationSession() {
        authenticatedPageId = null
        authenticatedAccessToken = null
        authenticatedPageName = ""
        _cachedCatalog.value = emptyList()
    }

    /**
     * Step 1: Validates a Facebook Page ID and Permanent Page Access Token against Graph API v19.0.
     * Until this function succeeds, [isAuthenticated] remains `false` and [cachedCatalog] remains empty.
     */
    suspend fun validatePageAccessToken(
        pageId: String,
        accessToken: String
    ): TokenValidationResult = withContext(Dispatchers.IO) {
        val cleanPageId = pageId.trim()
        val cleanToken = accessToken.trim()
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        if (cleanPageId.isEmpty() || cleanToken.isEmpty()) {
            clearAuthenticationSession()
            return@withContext TokenValidationResult(
                isValid = false,
                validatedAt = nowStr,
                messageAr = "يرجى إدخال معرف الصفحة (Page ID) وتوكن الوصول الدائم (Page Access Token) للمصادقة."
            )
        }

        try {
            val encodedToken = URLEncoder.encode(cleanToken, "UTF-8")
            val url =
                "$GRAPH_API_BASE/$cleanPageId?fields=id,name,category,picture.type(large)&access_token=$encodedToken"
            val req = Request.Builder().url(url).get().build()

            httpClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    clearAuthenticationSession()
                    val fbError = runCatching {
                        JSONObject(bodyStr).optJSONObject("error")?.optString("message")
                    }.getOrNull() ?: "HTTP ${resp.code}"
                    return@withContext TokenValidationResult(
                        isValid = false,
                        validatedAt = nowStr,
                        messageAr = "فشل التحقق من توكن صفحة فيسبوك: $fbError"
                    )
                }

                val json = JSONObject(bodyStr)
                val verifiedId = json.optString("id", cleanPageId)
                val verifiedName = json.optString("name", "").trim()
                val category = json.optString("category", "Facebook Page")
                val pictureUrl = json.optJSONObject("picture")
                    ?.optJSONObject("data")
                    ?.optString("url", "")
                    .orEmpty()

                if (verifiedId.isEmpty() || verifiedName.isEmpty()) {
                    clearAuthenticationSession()
                    return@withContext TokenValidationResult(
                        isValid = false,
                        validatedAt = nowStr,
                        messageAr = "استجابة غير مكتملة من Graph API؛ تأكد من أن التوكن خاص بصفحة فيسبوك."
                    )
                }

                // Mark session as authenticated
                authenticatedPageId = verifiedId
                authenticatedAccessToken = cleanToken
                authenticatedPageName = verifiedName

                TokenValidationResult(
                    isValid = true,
                    verifiedPageId = verifiedId,
                    verifiedPageName = verifiedName,
                    pageCategory = category,
                    pagePictureUrl = pictureUrl,
                    validatedAt = nowStr,
                    messageAr = "تم التحقق من التوكن ومصادقة صفحة ($verifiedName) بنجاح عبر Graph API v19.0"
                )
            }
        } catch (e: Exception) {
            clearAuthenticationSession()
            TokenValidationResult(
                isValid = false,
                validatedAt = nowStr,
                messageAr = "تعذر الاتصال بـ graph.facebook.com للتحقق من التوكن: ${e.localizedMessage ?: "خطأ شبكة"}"
            )
        }
    }

    /**
     * Step 2: Fetches JSON catalog data from the authenticated Facebook Page.
     * Strictly returns an empty catalog (`emptyList()`) if the user has not authenticated
     * via [validatePageAccessToken] in the Admin Dashboard.
     */
    suspend fun fetchJsonCatalogData(
        config: FacebookConfig
    ): CatalogFetchResult = withContext(Dispatchers.IO) {
        val nowStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val cleanPageId = config.pageId.trim()
        val cleanToken = config.accessToken.trim()
        val cleanPostId = config.catalogPostId.trim()

        // Enforce strict authentication check
        if (!isAuthenticated ||
            cleanPageId != authenticatedPageId && cleanPageId.isEmpty() ||
            cleanToken != authenticatedAccessToken
        ) {
            _cachedCatalog.value = emptyList()
            return@withContext CatalogFetchResult(
                isAuthenticated = false,
                success = false,
                catalog = emptyList(),
                sourceMode = config.syncMode,
                fetchedAt = nowStr,
                messageAr = "الكتالوج فارغ ومقفل: يجب مصادقة Page Access Token أولاً من لوحة الإدارة."
            )
        }

        try {
            val encodedToken = URLEncoder.encode(cleanToken, "UTF-8")
            val activePageId = authenticatedPageId ?: cleanPageId

            val fetchedCatalog: List<AnimeSeries> = when (config.syncMode) {
                FacebookSyncMode.MASTER_CATALOG_POST -> {
                    if (cleanPostId.isNotEmpty()) {
                        fetchMasterCatalogPost(cleanPostId, encodedToken)
                    } else {
                        // Search latest Page posts for an embedded JSON catalog block
                        fetchCatalogFromPageFeed(activePageId, encodedToken)
                    }
                }
                FacebookSyncMode.AUTO_VIDEO_DESCRIPTIONS -> {
                    fetchPageVideosCatalog(activePageId, encodedToken)
                }
            }

            _cachedCatalog.value = fetchedCatalog
            val totalEpisodes = fetchedCatalog.sumOf { it.episodes.size }

            val msg = if (fetchedCatalog.isNotEmpty()) {
                "تم سحب ${fetchedCatalog.size} أنمي ($totalEpisodes حلقة) من صفحة ($authenticatedPageName) عبر *6"
            } else {
                "متصل بصفحة ($authenticatedPageName) • الكتالوج على السيرفر لا يحتوي على حلقات منشورة حالياً"
            }

            CatalogFetchResult(
                isAuthenticated = true,
                success = true,
                catalog = fetchedCatalog,
                sourceMode = config.syncMode,
                fetchedAt = nowStr,
                messageAr = msg
            )
        } catch (e: Exception) {
            CatalogFetchResult(
                isAuthenticated = true,
                success = false,
                catalog = _cachedCatalog.value,
                sourceMode = config.syncMode,
                fetchedAt = nowStr,
                messageAr = "حدث خطأ أثناء جلب بيانات الكتالوج من السيرفر: ${e.localizedMessage ?: "خطأ شبكة"}"
            )
        }
    }

    /**
     * Convenience workflow used by "Test & Sync Now" in the Admin Dashboard:
     * 1. Validates the Facebook Page Access Token.
     * 2. If valid, fetches the JSON catalog data. If invalid, keeps the catalog strictly empty.
     */
    suspend fun authenticateAndSyncCatalog(
        config: FacebookConfig
    ): Pair<TokenValidationResult, CatalogFetchResult> {
        val validation = validatePageAccessToken(config.pageId, config.accessToken)
        if (!validation.isValid) {
            val emptyFetch = CatalogFetchResult(
                isAuthenticated = false,
                success = false,
                catalog = emptyList(),
                sourceMode = config.syncMode,
                fetchedAt = validation.validatedAt,
                messageAr = validation.messageAr
            )
            return validation to emptyFetch
        }
        val normalizedConfig = config.copy(pageId = validation.verifiedPageId)
        val fetchResult = fetchJsonCatalogData(normalizedConfig)
        return validation to fetchResult
    }

    /**
     * Mode A: Automated Video-Description Mode (الأوتوماتيكي السريع)
     * GET https://graph.facebook.com/v19.0/{pageId}/videos?fields=id,title,description,picture,source,format,length,created_time&limit=100&access_token={accessToken}
     */
    suspend fun fetchPageVideosCatalog(
        pageId: String,
        encodedToken: String
    ): List<AnimeSeries> = withContext(Dispatchers.IO) {
        if (!isAuthenticated) return@withContext emptyList()
        val url = "$GRAPH_API_BASE/$pageId/videos" +
            "?fields=id,title,description,picture,source,format,length,created_time&limit=100&access_token=$encodedToken"
        val req = Request.Builder().url(url).get().build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val root = JSONObject(resp.body?.string().orEmpty())
            val dataArr = root.optJSONArray("data") ?: return@withContext emptyList()
            parseVideosArrayIntoCatalog(dataArr)
        }
    }

    /**
     * Mode B: Master Catalog Post Mode (منشور قاعدة البيانات الموحد)
     * GET https://graph.facebook.com/v19.0/{catalogPostId}?fields=message&access_token={accessToken}
     */
    suspend fun fetchMasterCatalogPost(
        catalogPostId: String,
        encodedToken: String
    ): List<AnimeSeries> = withContext(Dispatchers.IO) {
        if (!isAuthenticated) return@withContext emptyList()
        val url = "$GRAPH_API_BASE/$catalogPostId?fields=message&access_token=$encodedToken"
        val req = Request.Builder().url(url).get().build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val root = JSONObject(resp.body?.string().orEmpty())
            val message = root.optString("message", "")
            parseMasterPostJson(message)
        }
    }

    /**
     * Fallback for Master Catalog Post Mode when no specific Post ID was provided:
     * Scans the Page's feed for a post containing a JSON catalog payload.
     */
    private suspend fun fetchCatalogFromPageFeed(
        pageId: String,
        encodedToken: String
    ): List<AnimeSeries> = withContext(Dispatchers.IO) {
        if (!isAuthenticated) return@withContext emptyList()
        val url = "$GRAPH_API_BASE/$pageId/posts?fields=id,message&limit=25&access_token=$encodedToken"
        val req = Request.Builder().url(url).get().build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val root = JSONObject(resp.body?.string().orEmpty())
            val dataArr = root.optJSONArray("data") ?: return@withContext emptyList()
            for (i in 0 until dataArr.length()) {
                val postObj = dataArr.optJSONObject(i) ?: continue
                val msg = postObj.optString("message", "")
                if (msg.contains("\"catalog\"") || msg.contains("\"anime_title\"")) {
                    val parsed = parseMasterPostJson(msg)
                    if (parsed.isNotEmpty()) return@withContext parsed
                }
            }
            emptyList()
        }
    }

    /**
     * Mode C: On-Demand fbcdn.net Stream Resolver (حلال روابط الفيديو الفوري)
     * GET https://graph.facebook.com/v19.0/{video_id}?fields=source,format,picture,length&access_token={accessToken}
     */
    suspend fun resolveVideoStream(
        episode: EpisodeItem,
        accessToken: String
    ): ResolvedStreamResult = withContext(Dispatchers.IO) {
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val cleanToken = accessToken.trim()
        val videoId = episode.facebookVideoId.trim()

        if (cleanToken.isNotEmpty() && videoId.isNotEmpty()) {
            try {
                val encodedToken = URLEncoder.encode(cleanToken, "UTF-8")
                val url =
                    "$GRAPH_API_BASE/$videoId?fields=source,format,picture,length&access_token=$encodedToken"
                val req = Request.Builder().url(url).get().build()
                httpClient.newCall(req).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        val obj = JSONObject(bodyStr)
                        val directSource = obj.optString("source", episode.defaultSourceUrl)
                        val formatArr = obj.optJSONArray("format")
                        val parsedFormats = parseGraphFormats(formatArr, directSource)
                        val finalFormats = if (parsedFormats.isNotEmpty()) {
                            parsedFormats
                        } else {
                            DemoAnimeCatalog.buildFormatsFromSource(directSource)
                        }
                        val host = runCatching { URI(directSource).host }.getOrNull()
                            ?: "video.xx.fbcdn.net"
                        return@withContext ResolvedStreamResult(
                            videoId = videoId,
                            resolvedAt = timeFmt,
                            defaultUrl = directSource,
                            formats = finalFormats,
                            sourceHost = "$host (Graph API v19.0 Live)",
                            isLiveGraphResponse = true,
                            errorMessage = if (directSource.isBlank()) {
                                "لم يُرجع Graph API رابط source لهذا الفيديو؛ تحقق من صلاحيات التوكن."
                            } else null
                        )
                    } else {
                        val errMsg = runCatching {
                            JSONObject(bodyStr).optJSONObject("error")?.optString("message")
                        }.getOrNull() ?: "HTTP ${resp.code}"
                        return@withContext ResolvedStreamResult(
                            videoId = videoId,
                            resolvedAt = timeFmt,
                            defaultUrl = episode.defaultSourceUrl,
                            formats = episode.formats,
                            sourceHost = "graph.facebook.com",
                            isLiveGraphResponse = false,
                            errorMessage = "فشل جلب رابط البث من فيسبوك: $errMsg"
                        )
                    }
                }
            } catch (e: Exception) {
                return@withContext ResolvedStreamResult(
                    videoId = videoId,
                    resolvedAt = timeFmt,
                    defaultUrl = episode.defaultSourceUrl,
                    formats = episode.formats,
                    sourceHost = "غير متصل",
                    isLiveGraphResponse = false,
                    errorMessage = "خطأ شبكة أثناء جلب رابط الفيديو: ${e.localizedMessage}"
                )
            }
        }

        if (episode.defaultSourceUrl.isNotBlank()) {
            val host = runCatching { URI(episode.defaultSourceUrl).host }.getOrNull() ?: "direct-stream"
            return@withContext ResolvedStreamResult(
                videoId = videoId,
                resolvedAt = timeFmt,
                defaultUrl = episode.defaultSourceUrl,
                formats = episode.formats.ifEmpty {
                    DemoAnimeCatalog.buildFormatsFromSource(episode.defaultSourceUrl)
                },
                sourceHost = host,
                isLiveGraphResponse = false
            )
        }

        ResolvedStreamResult(
            videoId = videoId,
            resolvedAt = timeFmt,
            defaultUrl = "",
            formats = emptyList(),
            sourceHost = "غير مصادق",
            isLiveGraphResponse = false,
            errorMessage = "يرجى مصادقة Page Access Token في لوحة الإدارة لفك رابط البث المباشر (fbcdn.net)."
        )
    }

    private fun parseGraphFormats(
        formatArr: JSONArray?,
        fallbackSourceUrl: String
    ): List<VideoStreamFormat> {
        if (formatArr == null || formatArr.length() == 0) {
            return DemoAnimeCatalog.buildFormatsFromSource(fallbackSourceUrl)
        }
        val list = mutableListOf<VideoStreamFormat>()
        for (i in 0 until formatArr.length()) {
            val fObj = formatArr.optJSONObject(i) ?: continue
            val h = fObj.optInt("height", 480)
            val w = fObj.optInt("width", 854)
            val quality = when {
                h >= 1000 -> StreamQuality.FHD_1080P
                h >= 700 -> StreamQuality.HD_720P
                else -> StreamQuality.SD_480P
            }
            if (fallbackSourceUrl.isNotBlank() && list.none { it.quality == quality }) {
                list.add(
                    VideoStreamFormat(
                        quality = quality,
                        mp4Url = fallbackSourceUrl,
                        width = w,
                        height = h,
                        bitrateKbps = if (h >= 720) 1600 else 680,
                        isFbcdnZeroRated = fallbackSourceUrl.contains("fbcdn.net")
                    )
                )
            }
        }
        return if (list.isNotEmpty()) list else DemoAnimeCatalog.buildFormatsFromSource(fallbackSourceUrl)
    }

    fun parseVideosArrayIntoCatalog(dataArr: JSONArray): List<AnimeSeries> {
        val grouped = LinkedHashMap<String, MutableList<JSONObject>>()
        for (i in 0 until dataArr.length()) {
            val v = dataArr.optJSONObject(i) ?: continue
            val desc = v.optString("description", "")
            val title = v.optString("title", "").trim()
            val metaJson = extractEmbeddedJson(desc)
            val animeTitle = metaJson?.optString("anime_title")?.takeIf { it.isNotBlank() }
                ?: title.substringBefore(" - ").substringBefore(" الحلقة").trim()
                    .ifEmpty { "أنمي فيسبوك #${i + 1}" }
            grouped.getOrPut(animeTitle) { mutableListOf() }.add(v)
        }

        val themes = PosterEmblemStyle.entries
        return grouped.entries.mapIndexed { idx, (animeTitle, videos) ->
            val firstVideo = videos.first()
            val firstMeta = extractEmbeddedJson(firstVideo.optString("description", ""))
            val animeId = "fb_anime_${animeTitle.hashCode().toUInt()}"
            val episodes = videos.mapIndexed { epIdx, vObj ->
                val vId = vObj.optString("id", "")
                val vDescMeta = extractEmbeddedJson(vObj.optString("description", ""))
                val epNum = vDescMeta?.optInt("episode", epIdx + 1)
                    ?: extractEpisodeNumberFromTitle(vObj.optString("title", ""), epIdx + 1)
                val isFiller = vDescMeta?.optBoolean("is_filler", false) ?: false
                val srcUrl = vObj.optString("source", "")
                val formatArr = vObj.optJSONArray("format")
                EpisodeItem(
                    id = "${animeId}_ep_${epNum}_$vId",
                    animeId = animeId,
                    number = epNum,
                    titleAr = vDescMeta?.optString("ep_title")?.takeIf { it.isNotBlank() }
                        ?: vObj.optString("title", "الحلقة $epNum"),
                    facebookVideoId = vId,
                    durationSeconds = vObj.optInt("length", 1420),
                    isFiller = isFiller,
                    releaseDate = vObj.optString("created_time", "").take(10),
                    defaultSourceUrl = srcUrl,
                    formats = parseGraphFormats(formatArr, srcUrl)
                )
            }.sortedBy { it.number }

            val genresList = mutableListOf<String>()
            firstMeta?.optJSONArray("genres")?.let { gArr ->
                for (g in 0 until gArr.length()) {
                    val gStr = gArr.optString(g).trim()
                    if (gStr.isNotEmpty()) genresList.add(gStr)
                }
            }
            if (genresList.isEmpty()) genresList.add("أنمي")

            AnimeSeries(
                id = animeId,
                titleAr = animeTitle,
                titleEn = firstMeta?.optString("title_en", animeTitle) ?: animeTitle,
                synopsisAr = firstMeta?.optString("story")?.takeIf { it.isNotBlank() }
                    ?: firstVideo.optString("description", "مرفوع على صفحة فيسبوك *6"),
                genres = genresList,
                studio = firstMeta?.optString("studio", "Facebook *6 Cloud") ?: "Facebook *6 Cloud",
                releaseYear = firstMeta?.optInt("year", 2025) ?: 2025,
                statusAr = firstMeta?.optString("status", "مستمر") ?: "مستمر",
                typeAr = firstMeta?.optString("type", "TV") ?: "TV",
                malScore = firstMeta?.optDouble("score", 8.5) ?: 8.5,
                airingDayAr = firstMeta?.optString("day", "السبت") ?: "السبت",
                airingTimeAr = firstMeta?.optString("time", "20:00 بتوقيت مكة") ?: "20:00 بتوقيت مكة",
                countdownText = "متزامن من صفحة فيسبوك *6",
                posterFbcdnUrl = firstMeta?.optString("poster_fbcdn")?.takeIf { it.isNotBlank() }
                    ?: firstVideo.optString("picture", ""),
                visualTheme = AnimeVisualTheme(
                    primaryColorHex = 0xFF1877F2,
                    secondaryColorHex = 0xFF0A1020,
                    accentColorHex = 0xFFFF2A54,
                    emblemStyle = themes[idx % themes.size]
                ),
                isSeasonal = true,
                isSpotlight = idx < 3,
                episodes = episodes
            )
        }
    }

    private fun extractEpisodeNumberFromTitle(title: String, fallback: Int): Int {
        val match = Regex("(?:الحلقة|حلقة|Episode|Ep)\\s*(\\d+)", RegexOption.IGNORE_CASE).find(title)
            ?: Regex("(\\d+)").find(title)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: fallback
    }

    fun parseMasterPostJson(message: String): List<AnimeSeries> {
        val jsonBlock = extractEmbeddedJson(message) ?: return emptyList()

        val seriesArr = jsonBlock.optJSONArray("catalog")
        if (seriesArr == null) {
            if (jsonBlock.has("anime_title")) {
                val singleVideoObj = JSONObject().apply {
                    put("id", jsonBlock.optString("video_id", ""))
                    put("title", jsonBlock.optString("anime_title"))
                    put("description", jsonBlock.toString())
                    put("source", jsonBlock.optString("source_url", ""))
                }
                return parseVideosArrayIntoCatalog(JSONArray().put(singleVideoObj))
            }
            return emptyList()
        }

        val result = mutableListOf<AnimeSeries>()
        val styles = PosterEmblemStyle.entries
        for (i in 0 until seriesArr.length()) {
            val item = seriesArr.optJSONObject(i) ?: continue
            val animeId = item.optString("id", "master_anime_$i")
            val epsArr = item.optJSONArray("episodes") ?: JSONArray()
            val episodes = mutableListOf<EpisodeItem>()
            for (e in 0 until epsArr.length()) {
                val epObj = epsArr.optJSONObject(e) ?: continue
                val vId = epObj.optString("video_id", "")
                val num = epObj.optInt("number", e + 1)
                val directUrl = epObj.optString("source_url", epObj.optString("source_hint", ""))
                episodes.add(
                    EpisodeItem(
                        id = "${animeId}_ep_$num",
                        animeId = animeId,
                        number = num,
                        titleAr = epObj.optString("title", "الحلقة $num"),
                        facebookVideoId = vId,
                        durationSeconds = epObj.optInt("length", 1420),
                        isFiller = epObj.optBoolean("filler", false),
                        releaseDate = epObj.optString("date", ""),
                        defaultSourceUrl = directUrl,
                        formats = DemoAnimeCatalog.buildFormatsFromSource(directUrl)
                    )
                )
            }
            val genres = mutableListOf<String>()
            item.optJSONArray("genres")?.let { gArr ->
                for (g in 0 until gArr.length()) genres.add(gArr.optString(g))
            }
            result.add(
                AnimeSeries(
                    id = animeId,
                    titleAr = item.optString("title", "أنمي *6"),
                    titleEn = item.optString("title_en", ""),
                    synopsisAr = item.optString("story", ""),
                    genres = if (genres.isEmpty()) listOf("أنمي") else genres,
                    studio = item.optString("studio", ""),
                    releaseYear = item.optInt("year", 2025),
                    statusAr = item.optString("status", "مستمر"),
                    typeAr = item.optString("type", "TV"),
                    malScore = item.optDouble("score", 8.5),
                    airingDayAr = item.optString("day", "السبت"),
                    airingTimeAr = item.optString("time", "20:00 بتوقيت مكة"),
                    countdownText = "متزامن من منشور قاعدة البيانات",
                    posterFbcdnUrl = item.optString("poster_fbcdn", ""),
                    visualTheme = AnimeVisualTheme(
                        primaryColorHex = 0xFFFF2A54,
                        secondaryColorHex = 0xFF10131C,
                        accentColorHex = 0xFF7C3AED,
                        emblemStyle = styles[i % styles.size]
                    ),
                    isSeasonal = true,
                    isSpotlight = i < 3,
                    episodes = episodes
                )
            )
        }
        return result
    }

    private fun extractEmbeddedJson(rawText: String): JSONObject? {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        return runCatching { JSONObject(rawText.substring(start, end + 1)) }.getOrNull()
    }

    fun generateVideoDescriptionJson(
        animeTitle: String,
        storyAr: String,
        genresCsv: String,
        statusAr: String,
        episodeNumber: Int,
        isFiller: Boolean,
        facebookVideoId: String,
        directSourceUrl: String = ""
    ): String {
        val genresList = genresCsv.split("،", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val root = JSONObject().apply {
            put("bihi_engine", "star6_v19")
            put("anime_title", animeTitle.trim())
            put("story", storyAr.trim())
            put("genres", JSONArray(genresList))
            put("status", statusAr.trim().ifEmpty { "مستمر" })
            put("episode", episodeNumber)
            put("is_filler", isFiller)
            put("video_id", facebookVideoId.trim())
            if (directSourceUrl.isNotBlank()) {
                put("source_url", directSourceUrl.trim())
            }
            put("stream_policy", "fbcdn.net_zero_rated")
        }
        return root.toString(2)
    }
}
