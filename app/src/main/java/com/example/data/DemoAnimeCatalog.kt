package com.example.data

import com.example.model.AnimeSeries
import com.example.model.AnimeVisualTheme
import com.example.model.CustomScraperSource
import com.example.model.DownloadRecord
import com.example.model.EpisodeItem
import com.example.model.PosterEmblemStyle
import com.example.model.StreamQuality
import com.example.model.VideoStreamFormat
import com.example.model.WatchHistoryEntry
import com.example.model.WatchStatus
import org.json.JSONArray
import org.json.JSONObject

object DemoAnimeCatalog {

    val daysOfWeekAr = listOf(
        "السبت",
        "الأحد",
        "الإثنين",
        "الثلاثاء",
        "الأربعاء",
        "الخميس",
        "الجمعة"
    )

    val allGenresAr = listOf(
        "الكل",
        "أكشن",
        "خيال مظلم",
        "شونين",
        "سايبربانك",
        "مغامرات",
        "سحر",
        "غموض",
        "قوى خارقة",
        "دراما",
        "كوميديا"
    )

    fun buildFormatsFromSource(directSourceUrl: String): List<VideoStreamFormat> {
        if (directSourceUrl.isBlank()) return emptyList()
        return listOf(
            VideoStreamFormat(
                quality = StreamQuality.SD_480P,
                mp4Url = directSourceUrl,
                width = 854,
                height = 480,
                bitrateKbps = 680,
                isFbcdnZeroRated = directSourceUrl.contains("fbcdn.net")
            ),
            VideoStreamFormat(
                quality = StreamQuality.HD_720P,
                mp4Url = directSourceUrl,
                width = 1280,
                height = 720,
                bitrateKbps = 1500,
                isFbcdnZeroRated = directSourceUrl.contains("fbcdn.net")
            ),
            VideoStreamFormat(
                quality = StreamQuality.FHD_1080P,
                mp4Url = directSourceUrl,
                width = 1920,
                height = 1080,
                bitrateKbps = 2800,
                isFbcdnZeroRated = directSourceUrl.contains("fbcdn.net")
            )
        )
    }

