package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AnimeVisualTheme
import com.example.model.NetworkMode
import com.example.model.PosterEmblemStyle
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
fun NetworkWhitelistTopBar(
    networkMode: NetworkMode,
    isLiveFbConnected: Boolean,
    onToggleNetworkMode: () -> Unit,
    onOpenAdminStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isStar6 = networkMode == NetworkMode.STAR6_FBCDN
    val badgeBorderColor = if (isStar6) EmeraldStar6.copy(alpha = 0.65f) else RoyalViolet.copy(alpha = 0.6f)
    val badgeBgColor = if (isStar6) FacebookBlue.copy(alpha = 0.18f) else ObsidianElevated

    Surface(
        color = AmoledBlack.copy(alpha = 0.94f),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Title & Dual-Core Identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CrimsonNeon, RoyalViolet, FacebookBlue)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "*6",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "أنمي بيهي",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CrimsonNeon.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SLAYER × WITCHER",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonNeon,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = if (isLiveFbConnected) "متصل بصفحة فيسبوك *6 المباشرة" else "محرك فيسبوك *6 السحابي + قارئ المواقع",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Network Whitelist Indicator + Admin Studio Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Network Whitelist Indicator Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBgColor,
                    border = BorderStroke(1.dp, badgeBorderColor),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggleNetworkMode() }
                        .testTag("network_mode_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isStar6) EmeraldStar6 else CrimsonNeon)
                        )
                        Icon(
                            imageVector = if (isStar6) Icons.Default.CloudDone else Icons.Default.Wifi,
                            contentDescription = networkMode.labelAr,
                            tint = if (isStar6) FacebookBlue else RoyalViolet,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = networkMode.labelAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Admin & FB Connect Studio Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FacebookBlue.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, FacebookBlue.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenAdminStudio() }
                        .testTag("admin_studio_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "لوحة فيسبوك *6",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = GlassBorder,
    containerColor: Color = ObsidianSurface.copy(alpha = 0.88f),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() }
    } else {
        modifier.clip(RoundedCornerShape(cornerRadius))
    }

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = cardModifier
    ) {
        content()
    }
}

/**
 * Zero-External-Runtime-Asset Artwork renderer:
 * Draws rich anime key-visual vector/canvas art locally for 100% *6 zero-rated compatibility,
 * and overlays fbcdn.net image if provided.
 */
