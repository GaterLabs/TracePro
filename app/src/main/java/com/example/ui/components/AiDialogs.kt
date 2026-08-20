package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.AiConfig
import com.example.data.ai.AiPromptBuilder
import com.example.data.local.entity.WarungEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AiCopilotDialog(
    viewModel: SfaViewModel,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by viewModel.aiChatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    val lang = com.example.util.LocalAppLanguage.current

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Explicitly enforce Light Theme for Copilot Modal across all Android System Modes
    MaterialTheme(colorScheme = HighContrastEnterpriseColorScheme) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.94f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = Slate900
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Bar (Professional Dark Slate with Emerald Highlights)
                    Surface(
                        color = Slate900,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AiRobotAvatar(
                                    size = 38.dp,
                                    containerBackground = Slate900,
                                    accentColor = EmeraldPrimary
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "TracerPro AI Copilot",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Surface(
                                            color = Slate800,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = aiConfig.model.ifBlank { "OpenAI Compatible" },
                                                color = EmeraldPrimary,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = com.example.util.AppStrings.tr("Asisten Cerdas & Analis Bisnis FMCG", "Smart Assistant & FMCG Business Analyst", lang),
                                        color = Slate400,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = com.example.util.AppStrings.tr("Pengaturan AI Gateway", "AI Gateway Settings", lang),
                                        tint = Slate300,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearAiChatHistory() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = com.example.util.AppStrings.tr("Bersihkan Chat", "Clear Chat", lang),
                                        tint = Slate300,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = com.example.util.AppStrings.tr("Tutup", "Close", lang),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Action Chips Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                viewModel.sendAiChatMessage(if (lang == "EN") "Create a WhatsApp report draft summarizing today's sales, cash collections, factory settlements, and outstanding credit." else "Buatkan draf laporan WhatsApp rekap hasil penjualan, kas, setoran pabrik, dan piutang hari ini untuk dikirim ke bos distributor.")
                            },
                            icon = { Icon(Icons.Default.Send, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp)) },
                            label = { Text(com.example.util.AppStrings.tr("Draf WA Bos Hari Ini", "Today's WA Draft", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate800) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Slate100)
                        )
                        SuggestionChip(
                            onClick = {
                                viewModel.sendAiChatMessage(if (lang == "EN") "Analyze outstanding debt health across all stores. Which outlets have highest default risk and need priority collection?" else "Analisis kesehatan piutang seluruh warung saat ini. Mana toko yang paling berisiko macet dan perlu diprioritaskan penagihan?")
                            },
                            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp)) },
                            label = { Text(com.example.util.AppStrings.tr("Audit Warung Macet", "Audit Risky Debt Outlets", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate800) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Slate100)
                        )
                        SuggestionChip(
                            onClick = {
                                viewModel.sendAiChatMessage(if (lang == "EN") "What is my estimated net profit from Salable Repack stock and current inventory velocity?" else "Berapa estimasi keuntungan bersih saya dari stok Repack Retur Layak Jual dan perputaran barang saat ini?")
                            },
                            icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp)) },
                            label = { Text(com.example.util.AppStrings.tr("Hitung Laba Repack", "Calculate Repack Profit", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate800) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Slate100)
                        )
                        SuggestionChip(
                            onClick = {
                                viewModel.sendAiChatMessage(if (lang == "EN") "Based on remaining 4-drawer car stock and route sales trends, how many cartons do you recommend loading tomorrow morning?" else "Berdasarkan sisa stok 4 laci di mobil dan tren penjualan rute, berapa dus saran muat barang pagi besok?")
                            },
                            icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Slate700, modifier = Modifier.size(14.dp)) },
                            label = { Text(com.example.util.AppStrings.tr("Saran Muat Pagi Besok", "Tomorrow Morning Load Advice", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate800) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Slate100)
                        )
                    }

                    HorizontalDivider(color = Slate200, thickness = 1.dp)

                    // Chat Messages LazyColumn
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages) { msg ->
                            val isUser = msg.role == "user"
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))

                            if (isUser) {
                                // User Message Bubble (Emerald Right-aligned)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = 14.dp,
                                            bottomEnd = 2.dp
                                        ),
                                        color = EmeraldPrimary,
                                        shadowElevation = 1.dp,
                                        modifier = Modifier.widthIn(max = 310.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                            MarkdownView(
                                                markdown = msg.content,
                                                isDarkBubble = true,
                                                baseTextColor = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = timeStr,
                                                color = Color.White.copy(alpha = 0.75f),
                                                fontSize = 9.5.sp,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Assistant Message Card (High-Contrast White Card with Markdown & Action Toolbar)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = 2.dp,
                                            bottomEnd = 14.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White,
                                            contentColor = Slate900
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth(0.96f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            // Assistant Header in Bubble
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    AiRobotAvatar(
                                                        size = 22.dp,
                                                        containerBackground = Slate900,
                                                        accentColor = EmeraldPrimary
                                                    )
                                                    Text(
                                                        text = "TracerPro Copilot",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp,
                                                        color = Slate800
                                                    )
                                                }
                                                Text(
                                                    text = timeStr,
                                                    fontSize = 9.5.sp,
                                                    color = Slate400
                                                )
                                            }

                                            HorizontalDivider(color = Slate100, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                                            // Render Rich Markdown
                                            MarkdownView(
                                                markdown = msg.content,
                                                isDarkBubble = false,
                                                baseTextColor = Slate900
                                            )

                                            // Action Buttons Toolbar for Copilot Responses
                                            if (msg.content.length > 20) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = Slate100, thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Copy Button
                                                    FilledTonalButton(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("TracerPro Copilot", msg.content))
                                                            Toast.makeText(context, com.example.util.AppStrings.tr("Teks / Markdown berhasil disalin!", "Text / Markdown copied to clipboard!", lang), Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = Slate100,
                                                            contentColor = Slate700
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(com.example.util.AppStrings.tr("Salin", "Copy", lang), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // Share to WhatsApp Button
                                                    Button(
                                                        onClick = {
                                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(Intent.EXTRA_TEXT, msg.content)
                                                                setPackage("com.whatsapp")
                                                            }
                                                            try {
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                val shareIntent = Intent.createChooser(
                                                                    Intent(Intent.ACTION_SEND).apply {
                                                                        type = "text/plain"
                                                                        putExtra(Intent.EXTRA_TEXT, msg.content)
                                                                    },
                                                                    com.example.util.AppStrings.tr("Kirim Laporan via WhatsApp", "Send Report via WhatsApp", lang)
                                                                )
                                                                context.startActivity(shareIntent)
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(com.example.util.AppStrings.tr("Kirim WA", "Share WA", lang), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Loading Indicator
                        if (isAiLoading) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = com.example.util.AppStrings.tr("Copilot sedang menganalisis database operasional...", "Copilot is analyzing operational database...", lang),
                                        fontSize = 11.5.sp,
                                        color = Slate600,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Input Bar (Clean Light Floating Form)
                    HorizontalDivider(color = Slate200, thickness = 1.dp)
                    Surface(
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text(com.example.util.AppStrings.tr("Tanyakan apa saja seputar operasional SFA...", "Ask anything about SFA operations...", lang), fontSize = 12.5.sp, color = Slate400) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_input_field"),
                                maxLines = 4,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Slate50,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Slate300,
                                    focusedTextColor = Slate900,
                                    unfocusedTextColor = Slate900
                                )
                            )

                            FilledIconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        val text = inputText
                                        inputText = ""
                                        viewModel.sendAiChatMessage(text)
                                    }
                                },
                                enabled = inputText.isNotBlank() && !isAiLoading,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = EmeraldPrimary,
                                    disabledContainerColor = Slate300
                                ),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = com.example.util.AppStrings.tr("Kirim", "Send", lang),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiConfigDialog(
    viewModel: SfaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by viewModel.aiConfig.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val lang = com.example.util.LocalAppLanguage.current

    var endpoint by remember { mutableStateOf(currentConfig.endpoint) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var model by remember { mutableStateOf(currentConfig.model) }
    var customPersona by remember { mutableStateOf(currentConfig.customPersona) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf<Boolean?>(null) }

    // Enforce Light Theme for Settings
    MaterialTheme(colorScheme = HighContrastEnterpriseColorScheme) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Surface(
                        color = Slate900,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = com.example.util.AppStrings.tr("Pengaturan AI Gateway & Persona", "AI Gateway & Persona Settings", lang),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "OpenAI-Compatible Custom Gateway Engine",
                                        color = Slate400,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = com.example.util.AppStrings.tr("Tutup", "Close", lang), tint = Color.White)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // System Persona & Domain Architecture Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = com.example.util.AppStrings.tr("Domain System Persona Aktif (Built-in Hardcoded)", "Active System Domain Persona (Built-in)", lang),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = com.example.util.AppStrings.tr("AI secara otomatis menguasai sistem 4 Laci Stok Mobil, kalkulasi piutang & aging warung, setoran kasir supplier, dan hukum laba repack murni. Persona kustom Anda di bawah melengkapi gaya komunikasi tanpa merusak logika bisnis.", "AI automatically understands the 4 Car Stock Drawers system, store credit & aging calculations, supplier settlements, and pure repack profit laws. Your custom persona complements the communication style without breaking business logic.", lang),
                                        fontSize = 11.sp,
                                        color = Color(0xFF15803D),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        // 1. Endpoint / Base URL
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = com.example.util.AppStrings.tr("Custom Gateway Base URL / Endpoint", "Custom Gateway Base URL / Endpoint", lang),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate800
                            )
                            OutlinedTextField(
                                value = endpoint,
                                onValueChange = { endpoint = it },
                                placeholder = { Text(com.example.util.AppStrings.tr("https://api.openai.com/v1 atau gateway custom Anda", "https://api.openai.com/v1 or your custom gateway", lang), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Slate300
                                )
                            )
                            // Preset chips for URL
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { endpoint = "https://ai.drakor.pp.ua/v1" },
                                    label = { Text("Default AI Proxy", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                )
                                SuggestionChip(
                                    onClick = { endpoint = "https://api.openai.com/v1" },
                                    label = { Text("OpenAI", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = { endpoint = "https://api.deepseek.com/v1" },
                                    label = { Text("DeepSeek", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = { endpoint = "https://openrouter.ai/api/v1" },
                                    label = { Text("OpenRouter", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = { endpoint = "https://api.groq.com/openai/v1" },
                                    label = { Text("Groq", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = { endpoint = "http://10.0.2.2:11434/v1" },
                                    label = { Text("Local Ollama", fontSize = 10.sp) }
                                )
                            }
                        }

                        // 2. API Key / Token
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "API Key / Bearer Token",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate800
                            )
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                placeholder = { Text("sk-...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = if (isApiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                        Icon(
                                            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility",
                                            tint = Slate500
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Slate300
                                )
                            )
                            Text(
                                text = com.example.util.AppStrings.tr("Bisa dikosongkan jika gateway lokal / reverse proxy Anda tidak memerlukan token.", "Can be left blank if your local gateway or reverse proxy doesn't require authentication token.", lang),
                                fontSize = 10.sp,
                                color = Slate500
                            )
                        }

                        // 3. Model Identifier
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = com.example.util.AppStrings.tr("Model Identifier", "Model Identifier", lang),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate800
                            )
                            OutlinedTextField(
                                value = model,
                                onValueChange = { model = it },
                                placeholder = { Text("gpt-4o-mini, deepseek-chat, llama-3.3-70b", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Slate300
                                )
                            )
                            // Model Presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(onClick = { model = "auto" }, label = { Text("auto (Default)", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                                SuggestionChip(onClick = { model = "gpt-4o-mini" }, label = { Text("gpt-4o-mini", fontSize = 10.sp) })
                                SuggestionChip(onClick = { model = "gpt-4o" }, label = { Text("gpt-4o", fontSize = 10.sp) })
                                SuggestionChip(onClick = { model = "deepseek-chat" }, label = { Text("deepseek-chat", fontSize = 10.sp) })
                                SuggestionChip(onClick = { model = "llama-3.3-70b-versatile" }, label = { Text("llama-3.3-70b", fontSize = 10.sp) })
                                SuggestionChip(onClick = { model = "claude-3-5-haiku-20241022" }, label = { Text("claude-3-5-haiku", fontSize = 10.sp) })
                                SuggestionChip(onClick = { model = "qwen-2.5-72b-instruct" }, label = { Text("qwen-2.5", fontSize = 10.sp) })
                            }
                        }

                        // 4. Custom User Persona & Instructions
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = com.example.util.AppStrings.tr("Custom Persona & Petunjuk Khusus Anda", "Custom Persona & Specific Instructions", lang),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate800
                                )
                                Text(
                                    text = com.example.util.AppStrings.tr("Opsional", "Optional", lang),
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                            OutlinedTextField(
                                value = customPersona,
                                onValueChange = { customPersona = it },
                                placeholder = {
                                    Text(
                                        com.example.util.AppStrings.tr(
                                            "Contoh:\n- Selalu gunakan gaya bahasa santai dan akrab khas salesman lapangan.\n- Prioritaskan selalu penagihan kas sebelum menyarankan drop barang baru.\n- Jika diminta draft WhatsApp, sertakan nomor kontak saya.",
                                            "Example:\n- Always speak in a friendly, practical tone suited for field sales.\n- Prioritize cash debt collection before suggesting new drop items.\n- If generating a WhatsApp draft, include my contact info.",
                                            lang
                                        ),
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Slate300
                                )
                            )
                            Text(
                                text = com.example.util.AppStrings.tr("Instruksi ini akan disuntikkan bersama data transaksi realtime setiap kali AI merespons.", "These instructions will be injected along with real-time transaction data whenever AI responds.", lang),
                                fontSize = 10.sp,
                                color = Slate500
                            )
                        }

                        // Test Connection Status Box
                        if (testStatusMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isTestSuccess == true) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isTestSuccess == true) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isTestSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isTestSuccess == true) EmeraldPrimary else RoseDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = testStatusMessage ?: "",
                                        fontSize = 11.sp,
                                        color = if (isTestSuccess == true) Color(0xFF166534) else Color(0xFF991B1B)
                                    )
                                }
                            }
                        }

                        // Test Connection Button
                        OutlinedButton(
                            onClick = {
                                val tempConfig = AiConfig(
                                    endpoint = endpoint,
                                    apiKey = apiKey,
                                    model = model,
                                    customPersona = customPersona
                                )
                                testStatusMessage = com.example.util.AppStrings.tr("Menguji komunikasi ke endpoint...", "Testing communication to endpoint...", lang)
                                isTestSuccess = null
                                viewModel.testAiConnection(tempConfig) { success, msg ->
                                    isTestSuccess = success
                                    testStatusMessage = msg
                                }
                            },
                            enabled = !isAiLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate800)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(com.example.util.AppStrings.tr("Test Koneksi ke Gateway", "Test Gateway Connection", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Action Footer
                    HorizontalDivider(color = Slate200)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(com.example.util.AppStrings.tr("Batal", "Cancel", lang), color = Slate700)
                        }

                        Button(
                            onClick = {
                                val newCfg = currentConfig.copy(
                                    endpoint = endpoint,
                                    apiKey = apiKey,
                                    model = model,
                                    customPersona = customPersona
                                )
                                viewModel.updateAiConfig(newCfg)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(com.example.util.AppStrings.tr("Simpan", "Save", lang), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiOutletRecommendationDialog(
    warung: WarungEntity,
    viewModel: SfaViewModel,
    onDismiss: () -> Unit,
    onTitipBaruClicked: () -> Unit
) {
    val context = LocalContext.current
    val lang = com.example.util.LocalAppLanguage.current
    var recommendationText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(warung.id) {
        viewModel.getAiOutletRecommendation(warung) { result ->
            recommendationText = result
            isLoading = false
        }
    }

    MaterialTheme(colorScheme = HighContrastEnterpriseColorScheme) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.80f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Surface(
                        color = Slate900,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = com.example.util.AppStrings.tr("AI Rekomendasi Restock", "AI Smart Restock & Cross-Sell", lang),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = warung.namaWarung,
                                        color = Slate400,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = com.example.util.AppStrings.tr("Tutup", "Close", lang), tint = Color.White)
                            }
                        }
                    }

                    // Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Warung Snapshot Card
                        Surface(
                            color = Slate50,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(com.example.util.AppStrings.tr("Kategori Toko", "Store Category", lang), fontSize = 10.sp, color = Slate500)
                                    Text(warung.kategoriWarung, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                }
                                Column {
                                    Text(com.example.util.AppStrings.tr("Stok Titipan Aktif", "Active Consignment", lang), fontSize = 10.sp, color = Slate500)
                                    Text("${warung.stokTitipanPcs} Pcs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                }
                                Column {
                                    Text(com.example.util.AppStrings.tr("Saldo Bon Piutang", "Outstanding Debt", lang), fontSize = 10.sp, color = Slate500)
                                    Text(SfaViewModel.formatRupiah(warung.saldoPiutang), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberWarning)
                                }
                            }
                        }

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 3.dp)
                                    Text(
                                        text = com.example.util.AppStrings.tr("Menganalisis data transaksi & rekomendasi restock...", "Analyzing transactions & restock recommendation...", lang),
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                }
                            }
                        } else {
                            // Render Markdown Advice
                            MarkdownView(
                                markdown = recommendationText ?: com.example.util.AppStrings.tr("Tidak ada rekomendasi tersedia.", "No recommendations available.", lang),
                                isDarkBubble = false,
                                baseTextColor = Slate900
                            )
                        }
                    }

                    // Action Footer
                    HorizontalDivider(color = Slate200)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(com.example.util.AppStrings.tr("Tutup", "Close", lang), color = Slate700)
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onTitipBaruClicked()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(com.example.util.AppStrings.tr("Buka Drop Titip", "Open Consignment Drop", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