    fun serializeCatalogToJson(catalog: List<AnimeSeries>): String {
        val arr = JSONArray()
        catalog.forEach { anime ->
            val epsArr = JSONArray()
            anime.episodes.forEach { ep ->
                val fmtArr = JSONArray()
                ep.formats.forEach { f ->
                    fmtArr.put(
                        JSONObject().apply {
                            put("quality", f.quality.name)
                            put("mp4Url", f.mp4Url)
                            put("width", f.width)
                            put("height", f.height)
                            put("bitrateKbps", f.bitrateKbps)
                            put("isFbcdnZeroRated", f.isFbcdnZeroRated)
                        }
                    )
                }
                epsArr.put(
                    JSONObject().apply {
                        put("id", ep.id)
                        put("animeId", ep.animeId)
                        put("number", ep.number)
                        put("titleAr", ep.titleAr)
                        put("facebookVideoId", ep.facebookVideoId)
                        put("durationSeconds", ep.durationSeconds)
                        put("isFiller", ep.isFiller)
                        put("releaseDate", ep.releaseDate)
                        put("defaultSourceUrl", ep.defaultSourceUrl)
                        put("formats", fmtArr)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", anime.id)
                    put("titleAr", anime.titleAr)
                    put("titleEn", anime.titleEn)
                    put("synopsisAr", anime.synopsisAr)
                    put("genres", JSONArray(anime.genres))
                    put("studio", anime.studio)
                    put("releaseYear", anime.releaseYear)
                    put("statusAr", anime.statusAr)
                    put("typeAr", anime.typeAr)
                    put("malScore", anime.malScore)
                    put("airingDayAr", anime.airingDayAr)
                    put("airingTimeAr", anime.airingTimeAr)
                    put("countdownText", anime.countdownText)
                    put("posterFbcdnUrl", anime.posterFbcdnUrl)
                    put("primaryColorHex", anime.visualTheme.primaryColorHex)
                    put("secondaryColorHex", anime.visualTheme.secondaryColorHex)
                    put("accentColorHex", anime.visualTheme.accentColorHex)
                    put("emblemStyle", anime.visualTheme.emblemStyle.name)
                    put("isSeasonal", anime.isSeasonal)
                    put("isSpotlight", anime.isSpotlight)
                    put("episodes", epsArr)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeCatalogFromJson(rawJson: String?): List<AnimeSeries> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<AnimeSeries>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val genres = mutableListOf<String>()
                obj.optJSONArray("genres")?.let { gArr ->
                    for (g in 0 until gArr.length()) genres.add(gArr.optString(g))
                }
                val eps = mutableListOf<EpisodeItem>()
                obj.optJSONArray("episodes")?.let { eArr ->
                    for (e in 0 until eArr.length()) {
                        val eObj = eArr.optJSONObject(e) ?: continue
                        val defaultUrl = eObj.optString("defaultSourceUrl", "")
                        val formats = mutableListOf<VideoStreamFormat>()
                        eObj.optJSONArray("formats")?.let { fArr ->
                            for (f in 0 until fArr.length()) {
                                val fObj = fArr.optJSONObject(f) ?: continue
                                val q = runCatching {
                                    StreamQuality.valueOf(fObj.optString("quality", "SD_480P"))
                                }.getOrDefault(StreamQuality.SD_480P)
                                formats.add(
                                    VideoStreamFormat(
                                        quality = q,
                                        mp4Url = fObj.optString("mp4Url", defaultUrl),
                                        width = fObj.optInt("width", 854),
                                        height = fObj.optInt("height", 480),
                                        bitrateKbps = fObj.optInt("bitrateKbps", 680),
                                        isFbcdnZeroRated = fObj.optBoolean("isFbcdnZeroRated", true)
                                    )
                                )
                            }
                        }
                        eps.add(
                            EpisodeItem(
                                id = eObj.optString("id"),
                                animeId = eObj.optString("animeId"),
                                number = eObj.optInt("number", e + 1),
                                titleAr = eObj.optString("titleAr"),
                                facebookVideoId = eObj.optString("facebookVideoId"),
                                durationSeconds = eObj.optInt("durationSeconds", 1420),
                                isFiller = eObj.optBoolean("isFiller", false),
                                releaseDate = eObj.optString("releaseDate", ""),
                                defaultSourceUrl = defaultUrl,
                                formats = if (formats.isNotEmpty()) formats else buildFormatsFromSource(defaultUrl)
                            )
                        )
                    }
                }
                val style = runCatching {
                    PosterEmblemStyle.valueOf(obj.optString("emblemStyle", "CRIMSON_KATANA"))
                }.getOrDefault(PosterEmblemStyle.CRIMSON_KATANA)

                list.add(
                    AnimeSeries(
                        id = obj.optString("id"),
                        titleAr = obj.optString("titleAr"),
                        titleEn = obj.optString("titleEn"),
                        synopsisAr = obj.optString("synopsisAr"),
                        genres = genres,
                        studio = obj.optString("studio", ""),
                        releaseYear = obj.optInt("releaseYear", 2025),
                        statusAr = obj.optString("statusAr", "مستمر"),
                        typeAr = obj.optString("typeAr", "TV"),
                        malScore = obj.optDouble("malScore", 0.0),
                        airingDayAr = obj.optString("airingDayAr", "السبت"),
                        airingTimeAr = obj.optString("airingTimeAr", ""),
                        countdownText = obj.optString("countdownText", ""),
                        posterFbcdnUrl = obj.optString("posterFbcdnUrl", ""),
                        visualTheme = AnimeVisualTheme(
                            primaryColorHex = obj.optLong("primaryColorHex", 0xFFFF2A54),
                            secondaryColorHex = obj.optLong("secondaryColorHex", 0xFF10131C),
                            accentColorHex = obj.optLong("accentColorHex", 0xFF1877F2),
                            emblemStyle = style
                        ),
                        isSeasonal = obj.optBoolean("isSeasonal", true),
                        isSpotlight = obj.optBoolean("isSpotlight", false),
                        episodes = eps
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun serializeWatchStatuses(map: Map<String, WatchStatus>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v.name) }
        return obj.toString()
    }

    fun deserializeWatchStatuses(raw: String?): Map<String, WatchStatus> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, WatchStatus>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val status = runCatching { WatchStatus.valueOf(obj.optString(k)) }
                    .getOrDefault(WatchStatus.NONE)
                if (status != WatchStatus.NONE) result[k] = status
            }
            result
        }.getOrDefault(emptyMap())
    }

    fun serializeResumeMap(map: Map<String, Int>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    fun deserializeResumeMap(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = obj.optInt(k, 0)
            }
            result
        }.getOrDefault(emptyMap())
    }

    fun serializeHistory(list: List<WatchHistoryEntry>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("animeId", item.animeId)
                    put("animeTitleAr", item.animeTitleAr)
                    put("episodeId", item.episodeId)
                    put("episodeNumber", item.episodeNumber)
                    put("episodeTitleAr", item.episodeTitleAr)
                    put("lastPositionSec", item.lastPositionSec)
                    put("durationSec", item.durationSec)
                    put("updatedAtText", item.updatedAtText)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeHistory(raw: String?): List<WatchHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                WatchHistoryEntry(
                    animeId = o.optString("animeId"),
                    animeTitleAr = o.optString("animeTitleAr"),
                    episodeId = o.optString("episodeId"),
                    episodeNumber = o.optInt("episodeNumber", 1),
                    episodeTitleAr = o.optString("episodeTitleAr"),
                    lastPositionSec = o.optInt("lastPositionSec", 0),
                    durationSec = o.optInt("durationSec", 1420),
                    updatedAtText = o.optString("updatedAtText")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun serializeDownloads(list: List<DownloadRecord>): String {
        val arr = JSONArray()
        list.forEach { d ->
            arr.put(
                JSONObject().apply {
                    put("id", d.id)
                    put("episodeId", d.episodeId)
                    put("animeId", d.animeId)
                    put("animeTitleAr", d.animeTitleAr)
                    put("episodeNumber", d.episodeNumber)
                    put("qualityLabel", d.qualityLabel)
                    put("fileSizeMb", d.fileSizeMb)
                    put("fbcdnSourceUrl", d.fbcdnSourceUrl)
                    put("progressPercent", d.progressPercent)
                    put("stateLabel", d.stateLabel)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeDownloads(raw: String?): List<DownloadRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                DownloadRecord(
                    id = o.optString("id"),
                    episodeId = o.optString("episodeId"),
                    animeId = o.optString("animeId"),
                    animeTitleAr = o.optString("animeTitleAr"),
                    episodeNumber = o.optInt("episodeNumber", 1),
                    qualityLabel = o.optString("qualityLabel"),
                    fileSizeMb = o.optString("fileSizeMb"),
                    fbcdnSourceUrl = o.optString("fbcdnSourceUrl"),
                    progressPercent = o.optInt("progressPercent", 100),
                    stateLabel = o.optString("stateLabel", "رابط مباشر fbcdn.net")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun serializeCustomSources(list: List<CustomScraperSource>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("siteName", s.siteName)
                    put("baseUrl", s.baseUrl)
                    put("cardSelector", s.cardSelector)
                    put("episodeSelector", s.episodeSelector)
                    put("streamSelector", s.streamSelector)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeCustomSources(raw: String?): List<CustomScraperSource> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                CustomScraperSource(
                    id = o.optString("id"),
                    siteName = o.optString("siteName"),
                    baseUrl = o.optString("baseUrl"),
                    cardSelector = o.optString("cardSelector"),
                    episodeSelector = o.optString("episodeSelector"),
                    streamSelector = o.optString("streamSelector")
                )
            }
        }.getOrDefault(emptyList())
    }
}
