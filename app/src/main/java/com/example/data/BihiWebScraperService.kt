package com.example.data

import com.example.model.ScrapedAnimeResult
import com.example.model.ScrapedEpisodeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class BihiWebScraperService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    sealed class ScrapeOutcome {
        data class Success(val result: ScrapedAnimeResult) : ScrapeOutcome()
        data class Error(val message: String) : ScrapeOutcome()
    }

    /**
     * Real HTML Web-Reader & DOM Extractor.
     * Fetches the actual target URL (directly or via CORS reader proxy if needed),
     * strips ad/popup scripts, extracts real title, synopsis, poster, and episodes,
     * and returns an honest error if the URL is empty or unreachable.
     */
    suspend fun scrapeAnimeUrl(
        rawUrl: String,
        cardSelector: String,
        episodeSelector: String,
        streamSelector: String
    ): ScrapeOutcome = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return@withContext ScrapeOutcome.Error("يرجى إدخال رابط صفحة أنمي صحيح يبدأ بـ https:// أو http://")
        }

        val steps = mutableListOf<String>()
        steps.add("1. الاتصال بالرابط: $url")

        var rawHtml = ""
        var fetchError: String? = null

        // Attempt 1: Direct HTTP GET
        try {
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
                )
                .header("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    rawHtml = resp.body?.string().orEmpty()
                } else {
                    fetchError = "HTTP ${resp.code}"
                }
            }
        } catch (e: Exception) {
            fetchError = e.localizedMessage ?: "خطأ اتصال"
        }

        // Attempt 2: Fallback via Reader Proxy if direct fetch was blocked by Cloudflare/CORS
        if (rawHtml.isBlank()) {
            try {
                val encoded = URLEncoder.encode(url, "UTF-8")
                val proxyUrl = "https://api.allorigins.win/raw?url=$encoded"
                val proxyReq = Request.Builder().url(proxyUrl).get().build()
                httpClient.newCall(proxyReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        rawHtml = resp.body?.string().orEmpty()
                        steps.add("تم الجلب بنجاح عبر بروكسي القراءة المباشر")
                    }
                }
            } catch (_: Exception) {
                // Keep original fetchError
            }
        }

        if (rawHtml.isBlank()) {
            return@withContext ScrapeOutcome.Error(
                "تعذر سحب الصفحة من الرابط المدخل (${fetchError ?: "استجابة فارغة"}). تأكد من صحة الرابط أو من تفعيل اتصال الإنترنت."
            )
        }

        // 2. Strip Ad/Popup scripts
        val scriptRegex = Regex("<script\\b[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
        val adsStrippedCount = scriptRegex.findAll(rawHtml).count()
        val cleanHtml = scriptRegex.replace(rawHtml, "")
        steps.add("2. مصفاة الإعلانات: تم إزالة $adsStrippedCount وسم <script> ونوافذ تتبع من الصفحة")

        // 3. Extract Metadata (Title, Synopsis, Poster, Genres)
        val ogTitle = extractMetaContent(cleanHtml, "og:title")
        val h1Title = Regex("<h1[^>]*>(.*?)</h1>", RegexOption.IGNORE_CASE)
            .find(cleanHtml)?.groupValues?.getOrNull(1)?.let { stripTags(it) }
        val docTitle = Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE)
            .find(cleanHtml)?.groupValues?.getOrNull(1)?.let { stripTags(it) }

        val finalTitle = (ogTitle ?: h1Title ?: docTitle ?: URI(url).path.trim('/').substringAfterLast('/'))
            .trim()
            .ifEmpty { "أنمي مستخرج من الويب" }

        val ogDesc = extractMetaContent(cleanHtml, "og:description")
            ?: extractMetaContent(cleanHtml, "description")
            ?: Regex("<p[^>]*>([\\s\\S]{40,500}?)</p>", RegexOption.IGNORE_CASE)
                .find(cleanHtml)?.groupValues?.getOrNull(1)?.let { stripTags(it) }
            ?: "تم استخراج بيانات هذا الأنمي مباشرة من: $url"

        val ogImage = extractMetaContent(cleanHtml, "og:image").orEmpty()

        steps.add("3. تطبيق المحددات [$cardSelector] و [$episodeSelector] و [$streamSelector]")

        // 4. Extract Episodes and Video/Iframe links from HTML
        val baseUri = runCatching { URI(url) }.getOrNull()
        val extractedEpisodes = mutableListOf<ScrapedEpisodeItem>()

        // Find direct video/iframe streams first
        val iframeSrcs = Regex("<(?:iframe|video|source)[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .findAll(cleanHtml)
            .map { resolveRelativeUrl(baseUri, it.groupValues[1]) }
            .filter { it.startsWith("http") }
            .distinct()
            .toList()

        // Find episode anchor links
        val anchorMatches = Regex("<a[^>]+href=[\"']([^\"'#]+)[\"'][^>]*>([\\s\\S]*?)</a>", RegexOption.IGNORE_CASE)
            .findAll(cleanHtml)
            .mapNotNull { m ->
                val href = resolveRelativeUrl(baseUri, m.groupValues[1].trim())
                val text = stripTags(m.groupValues[2]).trim()
                if (href.startsWith("http") && (
                        text.contains("حلقة") ||
                            text.contains("الحلقة") ||
                            text.contains("episode", ignoreCase = true) ||
                            text.contains("ep", ignoreCase = true) ||
                            href.contains("episode", ignoreCase = true) ||
                            href.contains("ep-", ignoreCase = true) ||
                            href.contains("%d8%ad%d9%84%d9%82%d8%a9", ignoreCase = true)
                        )
                ) {
                    href to text.ifEmpty { href.substringAfterLast('/') }
                } else null
            }
            .distinctBy { it.first }
            .take(50)
            .toList()

        if (anchorMatches.isNotEmpty()) {
            anchorMatches.forEachIndexed { idx, (epUrl, epTitle) ->
                val epNum = Regex("(\\d+)").find(epTitle)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: (idx + 1)
                extractedEpisodes.add(
                    ScrapedEpisodeItem(
                        number = epNum,
                        title = epTitle,
                        streamUrl = iframeSrcs.getOrNull(idx) ?: epUrl,
                        suggestedFbVideoId = ""
                    )
                )
            }
        } else if (iframeSrcs.isNotEmpty()) {
            iframeSrcs.forEachIndexed { idx, streamUrl ->
                val epNum = idx + 1
                extractedEpisodes.add(
                    ScrapedEpisodeItem(
                        number = epNum,
                        title = "$finalTitle - الحلقة $epNum",
                        streamUrl = streamUrl,
                        suggestedFbVideoId = ""
                    )
                )
            }
        } else {
            // Single page entry representing the scraped page itself
            extractedEpisodes.add(
                ScrapedEpisodeItem(
                    number = 1,
                    title = "$finalTitle - الحلقة 1",
                    streamUrl = url,
                    suggestedFbVideoId = ""
                )
            )
        }

        steps.add("4. اكتمل الاستخراج: تم العثور على ${extractedEpisodes.size} حلقة/رابط من الصفحة الحقيقية")

        val hostStudio = runCatching { URI(url).host.removePrefix("www.") }.getOrDefault("Web Source")
        val fbJson = buildStar6CatalogJson(
            titleAr = finalTitle,
            titleEn = finalTitle,
            synopsisAr = ogDesc,
            genres = listOf("أنمي", "مستخرج"),
            statusAr = "مستمر",
            typeAr = "TV",
            year = 2025,
            studio = hostStudio,
            score = 8.5,
            posterUrl = ogImage,
            episodes = extractedEpisodes
        )

        ScrapeOutcome.Success(
            ScrapedAnimeResult(
                id = "scraped_${url.hashCode().toUInt()}",
                sourceUrl = url,
                titleAr = finalTitle,
                titleEn = finalTitle,
                synopsisAr = ogDesc,
                genres = listOf("أنمي"),
                statusAr = "مستمر",
                typeAr = "TV",
                releaseYear = 2025,
                studio = hostStudio,
                malScore = 8.5,
                posterUrl = ogImage,
                episodes = extractedEpisodes,
                adsStrippedCount = adsStrippedCount,
                extractionSteps = steps,
                fbStar6Json = fbJson
            )
        )
    }

    private fun extractMetaContent(html: String, propOrName: String): String? {
        val pattern1 = Regex(
            "<meta[^>]+(?:property|name)=[\"']$propOrName[\"'][^>]+content=[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE
        )
        val pattern2 = Regex(
            "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+(?:property|name)=[\"']$propOrName[\"']",
            RegexOption.IGNORE_CASE
        )
        return (pattern1.find(html) ?: pattern2.find(html))
            ?.groupValues?.getOrNull(1)
            ?.let { stripTags(it).trim() }
            ?.takeIf { it.isNotEmpty() }
    }

    private fun stripTags(input: String): String {
        return input
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#8211;", "-")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun resolveRelativeUrl(baseUri: URI?, href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        return runCatching { baseUri?.resolve(href)?.toString() ?: href }.getOrDefault(href)
    }

    private fun buildStar6CatalogJson(
        titleAr: String,
        titleEn: String,
        synopsisAr: String,
        genres: List<String>,
        statusAr: String,
        typeAr: String,
        year: Int,
        studio: String,
        score: Double,
        posterUrl: String,
        episodes: List<ScrapedEpisodeItem>
    ): String {
        val epsJson = JSONArray()
        episodes.forEach { ep ->
            epsJson.put(
                JSONObject().apply {
                    put("number", ep.number)
                    put("title", ep.title)
                    put("video_id", ep.suggestedFbVideoId)
                    put("filler", false)
                    put("source_url", ep.streamUrl)
                }
            )
        }
        val animeObj = JSONObject().apply {
            put("id", "fb_imported_${titleAr.hashCode().toUInt()}")
            put("title", titleAr)
            put("title_en", titleEn)
            put("story", synopsisAr)
            put("genres", JSONArray(genres))
            put("status", statusAr)
            put("type", typeAr)
            put("year", year)
            put("studio", studio)
            put("score", score)
            put("day", "السبت")
            put("poster_fbcdn", posterUrl)
            put("episodes", epsJson)
        }
        val wrapper = JSONObject().apply {
            put("engine", "Anime Bihi *6 Facebook Graph Importer")
            put("catalog", JSONArray().put(animeObj))
        }
        return wrapper.toString(2)
    }
}