@Composable
fun AnimeZeroRatedPoster(
    visualTheme: AnimeVisualTheme,
    titleEn: String,
    posterFbcdnUrl: String = "",
    modifier: Modifier = Modifier
) {
    val primary = Color(visualTheme.primaryColorHex)
    val secondary = Color(visualTheme.secondaryColorHex)
    val accent = Color(visualTheme.accentColorHex)

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(secondary, AmoledBlack, primary.copy(alpha = 0.42f))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Glowing Moon / Cyber Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.72f, h * 0.26f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.72f, h * 0.26f),
                radius = w * 0.55f
            )

            // Secondary Aura Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(w * 0.25f, h * 0.72f),
                    radius = w * 0.48f
                ),
                center = Offset(w * 0.25f, h * 0.72f),
                radius = w * 0.48f
            )

            // Stylized Anime Emblem Silhouette per series
            when (visualTheme.emblemStyle) {
                PosterEmblemStyle.CRIMSON_KATANA -> {
                    val bladePath = Path().apply {
                        moveTo(w * 0.18f, h * 0.82f)
                        lineTo(w * 0.84f, h * 0.16f)
                        lineTo(w * 0.89f, h * 0.22f)
                        lineTo(w * 0.24f, h * 0.86f)
                        close()
                    }
                    drawPath(bladePath, color = primary.copy(alpha = 0.85f))
                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = Offset(w * 0.2f, h * 0.84f),
                        end = Offset(w * 0.86f, h * 0.18f),
                        strokeWidth = 3.5f
                    )
                }
                PosterEmblemStyle.SHADOW_MONARCH -> {
                    val crownPath = Path().apply {
                        moveTo(w * 0.25f, h * 0.65f)
                        lineTo(w * 0.32f, h * 0.34f)
                        lineTo(w * 0.50f, h * 0.50f)
                        lineTo(w * 0.68f, h * 0.34f)
                        lineTo(w * 0.75f, h * 0.65f)
                        close()
                    }
                    drawPath(crownPath, color = primary.copy(alpha = 0.75f))
                    drawPath(crownPath, color = accent, style = Stroke(width = 3f))
                }
                PosterEmblemStyle.CYBER_SHINOBI -> {
                    // Cyber Visor & Shuriken Star
                    val shuriken = Path().apply {
                        moveTo(w * 0.5f, h * 0.22f)
                        lineTo(w * 0.6f, h * 0.44f)
                        lineTo(w * 0.82f, h * 0.5f)
                        lineTo(w * 0.6f, h * 0.56f)
                        lineTo(w * 0.5f, h * 0.78f)
                        lineTo(w * 0.4f, h * 0.56f)
                        lineTo(w * 0.18f, h * 0.5f)
                        lineTo(w * 0.4f, h * 0.44f)
                        close()
                    }
                    drawPath(shuriken, color = primary.copy(alpha = 0.7f))
                    drawCircle(
                        color = AmoledBlack,
                        radius = w * 0.08f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                }
                PosterEmblemStyle.CURSED_SORCERER -> {
                    drawCircle(
                        color = accent.copy(alpha = 0.7f),
                        radius = w * 0.24f,
                        center = Offset(w * 0.5f, h * 0.46f),
                        style = Stroke(width = 5f)
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.85f),
                        radius = w * 0.11f,
                        center = Offset(w * 0.5f, h * 0.46f)
                    )
                }
                PosterEmblemStyle.SOLAR_PIRATE -> {
                    drawCircle(
                        color = primary.copy(alpha = 0.8f),
                        radius = w * 0.22f,
                        center = Offset(w * 0.5f, h * 0.46f)
                    )
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.18f, h * 0.46f),
                        end = Offset(w * 0.82f, h * 0.46f),
                        strokeWidth = 6f
                    )
                }
                PosterEmblemStyle.AZURE_ALCHEMIST -> {
                    val diamond = Path().apply {
                        moveTo(w * 0.5f, h * 0.24f)
                        lineTo(w * 0.74f, h * 0.48f)
                        lineTo(w * 0.5f, h * 0.72f)
                        lineTo(w * 0.26f, h * 0.48f)
                        close()
                    }
                    drawPath(diamond, color = primary.copy(alpha = 0.7f))
                    drawPath(diamond, color = Color.White.copy(alpha = 0.7f), style = Stroke(width = 3f))
                }
            }
        }

        if (posterFbcdnUrl.isNotBlank()) {
            AsyncImage(
                model = posterFbcdnUrl,
                contentDescription = titleEn,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtle Bottom Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AmoledBlack.copy(alpha = 0.45f),
                            AmoledBlack.copy(alpha = 0.92f)
                        )
                    )
                )
        )
    }
}

@Composable
fun MalRatingBadge(score: Double, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AmoledBlack.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "تقييم MAL",
                tint = AmberGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = String.format(java.util.Locale.US, "%.2f", score),
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FillerOrCanonBadge(isFiller: Boolean, modifier: Modifier = Modifier) {
    val bgColor = if (isFiller) AmberGold.copy(alpha = 0.2f) else EmeraldStar6.copy(alpha = 0.2f)
    val strokeColor = if (isFiller) AmberGold.copy(alpha = 0.6f) else EmeraldStar6.copy(alpha = 0.6f)
    val textColor = if (isFiller) AmberGold else EmeraldStar6
    val label = if (isFiller) "فلر (Filler)" else "قصة أصلية (Canon)"

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, strokeColor),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun Star6StreamBadge(text: String = "*6 fbcdn.net", modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = FacebookBlue.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, FacebookBlue.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(EmeraldStar6)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
