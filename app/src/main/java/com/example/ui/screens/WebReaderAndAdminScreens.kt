package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomScraperSource
import com.example.model.FacebookConfig
import com.example.model.FacebookSyncMode
import com.example.model.ScrapedAnimeResult
import com.example.model.TokenValidationResult
import com.example.ui.components.GlassSurfaceCard
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebReaderScreen(
    customSources: List<CustomScraperSource>,
    scrapedResult: ScrapedAnimeResult?,
    isScraping: Boolean,
    statusBanner: String?,
    errorBanner: String?,
    onRunScraper: (url: String, cardSel: String, epSel: String, streamSel: String) -> Unit,
    onSaveCustomSource: (name: String, url: String, cardSel: String, epSel: String, streamSel: String) -> Unit,
    onImportScrapedAnimeToApp: (ScrapedAnimeResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var urlInput by remember { mutableStateOf("") }
    var siteNameInput by remember { mutableStateOf("") }
    var cardSelector by remember { mutableStateOf(".anime-card, .anime-info-container") }
    var episodeSelector by remember { mutableStateOf(".ep-link, .episodes-card a") }
    var streamSelector by remember { mutableStateOf("iframe, video source") }
    var showSelectorMapper by remember { mutableStateOf(false) }
    var showFbJsonPreview by remember { mutableStateOf(true) }
    var copiedFeedback by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("web_reader_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassSurfaceCard(
                cornerRadius = 18.dp,
                borderColor = FacebookBlue.copy(alpha = 0.55f),
                containerColor = ObsidianSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = FacebookBlue
                        )
                        Column {
                            Text(
                                text = "قارئ وساحب المواقع الشامل (Bihi Web-Reader)",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "الصق رابط أي صفحة أنمي حقيقية لسحب بياناتها وحلقاتها وتحويلها لقاعدة بيانات فيسبوك *6",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    if (customSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "مصادرك المخصصة المحفوظة:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(customSources, key = { it.id }) { src ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ObsidianElevated,
                                    border = BorderStroke(1.dp, RoyalViolet.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            siteNameInput = src.siteName
                                            urlInput = src.baseUrl
                                            cardSelector = src.cardSelector
                                            episodeSelector = src.episodeSelector
                                            streamSelector = src.streamSelector
                                        }
                                        .testTag("scraper_source_${src.id}")
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = src.siteName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                        Text(
                                            text = src.cardSelector,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldStar6
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("رابط صفحة الأنمي الخارجي (https://...)") },
                        placeholder = { Text("https://...", color = TextSecondary) },
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
                            .testTag("scraper_url_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onRunScraper(urlInput, cardSelector, episodeSelector, streamSelector)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonNeon,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_scraper_button")
                        ) {
                            if (isScraping) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سحب الأنمي وتصفية الإعلانات",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = { showSelectorMapper = !showSelectorMapper },
                            border = BorderStroke(1.dp, FacebookBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_css_mapper_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "محددات CSS",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }

                    if (showSelectorMapper) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AmoledBlack.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, RoyalViolet.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "مخصص محددات CSS (Custom CSS Selector Mapper)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = RoyalViolet,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = siteNameInput,
                                    onValueChange = { siteNameInput = it },
                                    label = { Text("اسم الموقع لحفظ البطاقة") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = cardSelector,
                                    onValueChange = { cardSelector = it },
                                    label = { Text("محدد بطاقة الأنمي (مثال: .anime-card)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = episodeSelector,
                                    onValueChange = { episodeSelector = it },
                                    label = { Text("محدد روابط الحلقات (مثال: .ep-link)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = streamSelector,
                                    onValueChange = { streamSelector = it },
                                    label = { Text("محدد مشغل الفيديو (مثال: iframe, video)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onSaveCustomSource(
                                            siteNameInput,
                                            urlInput,
                                            cardSelector,
                                            episodeSelector,
                                            streamSelector
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalViolet,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ الموقع كبطاقة مصدر مخصصة")
                                }
                            }
                        }
                    }
                }
            }
        }

        errorBanner?.let { err ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CrimsonNeon.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, CrimsonNeon)
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelLarge,
                        color = CrimsonNeon,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        statusBanner?.let { msg ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldStar6.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, EmeraldStar6)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldStar6,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (scrapedResult != null) {
            item {
                GlassSurfaceCard(
                    cornerRadius = 18.dp,
                    borderColor = EmeraldStar6.copy(alpha = 0.6f),
                    containerColor = ObsidianSurface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Star6StreamBadge(text = "تم حجب ${scrapedResult.adsStrippedCount} سكربت إعلاني")
                            Text(
                                text = "المصدر: ${scrapedResult.studio}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scrapedResult.titleAr,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = scrapedResult.synopsisAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        scrapedResult.extractionSteps.forEach { step ->
                            Text(
                                text = "✓ $step",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldStar6,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "الروابط والحلقات المستخرجة (${scrapedResult.episodes.size}):",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        scrapedResult.episodes.forEach { ep ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "ح ${ep.number}: ${ep.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = ep.streamUrl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FacebookBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onImportScrapedAnimeToApp(scrapedResult) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldStar6,
                                    contentColor = AmoledBlack
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("import_scraped_to_app_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "إضافة الأنمي لمكتبة التطبيق *6",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Button(
                                onClick = {
                                    showFbJsonPreview = true
                                    clipboard.setText(AnnotatedString(scrapedResult.fbStar6Json))
                                    copiedFeedback = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FacebookBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("convert_to_fb_json_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (copiedFeedback) "تم نسخ كود فيسبوك *6!" else "تحويل لقاعدة بيانات فيسبوك *6",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        if (showFbJsonPreview) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmoledBlack,
                                border = BorderStroke(1.dp, FacebookBlue.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = scrapedResult.fbStar6Json,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = EmeraldStar6,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminFacebookStudioSheet(
    config: FacebookConfig,
    lastTokenValidation: TokenValidationResult?,
    isSyncing: Boolean,
    generatedJsonBlock: String,
    onUpdateConfig: (pageId: String, token: String, postId: String, mode: FacebookSyncMode) -> Unit,
    onValidateTokenOnly: () -> Unit,
    onTestAndSyncNow: () -> Unit,
    onImportRawCatalogJson: (String) -> Unit,
    onClearAllCatalogData: () -> Unit,
    onGenerateEpisodeJson: (
        animeTitle: String,
        storyAr: String,
        genresCsv: String,
        statusAr: String,
        epNumber: Int,
        isFiller: Boolean,
        fbVideoId: String,
        directSourceUrl: String
    ) -> Unit,
    onPublishGeneratedToLocalCatalog: (
        animeTitle: String,
        storyAr: String,
        genresCsv: String,
        statusAr: String,
        epNumber: Int,
        isFiller: Boolean,
        fbVideoId: String,
        directSourceUrl: String
    ) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var pageId by remember(config.pageId) { mutableStateOf(config.pageId) }
    var accessToken by remember(config.accessToken) { mutableStateOf(config.accessToken) }
    var catalogPostId by remember(config.catalogPostId) { mutableStateOf(config.catalogPostId) }
    var syncMode by remember(config.syncMode) { mutableStateOf(config.syncMode) }

    var genTitle by remember { mutableStateOf("") }
    var genStory by remember { mutableStateOf("") }
    var genGenres by remember { mutableStateOf("") }
    var genStatus by remember { mutableStateOf("مستمر") }
    var genEpNumber by remember { mutableStateOf("1") }
    var genIsFiller by remember { mutableStateOf(false) }
    var genVideoId by remember { mutableStateOf("") }
    var genDirectUrl by remember { mutableStateOf("") }
    var rawMasterJsonInput by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf(false) }

    Surface(
        color = AmoledBlack.copy(alpha = 0.98f),
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_facebook_studio_sheet")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Star6StreamBadge(text = "FacebookGraphService • Graph API v19.0")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "لوحة مصادقة وربط صفحة فيسبوك *6",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_admin_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }
            }

            // Section 1: إعدادات الربط والتحقق من التوكن (Token Validation & Catalog Sync)
            item {
                GlassSurfaceCard(
                    cornerRadius = 18.dp,
                    borderColor = if (config.isLiveConnected) EmeraldStar6 else FacebookBlue.copy(alpha = 0.65f),
                    containerColor = ObsidianSurface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (config.isLiveConnected) Icons.Default.VerifiedUser else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (config.isLiveConnected) EmeraldStar6 else AmberGold
                            )
                            Text(
                                text = "1. مصادقة التوكن وسحب كتالوج JSON (FacebookGraphService)",
                                style = MaterialTheme.typography.titleLarge,
                                color = FacebookBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmoledBlack.copy(alpha = 0.75f),
                            border = BorderStroke(
                                1.dp,
                                if (config.isLiveConnected) EmeraldStar6.copy(alpha = 0.6f) else AmberGold.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (config.isLiveConnected) {
                                        "حالة المصادقة: موثّق • الصفحة: ${config.connectedPageName}"
                                    } else {
                                        "حالة المصادقة: غير مصادق (الكتالوج فارغ ومقفل)"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (config.isLiveConnected) EmeraldStar6 else AmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = config.statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                lastTokenValidation?.let { v ->
                                    if (v.isValid) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Page ID: ${v.verifiedPageId} • التصنيف: ${v.pageCategory} • آخر تحقق: ${v.validatedAt}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldStar6
                                        )
                                    }
                                }
                            }
                        }

                        FacebookSyncMode.entries.forEach { mode ->
                            val selected = syncMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) FacebookBlue.copy(alpha = 0.2f) else ObsidianElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) FacebookBlue else GlassBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        syncMode = mode
                                        onUpdateConfig(pageId, accessToken, catalogPostId, mode)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = mode.titleAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = mode.subtitleAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = pageId,
                            onValueChange = {
                                pageId = it
                                onUpdateConfig(pageId, accessToken, catalogPostId, syncMode)
                            },
                            label = { Text("معرف صفحة فيسبوك (Facebook Page ID)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fb_page_id_input")
                        )

                        OutlinedTextField(
                            value = accessToken,
                            onValueChange = {
                                accessToken = it
                                onUpdateConfig(pageId, accessToken, catalogPostId, syncMode)
                            },
                            label = { Text("توكن الصفحة الدائم (Permanent Page Access Token)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fb_access_token_input")
                        )

                        OutlinedTextField(
                            value = catalogPostId,
                            onValueChange = {
                                catalogPostId = it
                                onUpdateConfig(pageId, accessToken, catalogPostId, syncMode)
                            },
                            label = { Text("معرف منشور قاعدة البيانات الموحد (Master Catalog Post ID - اختياري)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fb_catalog_post_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onValidateTokenOnly,
                                border = BorderStroke(1.dp, EmeraldStar6),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fb_validate_token_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldStar6,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "فحص التوكن فقط",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldStar6
                                )
                            }

                            Button(
                                onClick = onTestAndSyncNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FacebookBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("fb_test_sync_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مصادقة وسحب الكتالوج",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: مولد أكواد الحلقات والأنمي (Video Description & JSON Generator)
            item {
                GlassSurfaceCard(
                    cornerRadius = 18.dp,
                    borderColor = CrimsonNeon.copy(alpha = 0.55f),
                    containerColor = ObsidianSurface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "2. مولد أكواد الحلقات والأنمي (Video Description & JSON Generator)",
                            style = MaterialTheme.typography.titleLarge,
                            color = CrimsonNeon,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ولّد كود JSON المنسق للصقه في وصف فيديو فيسبوك، أو أضف الحلقة بعد مصادقة التوكن:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = genTitle,
                            onValueChange = { genTitle = it },
                            label = { Text("اسم الأنمي (Anime Title)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gen_anime_title_input")
                        )

                        OutlinedTextField(
                            value = genStory,
                            onValueChange = { genStory = it },
                            label = { Text("قصة الأنمي بالعربية (Arabic Story)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = genGenres,
                                onValueChange = { genGenres = it },
                                label = { Text("التصنيفات (مفصولة بفاصلة)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = genStatus,
                                onValueChange = { genStatus = it },
                                label = { Text("الحالة (مستمر/مكتمل)") },
                                singleLine = true,
                                modifier = Modifier.width(130.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = genEpNumber,
                                onValueChange = { genEpNumber = it },
                                label = { Text("رقم الحلقة") },
                                singleLine = true,
                                modifier = Modifier.width(105.dp)
                            )
                            OutlinedTextField(
                                value = genVideoId,
                                onValueChange = { genVideoId = it },
                                label = { Text("معرف فيديو فيسبوك (Video_ID)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gen_video_id_input")
                            )
                        }

                        OutlinedTextField(
                            value = genDirectUrl,
                            onValueChange = { genDirectUrl = it },
                            label = { Text("رابط MP4 مباشر اختياري (fbcdn.net)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "هل الحلقة فلر (Filler)؟",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Switch(
                                checked = genIsFiller,
                                onCheckedChange = { genIsFiller = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AmberGold
                                )
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val epInt = genEpNumber.toIntOrNull() ?: 1
                                    onGenerateEpisodeJson(
                                        genTitle,
                                        genStory,
                                        genGenres,
                                        genStatus,
                                        epInt,
                                        genIsFiller,
                                        genVideoId,
                                        genDirectUrl
                                    )
                                    clipboard.setText(AnnotatedString(generatedJsonBlock))
                                    copiedNotice = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonNeon,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("copy_generated_json_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (copiedNotice) "تم توليد ونسخ الكود!" else "توليد ونسخ الكود (Copy JSON)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Button(
                                onClick = {
                                    val epInt = genEpNumber.toIntOrNull() ?: 1
                                    onPublishGeneratedToLocalCatalog(
                                        genTitle,
                                        genStory,
                                        genGenres,
                                        genStatus,
                                        epInt,
                                        genIsFiller,
                                        genVideoId,
                                        genDirectUrl
                                    )
                                },
                                enabled = config.isLiveConnected && genTitle.isNotBlank() && (genVideoId.isNotBlank() || genDirectUrl.isNotBlank()),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldStar6,
                                    contentColor = AmoledBlack
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("publish_episode_to_app_button")
                            ) {
                                Text(
                                    text = "إضافة للكتالوج (يتطلب مصادقة)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        if (generatedJsonBlock.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmoledBlack,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generated_json_output_box")
                            ) {
                                Text(
                                    text = generatedJsonBlock,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = EmeraldStar6,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Direct Master Catalog JSON Import (Gated by Auth) & Session Reset
            item {
                GlassSurfaceCard(
                    cornerRadius = 18.dp,
                    borderColor = RoyalViolet.copy(alpha = 0.5f),
                    containerColor = ObsidianSurface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "3. استيراد كتالوج JSON الموحد وإدارة الجلسة",
                            style = MaterialTheme.typography.titleLarge,
                            color = RoyalViolet,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = rawMasterJsonInput,
                            onValueChange = { rawMasterJsonInput = it },
                            label = { Text("الصق كود JSON لقاعدة بيانات فيسبوك *6 هنا (يتطلب مصادقة التوكن)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onImportRawCatalogJson(rawMasterJsonInput)
                                    rawMasterJsonInput = ""
                                },
                                enabled = config.isLiveConnected && rawMasterJsonInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalViolet,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("استيراد JSON للكتالوج")
                            }

                            OutlinedButton(
                                onClick = onClearAllCatalogData,
                                border = BorderStroke(1.dp, CrimsonNeon),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("logout_clear_catalog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = CrimsonNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("قفل وتصفير الكتالوج", color = CrimsonNeon)
                            }
                        }
                    }
                }
            }
        }
    }
}
