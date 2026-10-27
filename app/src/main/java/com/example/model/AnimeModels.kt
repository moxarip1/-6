package com.example.model

enum class NetworkMode(val labelAr: String, val badgeSubtext: String) {
    STAR6_FBCDN("وضع *6 نشط (fbcdn.net)", "صفر استهلاك للبيانات الخارجية • Graph API"),
    STANDARD_WIFI("الوضع العادي (Wi-Fi / *3)", "اتصال مفتوح لكافة السيرفرات والمواقع")
}

enum class MainTab(val titleAr: String, val testTag: String) {
    HOME("الرئيسية", "tab_home"),
    SCHEDULE("جدول المواعيد", "tab_schedule"),
    EXPLORE("الاستكشاف", "tab_explore"),
    WEB_READER("قارئ المواقع", "tab_web_reader"),
    LIBRARY("مكتبتي", "tab_library")
}

enum class WatchStatus(val labelAr: String) {
    NONE("إضافة لقائمتي"),
    WATCHING("أشاهده حالياً"),
    COMPLETED("مكتمل"),
    PLAN_TO_WATCH("أخطط لمشاهدته"),
    FAVORITE("المفضلة")
}

enum class LibrarySubTab(val titleAr: String) {
    WATCHING("أشاهده حالياً"),
    COMPLETED("مكتمل"),
    FAVORITES("المفضلة"),
    HISTORY("سجل المشاهدة"),
    DOWNLOADS("التحميلات")
}

enum class StreamQuality(val labelAr: String, val shortTag: String, val height: Int) {
    SD_480P("توفير *6 (480p SD)", "480p *6", 480),
    HD_720P("جودة عالية (720p HD)", "720p HD", 720),
    FHD_1080P("فائقة (1080p FHD)", "1080p FHD", 1080)
}

enum class FacebookSyncMode(val titleAr: String, val subtitleAr: String) {
    AUTO_VIDEO_DESCRIPTIONS(
        "الأوتوماتيكي السريع (وصف الفيديوهات)",
        "يسحب جميع فيديوهات الصفحة ويقرأ بيانات الأنمي من الوصف أو العنوان تلقائياً"
    ),
    MASTER_CATALOG_POST(
        "منشور قاعدة البيانات الموحد (Master Post)",
        "يقرأ منشور فيسبوك واحد يحتوي على كود JSON المضغوط لكامل مكتبة الأنمي"
    )
}

data class TokenValidationResult(
    val isValid: Boolean,
    val verifiedPageId: String = "",
    val verifiedPageName: String = "",
    val pageCategory: String = "",
    val pagePictureUrl: String = "",
    val validatedAt: String = "",
    val messageAr: String
)

data class CatalogFetchResult(
    val isAuthenticated: Boolean,
    val success: Boolean,
    val catalog: List<AnimeSeries>,
    val sourceMode: FacebookSyncMode,
    val fetchedAt: String,
    val messageAr: String
)

data class VideoStreamFormat(
    val quality: StreamQuality,
    val mp4Url: String,
    val width: Int,
    val height: Int,
    val bitrateKbps: Int,
    val isFbcdnZeroRated: Boolean = true
)

data class EpisodeItem(
    val id: String,
    val animeId: String,
    val number: Int,
    val titleAr: String,
    val facebookVideoId: String,
    val durationSeconds: Int,
    val isFiller: Boolean,
    val releaseDate: String,
    val defaultSourceUrl: String,
    val formats: List<VideoStreamFormat>
)

enum class PosterEmblemStyle {
    CRIMSON_KATANA,
    SHADOW_MONARCH,
    CYBER_SHINOBI,
    AZURE_ALCHEMIST,
    SOLAR_PIRATE,
    CURSED_SORCERER
}

data class AnimeVisualTheme(
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val emblemStyle: PosterEmblemStyle
)

data class AnimeSeries(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val synopsisAr: String,
    val genres: List<String>,
    val studio: String,
    val releaseYear: Int,
    val statusAr: String, // "مستمر" or "مكتمل"
    val typeAr: String,   // "TV", "Movie", "OVA"
    val malScore: Double,
    val airingDayAr: String,
    val airingTimeAr: String,
    val countdownText: String,
    val posterFbcdnUrl: String = "",
    val visualTheme: AnimeVisualTheme,
    val isSeasonal: Boolean = true,
    val isSpotlight: Boolean = false,
    val episodes: List<EpisodeItem>
)

data class FacebookConfig(
    val pageId: String = "",
    val accessToken: String = "",
    val catalogPostId: String = "",
    val syncMode: FacebookSyncMode = FacebookSyncMode.AUTO_VIDEO_DESCRIPTIONS,
    val connectedPageName: String = "غير مصادق (الكتالوج مقفل وفارغ)",
    val pageCategory: String = "",
    val isLiveConnected: Boolean = false,
    val lastSyncTime: String = "بانتظار المصادقة",
    val statusMessage: String = "الكتالوج فارغ تماماً حتى تتم المصادقة بتوكن الصفحة عبر لوحة الإدارة"
)

data class ResolvedStreamResult(
    val videoId: String,
    val resolvedAt: String,
    val defaultUrl: String,
    val formats: List<VideoStreamFormat>,
    val sourceHost: String,
    val isLiveGraphResponse: Boolean,
    val errorMessage: String? = null
)

data class CustomScraperSource(
    val id: String,
    val siteName: String,
    val baseUrl: String,
    val cardSelector: String,
    val episodeSelector: String,
    val streamSelector: String
)

data class ScrapedEpisodeItem(
    val number: Int,
    val title: String,
    val streamUrl: String,
    val suggestedFbVideoId: String
)

data class ScrapedAnimeResult(
    val id: String,
    val sourceUrl: String,
    val titleAr: String,
    val titleEn: String,
    val synopsisAr: String,
    val genres: List<String>,
    val statusAr: String,
    val typeAr: String,
    val releaseYear: Int,
    val studio: String,
    val malScore: Double,
    val posterUrl: String = "",
    val episodes: List<ScrapedEpisodeItem>,
    val adsStrippedCount: Int,
    val extractionSteps: List<String>,
    val fbStar6Json: String
)

data class WatchHistoryEntry(
    val animeId: String,
    val animeTitleAr: String,
    val episodeId: String,
    val episodeNumber: Int,
    val episodeTitleAr: String,
    val lastPositionSec: Int,
    val durationSec: Int,
    val updatedAtText: String
)

data class DownloadRecord(
    val id: String,
    val episodeId: String,
    val animeId: String,
    val animeTitleAr: String,
    val episodeNumber: Int,
    val qualityLabel: String,
    val fileSizeMb: String,
    val fbcdnSourceUrl: String,
    val progressPercent: Int = 100,
    val stateLabel: String = "رابط مباشر fbcdn.net"
)
