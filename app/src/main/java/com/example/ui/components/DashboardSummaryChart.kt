package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.*
import java.util.*

@Composable
fun DashboardSummaryChart(
    todayTransactions: List<TransactionEntity>,
    totalWarungsInRoute: Int,
    modifier: Modifier = Modifier,
    lang: String = "ID"
) {
    // 1. Core metric calculations for today
    val totalSalesVolumePcs = remember(todayTransactions) {
        todayTransactions.sumOf { it.pcsLaku }
    }

    val totalKonsinyasiDropPcs = remember(todayTransactions) {
        todayTransactions.sumOf { it.restockBaruPcs }
    }

    val totalReturDitarikPcs = remember(todayTransactions) {
        todayTransactions.sumOf { it.bsDitarikPcs }
    }

    val distinctVisitedOutletIds = remember(todayTransactions) {
        todayTransactions.map { it.warungId }.distinct()
    }
    val visitedOutletCount = distinctVisitedOutletIds.size
    val totalTargetOutlets = totalWarungsInRoute

    val visitRatio = if (totalTargetOutlets > 0) {
        (visitedOutletCount.toFloat() / totalTargetOutlets.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // 2. Breakdown by time slots (4 operational sessions)
    val timeSlotData = remember(todayTransactions, lang) {
        val slots = mutableListOf(
            TimeSlotStat(
                name = com.example.util.AppStrings.tr("Pagi", "Morning", lang),
                timeRange = "07:00 - 10:00",
                icon = Icons.Default.WbSunny,
                volumePcs = 0,
                visitCount = 0
            ),
            TimeSlotStat(
                name = com.example.util.AppStrings.tr("Siang", "Noon", lang),
                timeRange = "10:00 - 13:00",
                icon = Icons.Default.LightMode,
                volumePcs = 0,
                visitCount = 0
            ),
            TimeSlotStat(
                name = com.example.util.AppStrings.tr("Sore", "Afternoon", lang),
                timeRange = "13:00 - 16:00",
                icon = Icons.Default.WbTwilight,
                volumePcs = 0,
                visitCount = 0
            ),
            TimeSlotStat(
                name = com.example.util.AppStrings.tr("Closing", "Closing", lang),
                timeRange = "16:00+",
                icon = Icons.Default.NightsStay,
                volumePcs = 0,
                visitCount = 0
            )
        )

        val cal = Calendar.getInstance()
        for (tx in todayTransactions) {
            cal.timeInMillis = tx.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slotIndex = when {
                hour < 10 -> 0
                hour < 13 -> 1
                hour < 16 -> 2
                else -> 3
            }
            slots[slotIndex] = slots[slotIndex].copy(
                volumePcs = slots[slotIndex].volumePcs + tx.pcsLaku,
                visitCount = slots[slotIndex].visitCount + 1
            )
        }
        slots
    }

    val maxVolumeSlot = (timeSlotData.maxOfOrNull { it.volumePcs } ?: 0)
    val peakSlot = if (maxVolumeSlot > 0) timeSlotData.maxByOrNull { it.volumePcs } else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = com.example.util.AppStrings.tr("PERFORMA & DISTRIBUSI HARIAN", "DAILY PERFORMANCE & DISTRIBUTION", lang),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = com.example.util.AppStrings.tr("Aktivitas Kunjungan & Sesi Penjualan", "Visit Activity & Sales Sessions", lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = com.example.util.AppStrings.tr("Real-time", "Live Today", lang),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Dual Key Metric Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Sales Volume
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = com.example.util.AppStrings.tr("Volume Terjual", "Sold Volume", lang),
                                fontSize = 11.sp,
                                color = Slate600,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Text(
                            text = "$totalSalesVolumePcs Pcs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BlueSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "+$totalKonsinyasiDropPcs Drop",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueText,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (totalReturDitarikPcs > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AmberSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = "$totalReturDitarikPcs Retur",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberText,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Outlet Visits & Route Progress
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = com.example.util.AppStrings.tr("Progres Rute", "Route Progress", lang),
                                fontSize = 11.sp,
                                color = Slate600,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(BlueSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Text(
                            text = if (totalTargetOutlets > 0) "$visitedOutletCount / $totalTargetOutlets" else "$visitedOutletCount ${com.example.util.AppStrings.tr("Toko", "Stores", lang)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Route Progress Bar
                        if (totalTargetOutlets > 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                LinearProgressIndicator(
                                    progress = { visitRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (visitRatio >= 0.7f) EmeraldSuccess else BlueAccent,
                                    trackColor = Slate200,
                                )
                                Text(
                                    text = "${(visitRatio * 100).toInt()}% • ${com.example.util.AppStrings.tr("Sisa", "Remaining", lang)} ${totalTargetOutlets - visitedOutletCount} ${com.example.util.AppStrings.tr("Toko", "Stores", lang)}",
                                    fontSize = 9.sp,
                                    color = if (visitRatio >= 0.7f) EmeraldText else Slate500,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            Text(
                                text = if (visitedOutletCount > 0) com.example.util.AppStrings.tr("Kunjungan Aktif", "Active Visits", lang) else com.example.util.AppStrings.tr("Belum Ada Kunjungan", "No Visits Yet", lang),
                                fontSize = 10.sp,
                                color = if (visitedOutletCount > 0) EmeraldSuccess else Slate500,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Session Distribution Section (4 Time Slots with High Polish)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Slate50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = com.example.util.AppStrings.tr("DISTRIBUSI SESI WAKTU", "SESSION TIME DISTRIBUTION", lang),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Slate900))
                                Text(com.example.util.AppStrings.tr("Volume", "Volume", lang), fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BlueAccent))
                                Text(com.example.util.AppStrings.tr("Kunjungan", "Visits", lang), fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // 4 Session Cards Grid (2 rows x 2 columns for clean readability)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SessionCard(
                                slot = timeSlotData[0],
                                isPeak = peakSlot == timeSlotData[0] && timeSlotData[0].volumePcs > 0,
                                maxVolume = maxVolumeSlot,
                                modifier = Modifier.weight(1f),
                                lang = lang
                            )
                            SessionCard(
                                slot = timeSlotData[1],
                                isPeak = peakSlot == timeSlotData[1] && timeSlotData[1].volumePcs > 0,
                                maxVolume = maxVolumeSlot,
                                modifier = Modifier.weight(1f),
                                lang = lang
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SessionCard(
                                slot = timeSlotData[2],
                                isPeak = peakSlot == timeSlotData[2] && timeSlotData[2].volumePcs > 0,
                                maxVolume = maxVolumeSlot,
                                modifier = Modifier.weight(1f),
                                lang = lang
                            )
                            SessionCard(
                                slot = timeSlotData[3],
                                isPeak = peakSlot == timeSlotData[3] && timeSlotData[3].volumePcs > 0,
                                maxVolume = maxVolumeSlot,
                                modifier = Modifier.weight(1f),
                                lang = lang
                            )
                        }
                    }

                    // Tactical Session Insight Line
                    val insightText = if (peakSlot != null && peakSlot.volumePcs > 0) {
                        if (lang == "EN") {
                            "⚡ Peak session today: ${peakSlot.name} (${peakSlot.volumePcs} Pcs • ${peakSlot.visitCount} visits)"
                        } else {
                            "⚡ Sesi teramai hari ini: ${peakSlot.name} (${peakSlot.volumePcs} Pcs • ${peakSlot.visitCount} kunjungan toko)"
                        }
                    } else {
                        com.example.util.AppStrings.tr("ℹ️ Belum ada transaksi sesi tercatat hari ini.", "ℹ️ No session transaction recorded today yet.", lang)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = insightText,
                                fontSize = 10.sp,
                                color = if (peakSlot != null && peakSlot.volumePcs > 0) Slate800 else Slate500,
                                fontWeight = if (peakSlot != null && peakSlot.volumePcs > 0) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    slot: TimeSlotStat,
    isPeak: Boolean,
    maxVolume: Int,
    modifier: Modifier = Modifier,
    lang: String = "ID"
) {
    val hasActivity = slot.volumePcs > 0 || slot.visitCount > 0
    val volumeFraction = if (maxVolume > 0) (slot.volumePcs.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isPeak) BlueSurface.copy(alpha = 0.6f) else SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPeak) BlueBorder else Slate200
        )
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Session Name, Time, and Peak Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = slot.icon,
                        contentDescription = null,
                        tint = if (isPeak) BlueAccent else if (hasActivity) Slate700 else Slate400,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = slot.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasActivity) Slate900 else Slate600
                    )
                }

                if (isPeak) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AmberSurface
                    ) {
                        Text(
                            text = "⭐ Peak",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Text(
                        text = slot.timeRange,
                        fontSize = 9.sp,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Stats Row: Volume & Visit Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${slot.volumePcs} Pcs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (slot.volumePcs > 0) Slate900 else Slate400
                    )
                    Text(
                        text = "${slot.visitCount} ${com.example.util.AppStrings.tr("Toko", "Visits", lang)}",
                        fontSize = 9.sp,
                        color = if (slot.visitCount > 0) BlueText else Slate400,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Mini session visual bar
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Slate200)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(volumeFraction.coerceAtLeast(if (hasActivity) 0.15f else 0f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isPeak) BlueAccent else if (slot.volumePcs > 0) Slate800 else Slate300)
                    )
                }
            }
        }
    }
}

private data class TimeSlotStat(
    val name: String,
    val timeRange: String,
    val icon: ImageVector,
    val volumePcs: Int,
    val visitCount: Int
)

