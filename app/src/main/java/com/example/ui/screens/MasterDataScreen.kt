package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState
import com.example.util.AppStrings
import com.example.util.LocationHelper

@Composable
fun MasterDataScreen(
    viewModel: SfaViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val warungs by viewModel.warungs.collectAsState()
    val rutes by viewModel.rutes.collectAsState()
    val pabriks by viewModel.pabriks.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        AppStrings.tabProductUom(lang),
        AppStrings.tabOutlets(lang),
        AppStrings.tabRoutes(lang),
        AppStrings.tabSuppliers(lang)
    )

    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> viewModel.openTransactionDialog(TransactionDialogState.AddEditProduct(null))
                        1 -> viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(null))
                        2 -> viewModel.openTransactionDialog(TransactionDialogState.AddEditRute(null))
                        3 -> viewModel.openTransactionDialog(TransactionDialogState.AddEditPabrik(null))
                    }
                },
                containerColor = Slate900,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("fab_add_master")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = AppStrings.btnAdd(lang))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Screen Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Slate100)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Navigasi",
                                tint = Slate800,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = AppStrings.masterTitle(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = AppStrings.masterSubtitle(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Tab Rows with High Contrast Pill Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Slate900,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Slate900,
                            height = 3.dp
                        )
                    }
                },
                divider = { HorizontalDivider(color = Slate200, thickness = 1.dp) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isTabSelected = selectedTab == index
                    Tab(
                        selected = isTabSelected,
                        onClick = { selectedTab = index },
                        selectedContentColor = Slate900,
                        unselectedContentColor = Slate600,
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isTabSelected) Slate900 else Slate600
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // PRODUK TAB
                        itemsIndexed(products, key = { _, item -> item.id }, contentType = { _, _ -> "product_item" }) { index, product ->
                            val matchedPabrik = pabriks.find { it.id == product.pabrikId }
                            ProductMasterCard(
                                product = product,
                                orderNumber = index + 1,
                                pabrikName = matchedPabrik?.namaPabrik,
                                lang = lang,
                                onEdit = {
                                    viewModel.openTransactionDialog(TransactionDialogState.AddEditProduct(product))
                                },
                                onDelete = {
                                    itemToDelete = product
                                }
                            )
                        }
                    }
                    1 -> {
                        // WARUNG TAB
                        itemsIndexed(warungs, key = { _, item -> item.id }, contentType = { _, _ -> "warung_item" }) { index, warung ->
                            val matchedRute = rutes.find { it.id == warung.ruteId }
                            val ruteLabel = if (matchedRute != null) "${matchedRute.namaRute.split("-").first().trim()} (${matchedRute.hariKunjungan})" else warung.ruteId
                            WarungMasterCard(
                                warung = warung,
                                orderNumber = index + 1,
                                ruteName = ruteLabel,
                                lang = lang,
                                onOpenMaps = if (warung.latitude != 0.0 || warung.longitude != 0.0) {
                                    { LocationHelper.openGoogleMapsNavigation(context, warung.latitude, warung.longitude, warung.namaWarung) }
                                } else null,
                                onEdit = {
                                    viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(warung))
                                },
                                onViewStatistics = {
                                    viewModel.openTransactionDialog(TransactionDialogState.OutletStatistics(warung))
                                },
                                onDelete = {
                                    itemToDelete = warung
                                }
                            )
                        }
                    }
                    2 -> {
                        // RUTE TAB
                        itemsIndexed(rutes, key = { _, item -> item.id }, contentType = { _, _ -> "rute_item" }) { index, rute ->
                            val routeWarungs = warungs.filter { it.ruteId == rute.id && it.status == "Aktif" }
                            RuteMasterCard(
                                rute = rute,
                                orderNumber = index + 1,
                                mappedWarungCount = routeWarungs.count { it.latitude != 0.0 || it.longitude != 0.0 },
                                lang = lang,
                                onNavigateRoute = {
                                    LocationHelper.openMultiStopGoogleMapsRoute(context, routeWarungs, rute.namaRute)
                                },
                                onEdit = {
                                    viewModel.openTransactionDialog(TransactionDialogState.AddEditRute(rute))
                                },
                                onDelete = {
                                    itemToDelete = rute
                                }
                            )
                        }
                    }
                    3 -> {
                        // PABRIK TAB
                        itemsIndexed(pabriks, key = { _, item -> item.id }, contentType = { _, _ -> "pabrik_item" }) { index, pabrik ->
                            val suppliedProducts = products.filter { it.pabrikId == pabrik.id }
                            PabrikMasterCard(
                                pabrik = pabrik,
                                orderNumber = index + 1,
                                suppliedProducts = suppliedProducts,
                                lang = lang,
                                onEdit = {
                                    viewModel.openTransactionDialog(TransactionDialogState.AddEditPabrik(pabrik))
                                },
                                onDelete = {
                                    itemToDelete = pabrik
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Delete Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(AppStrings.deleteConfirmTitle(lang), fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                val item = itemToDelete
                val name = when (item) {
                    is ProductEntity -> "${AppStrings.tr("Produk", "Product", lang)}: ${item.nama}"
                    is WarungEntity -> "${AppStrings.tr("Outlet", "Outlet", lang)}: ${item.namaWarung}"
                    is RuteEntity -> "${AppStrings.tr("Rute", "Route", lang)}: ${item.namaRute}"
                    is PabrikEntity -> "${AppStrings.tr("Supplier", "Supplier", lang)}: ${item.namaPabrik}"
                    else -> AppStrings.tr("item ini", "this item", lang)
                }
                Text(AppStrings.deleteConfirmMsg(name, lang), color = Slate700)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = itemToDelete
                        when (item) {
                            is ProductEntity -> viewModel.deleteProduct(item)
                            is WarungEntity -> viewModel.deleteWarung(item)
                            is RuteEntity -> viewModel.deleteRute(item)
                            is PabrikEntity -> viewModel.deletePabrik(item)
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) {
                    Text(AppStrings.btnDelete(lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text(AppStrings.btnCancel(lang), color = Slate700)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun ProductMasterCard(
    product: ProductEntity,
    orderNumber: Int = 1,
    pabrikName: String? = null,
    lang: String = "ID",
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate900
                    ) {
                        Text(
                            text = "#$orderNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Column {
                        Text(
                            text = product.nama,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = product.kategori,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600,
                                fontSize = 11.sp
                            )
                            if (!pabrikName.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BlueSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BlueBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PrecisionManufacturing,
                                            contentDescription = null,
                                            tint = BlueAccent,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = pabrikName,
                                            color = BlueAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Text(
                        text = "1 ${product.satuanBesar} = ${product.rasioKonversi} Pcs",
                        color = Slate800,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = AppStrings.buyPriceFactory(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${SfaViewModel.formatRupiah(product.hargaBeliPabrik)} / ${product.satuanBesar}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = AppStrings.sellPriceConsignment(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${SfaViewModel.formatRupiah(product.hargaJualDefault)} / Pcs",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = AppStrings.btnDelete(lang), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.btnDelete(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Slate100,
                        contentColor = Slate900
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = AppStrings.btnEdit(lang), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.btnEdit(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WarungMasterCard(
    warung: WarungEntity,
    orderNumber: Int = 1,
    ruteName: String = "",
    lang: String = "ID",
    onOpenMaps: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onViewStatistics: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Enlarged Outlet Photo (64.dp x 64.dp) with Order Number Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    if (warung.fotoOutlet != null) {
                        AsyncImage(
                            model = warung.fotoOutlet,
                            contentDescription = "Foto Outlet",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 6.dp),
                        color = Slate900.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "#$orderNumber",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = warung.namaWarung,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (warung.status == "Aktif") EmeraldSurface else AmberSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (warung.status == "Aktif") EmeraldBorder else AmberBorder)
                        ) {
                            Text(
                                text = if (warung.status == "Aktif") AppStrings.tr("Aktif", "Active", lang) else warung.status,
                                color = if (warung.status == "Aktif") EmeraldText else AmberText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${warung.namaPemilik.ifEmpty { "-" }} • ${warung.kategoriWarung}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        fontSize = 11.sp
                    )

                    Text(
                        text = warung.alamatLengkap.ifEmpty { AppStrings.tr("Alamat belum diatur", "Address not set", lang) },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Note snippet if present
            if (warung.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = AmberText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = warung.notes,
                            fontSize = 11.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ruteName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate100,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Text(
                            text = "${AppStrings.routeAssignment(lang)}: $ruteName",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "${AppStrings.tr("Limit Hutang:", "Credit Limit:", lang)} ${SfaViewModel.formatRupiah(warung.limitHutangMaksimal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate700,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "GPS: ${String.format(java.util.Locale.US, "%.4f, %.4f", warung.latitude, warung.longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlueAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenMaps != null) {
                    FilledTonalButton(
                        onClick = onOpenMaps,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BlueSurface,
                            contentColor = BlueAccent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Directions, contentDescription = "Maps", modifier = Modifier.size(14.dp), tint = BlueAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                OutlinedButton(
                    onClick = onViewStatistics,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate800),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = SolidColor(Slate300))
                ) {
                    Icon(imageVector = Icons.Default.QueryStats, contentDescription = AppStrings.outletStats(lang), modifier = Modifier.size(14.dp), tint = Slate700)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.outletStats(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = AppStrings.btnDelete(lang), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.btnDelete(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                FilledTonalButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Slate100,
                        contentColor = Slate900
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = AppStrings.btnEdit(lang), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.btnEdit(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RuteMasterCard(
    rute: RuteEntity,
    orderNumber: Int = 1,
    mappedWarungCount: Int = 0,
    lang: String = "ID",
    onNavigateRoute: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate900
                    ) {
                        Text(
                            text = "#$orderNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = rute.namaRute,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${AppStrings.tr("Hari:", "Day:", lang)} ${rute.hariKunjungan} • Salesman: ${rute.idSalesman}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                        Text(
                            text = "${AppStrings.tr("Estimasi:", "Estimate:", lang)} ${rute.estimasiJumlahWarung} Outlet • ${rute.jarakTotalKm} km" + if (mappedWarungCount > 0) " • $mappedWarungCount ${AppStrings.mappedOutlets(lang)}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlueAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBorder)
                ) {
                    Text(
                        text = if (rute.status == "Aktif") AppStrings.tr("Aktif", "Active", lang) else rute.status,
                        color = EmeraldText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateRoute != null && mappedWarungCount > 0) {
                    FilledTonalButton(
                        onClick = onNavigateRoute,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BlueSurface,
                            contentColor = BlueAccent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Directions, contentDescription = AppStrings.openMapsRoute(lang), modifier = Modifier.size(14.dp), tint = BlueAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.openMapsRoute(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = AppStrings.btnDelete(lang), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.btnDelete(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Slate100,
                        contentColor = Slate900
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = AppStrings.btnEdit(lang), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.btnEdit(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PabrikMasterCard(
    pabrik: PabrikEntity,
    orderNumber: Int = 1,
    suppliedProducts: List<ProductEntity> = emptyList(),
    lang: String = "ID",
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate900
                    ) {
                        Text(
                            text = "#$orderNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = pabrik.namaPabrik,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (suppliedProducts.isNotEmpty()) BlueSurface else Slate100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (suppliedProducts.isNotEmpty()) BlueBorder else Slate200)
                ) {
                    Text(
                        text = "${suppliedProducts.size} ${AppStrings.tr("SKU Terhubung", "Linked SKUs", lang)}",
                        color = if (suppliedProducts.isNotEmpty()) BlueAccent else Slate600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = "Contact Person: ${pabrik.namaCp} (${pabrik.noHpCp})",
                style = MaterialTheme.typography.bodySmall,
                color = Slate700
            )

            // Kebijakan retur card
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = RoseSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoseBorder)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = RoseDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${AppStrings.tr("Kebijakan Retur:", "Return Policy:", lang)} ${pabrik.kebijakanRetur}",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoseText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // List of supplied products if any
            if (suppliedProducts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "${AppStrings.tr("Produk Pasokan Supplier:", "Supplier Products Supplied:", lang)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600
                        )
                        Text(
                            text = suppliedProducts.joinToString(", ") { it.nama },
                            fontSize = 11.sp,
                            color = Slate900,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = "${AppStrings.tr("Rekening:", "Bank Account:", lang)} ${pabrik.rekeningBank}",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
                fontSize = 11.sp
            )

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = AppStrings.btnDelete(lang), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.btnDelete(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Slate100,
                        contentColor = Slate900
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = AppStrings.btnEdit(lang), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.btnEdit(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
