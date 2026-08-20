package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MinimalStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(Slate200)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DrawerInventorySummary(
    stokFresh: Int,
    stokBsBelumSortir: Int,
    stokPribadiLayak: Int,
    stokPribadiRusak: Int,
    onSortirClick: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "ID"
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(Slate200)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInbox,
                            contentDescription = null,
                            tint = Slate800,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = com.example.util.AppStrings.tr("4 LACI VIRTUAL INVENTORY", "4 VIRTUAL INVENTORY DRAWERS", lang),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = com.example.util.AppStrings.tr("Pemisahan Hak Aset & Fisik Dus/Pcs", "Asset Rights Separation & Boxes/Units", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.clickable { onSortirClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = com.example.util.AppStrings.tr("Sortir Retur", "Return Sorting", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate800,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2x2 Clean Drawer Panels with Subtle Neutral Backgrounds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Laci 1: Fresh Pabrik
                DrawerItem(
                    label = com.example.util.AppStrings.tr("Fresh Pabrik", "Fresh Factory", lang),
                    owner = com.example.util.AppStrings.tr("Milik Pabrik", "Supplier Asset", lang),
                    count = "$stokFresh Pcs",
                    dotColor = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )

                // Laci 2: Retur Tarikan (Belum Sortir)
                DrawerItem(
                    label = com.example.util.AppStrings.tr("Retur Tarikan", "Unsorted Returns", lang),
                    owner = com.example.util.AppStrings.tr("Perlu Dipilah", "Needs Sorting", lang),
                    count = "$stokBsBelumSortir Pcs",
                    dotColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Laci 3: Modal Pribadi Layak Jual
                DrawerItem(
                    label = com.example.util.AppStrings.tr("Aset Pribadi (Repack)", "Personal (Repack)", lang),
                    owner = com.example.util.AppStrings.tr("100% Hak Sales", "100% Sales Margin", lang),
                    count = "$stokPribadiLayak Pcs",
                    dotColor = IndigoAsset,
                    modifier = Modifier.weight(1f)
                )

                // Laci 4: Pribadi Rusak / Write-off
                DrawerItem(
                    label = com.example.util.AppStrings.tr("Rusak / Afkir (Dibuang)", "Damaged / Waste (Scrapped)", lang),
                    owner = com.example.util.AppStrings.tr("Kerugian Pribadi", "Personal Loss", lang),
                    count = "$stokPribadiRusak Pcs",
                    dotColor = RoseDanger,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    owner: String,
    count: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Slate50)
            .border(1.dp, Slate200, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = owner,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = Slate500,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RiskAgingBadge(
    daysOverdue: Int,
    saldoPiutang: Double,
    modifier: Modifier = Modifier,
    lang: String = "ID"
) {
    val (bg, borderCol, textCol, label) = when {
        saldoPiutang <= 0 -> Quadruple(EmeraldSurface, EmeraldBorder, EmeraldText, com.example.util.AppStrings.tr("Lancar (Rp 0)", "Current (Rp 0)", lang))
        daysOverdue <= 7 -> Quadruple(EmeraldSurface, EmeraldBorder, EmeraldText, com.example.util.AppStrings.tr("Current (≤7 hr)", "Current (≤7 d)", lang))
        daysOverdue <= 14 -> Quadruple(AmberSurface, AmberBorder, AmberText, com.example.util.AppStrings.tr("Overdue 1 (8-14 hr)", "Overdue 1 (8-14 d)", lang))
        daysOverdue <= 21 -> Quadruple(AmberSurface, AmberBorder, AmberText, com.example.util.AppStrings.tr("Overdue 2 (Tagih!)", "Overdue 2 (Collect!)", lang))
        else -> Quadruple(RoseSurface, RoseBorder, RoseText, com.example.util.AppStrings.tr("Overdue 3 (>21 hr STOP)", "Overdue 3 (>21 d STOP)", lang))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
    ) {
        Text(
            text = label,
            color = textCol,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

