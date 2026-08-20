package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 DPI)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 DPI)
    private const val MARGIN = 36f // 0.5 inch margins

    suspend fun generateDailyDistributionReport(
        context: Context,
        profile: UserProfileEntity?,
        periodLabel: String,
        transactions: List<TransactionEntity>,
        loadings: List<DailyLoadingEntity>,
        drawers: List<InventoryDrawerEntity>,
        products: List<ProductEntity>,
        warungs: List<WarungEntity>,
        pabriks: List<PabrikEntity>
    ): File = withContext(Dispatchers.IO) {
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val dateCompact = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Laporan_TracerPro_${dateCompact}.pdf"
        val pdfFile = File(reportsDir, fileName)

        val pdfDocument = PdfDocument()

        val productMap = products.associateBy { it.id }
        val warungMap = warungs.associateBy { it.id }
        val pabrikMap = pabriks.associateBy { it.id }

        // Metrics calculations
        val totalOmset = transactions.sumOf { it.subtotalLaku }
        val totalKas = transactions.sumOf { it.uangDiterima }
        val totalPiutangBaru = transactions.sumOf { it.saldoPiutangBaru }
        val totalVolumePcs = transactions.sumOf { it.pcsLaku }
        val totalBsDitarik = transactions.sumOf { it.bsDitarikPcs }
        val totalRestock = transactions.sumOf { it.restockBaruPcs }
        val totalSetoranPabrik = loadings.sumOf { if (it.statusClosing) it.tagihanPabrikClosing else it.potensiHutangPabrik }

        // Paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate900
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate500
            textSize = 8f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(16, 185, 129) // Emerald
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.rgb(226, 232, 240) // Slate200
            strokeWidth = 0.8f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var y = MARGIN

        // --- HEADER SECTION ---
        // Brand & Title
        canvas.drawText("TracerPro", MARGIN, y + 14f, brandPaint)
        canvas.drawText("LAPORAN DISTRIBUSI & SETORAN KONSINYASI FMCG", MARGIN + 85f, y + 13f, titlePaint)
        y += 24f

        val exportTimeStr = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale.getDefault()).format(Date())
        canvas.drawText("Periode Laporan: $periodLabel  |  Dicetak pada: $exportTimeStr", MARGIN, y, subtextPaint)
        y += 12f

        // Header info box
        fillPaint.color = Color.rgb(248, 250, 252) // Slate50
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 38f), 6f, 6f, fillPaint)
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 38f), 6f, 6f, strokePaint)

        val salesName = profile?.namaSalesman?.ifBlank { "Salesman Lapangan" } ?: "Salesman Lapangan"
        val distName = profile?.namaDistributor?.ifBlank { "Distributor Resmi" } ?: "Distributor Resmi"
        val phoneNo = profile?.noHp?.ifBlank { "-" } ?: "-"
        val vehicle = profile?.platNomorMobil?.ifBlank { "-" } ?: "-"

        boldPaint.textSize = 8.5f
        textPaint.textSize = 8.5f
        canvas.drawText("Salesman:", MARGIN + 8f, y + 14f, boldPaint)
        canvas.drawText(salesName, MARGIN + 58f, y + 14f, textPaint)
        canvas.drawText("Distributor / Depo:", MARGIN + 180f, y + 14f, boldPaint)
        canvas.drawText(distName, MARGIN + 265f, y + 14f, textPaint)

        canvas.drawText("No. HP / WA:", MARGIN + 8f, y + 28f, boldPaint)
        canvas.drawText(phoneNo, MARGIN + 58f, y + 28f, textPaint)
        canvas.drawText("No. Polisi Armada:", MARGIN + 180f, y + 28f, boldPaint)
        canvas.drawText(vehicle, MARGIN + 265f, y + 28f, textPaint)

        y += 48f

        // --- SUMMARY METRIC CARDS (2x2 Grid) ---
        val cardWidth = (PAGE_WIDTH - (MARGIN * 2) - 10f) / 2f
        val cardHeight = 36f

        // Card 1: Total Omset & Volume
        drawMetricCard(
            canvas, fillPaint, strokePaint, boldPaint, textPaint, subtextPaint,
            MARGIN, y, cardWidth, cardHeight,
            "TOTAL PENJUALAN KONSINYASI",
            formatRupiah(totalOmset),
            "Volume Terjual: ${totalVolumePcs} Pcs | Restock: +${totalRestock} Pcs",
            Color.rgb(16, 185, 129) // Emerald
        )

        // Card 2: Total Uang Masuk Kas
        drawMetricCard(
            canvas, fillPaint, strokePaint, boldPaint, textPaint, subtextPaint,
            MARGIN + cardWidth + 10f, y, cardWidth, cardHeight,
            "TOTAL UANG KAS TERKUMPUL",
            formatRupiah(totalKas),
            "Piutang Baru: ${formatRupiah(totalPiutangBaru)}",
            Color.rgb(37, 99, 235) // Blue
        )

        y += cardHeight + 8f

        // Card 3: Setoran Tagihan Pabrik
        drawMetricCard(
            canvas, fillPaint, strokePaint, boldPaint, textPaint, subtextPaint,
            MARGIN, y, cardWidth, cardHeight,
            "TAGIHAN PABRIK / PRINCIPAL",
            formatRupiah(totalSetoranPabrik),
            "Total Sesi Muat: ${loadings.size} Faktur",
            Color.rgb(217, 119, 6) // Amber
        )

        // Card 4: Retur Tarikan Ditarik
        drawMetricCard(
            canvas, fillPaint, strokePaint, boldPaint, textPaint, subtextPaint,
            MARGIN + cardWidth + 10f, y, cardWidth, cardHeight,
            "RETUR TARIKAN / FISIK KEMBALI",
            "$totalBsDitarik Pcs",
            "Transaksi Warung: ${transactions.size} Kunjungan",
            Color.rgb(225, 29, 72) // Rose
        )

        y += cardHeight + 16f

        // --- SECTION 1: RINCIAN TRANSAKSI OUTLET ---
        boldPaint.textSize = 10f
        canvas.drawText("1. RINCIAN TRANSAKSI & PENAGIHAN OUTLET", MARGIN, y, boldPaint)
        y += 8f

        // Table Header
        val colX = floatArrayOf(
            MARGIN,          // No (width 16)
            MARGIN + 18f,    // Outlet (width 110)
            MARGIN + 130f,   // Produk (width 80)
            MARGIN + 212f,   // Laku (width 40)
            MARGIN + 254f,   // Subtotal (width 60)
            MARGIN + 316f,   // Tagihan (width 62)
            MARGIN + 380f,   // Bayar Kas (width 60)
            MARGIN + 442f,   // Sisa Bon (width 50)
            MARGIN + 494f    // Status (width 29)
        )

        fillPaint.color = Color.rgb(15, 23, 42) // Slate900
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 16f, fillPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("No", colX[0] + 2f, y + 11f, headerTextPaint)
        canvas.drawText("Nama Outlet", colX[1], y + 11f, headerTextPaint)
        canvas.drawText("Produk", colX[2], y + 11f, headerTextPaint)
        canvas.drawText("Laku", colX[3], y + 11f, headerTextPaint)
        canvas.drawText("Subtotal", colX[4], y + 11f, headerTextPaint)
        canvas.drawText("Total Tagihan", colX[5], y + 11f, headerTextPaint)
        canvas.drawText("Bayar Kas", colX[6], y + 11f, headerTextPaint)
        canvas.drawText("Sisa Bon", colX[7], y + 11f, headerTextPaint)
        canvas.drawText("Status", colX[8], y + 11f, headerTextPaint)

        y += 16f

        textPaint.textSize = 7.5f
        boldPaint.textSize = 7.5f

        if (transactions.isEmpty()) {
            canvas.drawText("Tidak ada data transaksi pada periode yang dipilih.", MARGIN + 10f, y + 16f, subtextPaint)
            y += 24f
        } else {
            transactions.forEachIndexed { index, tx ->
                // Check if page overflow
                if (y > PAGE_HEIGHT - MARGIN - 70f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = MARGIN

                    // Repeat Header on new page
                    fillPaint.color = Color.rgb(15, 23, 42)
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 16f, fillPaint)
                    canvas.drawText("No", colX[0] + 2f, y + 11f, headerTextPaint)
                    canvas.drawText("Nama Outlet", colX[1], y + 11f, headerTextPaint)
                    canvas.drawText("Produk", colX[2], y + 11f, headerTextPaint)
                    canvas.drawText("Laku", colX[3], y + 11f, headerTextPaint)
                    canvas.drawText("Subtotal", colX[4], y + 11f, headerTextPaint)
                    canvas.drawText("Total Tagihan", colX[5], y + 11f, headerTextPaint)
                    canvas.drawText("Bayar Kas", colX[6], y + 11f, headerTextPaint)
                    canvas.drawText("Sisa Bon", colX[7], y + 11f, headerTextPaint)
                    canvas.drawText("Status", colX[8], y + 11f, headerTextPaint)
                    y += 16f
                }

                // Zebra striping
                if (index % 2 == 1) {
                    fillPaint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 14f, fillPaint)
                }

                val warungName = warungMap[tx.warungId]?.namaWarung ?: tx.warungId.take(12)
                val productName = productMap[tx.productId]?.nama ?: tx.productId.take(10)

                canvas.drawText("${index + 1}", colX[0] + 2f, y + 10f, textPaint)
                canvas.drawText(warungName.take(18), colX[1], y + 10f, boldPaint)
                canvas.drawText(productName.take(14), colX[2], y + 10f, textPaint)
                canvas.drawText("${tx.pcsLaku} Pcs", colX[3], y + 10f, textPaint)
                canvas.drawText(formatCompact(tx.subtotalLaku), colX[4], y + 10f, textPaint)
                canvas.drawText(formatCompact(tx.grandTotalTagihan), colX[5], y + 10f, textPaint)
                canvas.drawText(formatCompact(tx.uangDiterima), colX[6], y + 10f, boldPaint)
                canvas.drawText(formatCompact(tx.saldoPiutangBaru), colX[7], y + 10f, if (tx.saldoPiutangBaru > 0) boldPaint else textPaint)

                val statusColor = when (tx.statusBayar) {
                    "LUNAS" -> Color.rgb(5, 150, 105)
                    "SEBAGIAN" -> Color.rgb(217, 119, 6)
                    "TITIP_BARU" -> Color.rgb(37, 99, 235)
                    else -> Color.rgb(225, 29, 72)
                }
                boldPaint.color = statusColor
                canvas.drawText(tx.statusBayar.take(7), colX[8], y + 10f, boldPaint)
                boldPaint.color = Color.rgb(15, 23, 42) // reset

                canvas.drawLine(MARGIN, y + 14f, PAGE_WIDTH - MARGIN, y + 14f, strokePaint)
                y += 14f
            }
        }

        y += 12f

        // --- SECTION 2: AUDIT 4 LACI STOK MOBIL ---
        if (y > PAGE_HEIGHT - MARGIN - 140f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = MARGIN
        }

        boldPaint.textSize = 10f
        canvas.drawText("2. AUDIT POSISI 4 LACI STOK FISIK MOBIL", MARGIN, y, boldPaint)
        y += 8f

        val drawerColX = floatArrayOf(
            MARGIN,          // Produk (width 160)
            MARGIN + 165f,   // Laci 1: Fresh Mobil (width 90)
            MARGIN + 260f,   // Laci 2: Titipan Warung (width 90)
            MARGIN + 355f,   // Laci 3: Retur Belum Sortir (width 85)
            MARGIN + 445f    // Laci 4: Aset Repack Pribadi (width 78)
        )

        fillPaint.color = Color.rgb(51, 65, 85) // Slate700
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 16f, fillPaint)

        canvas.drawText("Nama Produk", drawerColX[0] + 4f, y + 11f, headerTextPaint)
        canvas.drawText("1. Fresh di Mobil", drawerColX[1], y + 11f, headerTextPaint)
        canvas.drawText("2. Titipan Warung", drawerColX[2], y + 11f, headerTextPaint)
        canvas.drawText("3. Retur Tarikan", drawerColX[3], y + 11f, headerTextPaint)
        canvas.drawText("4. Aset Mandiri", drawerColX[4], y + 11f, headerTextPaint)

        y += 16f

        products.forEachIndexed { idx, prod ->
            val d = drawers.find { it.productId == prod.id }
            val totalTitipDiWarung = warungs.sumOf { it.stokTitipanPcs }

            if (idx % 2 == 1) {
                fillPaint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 14f, fillPaint)
            }

            canvas.drawText(prod.nama.take(24), drawerColX[0] + 4f, y + 10f, boldPaint)
            canvas.drawText("${d?.stokFreshPabrikPcs ?: 0} Pcs", drawerColX[1], y + 10f, textPaint)
            canvas.drawText("${totalTitipDiWarung} Pcs", drawerColX[2], y + 10f, textPaint)
            canvas.drawText("${d?.stokBsBelumSortirPcs ?: 0} Pcs", drawerColX[3], y + 10f, textPaint)
            canvas.drawText("${d?.stokPribadiLayakJualPcs ?: 0} Pcs", drawerColX[4], y + 10f, boldPaint)

            canvas.drawLine(MARGIN, y + 14f, PAGE_WIDTH - MARGIN, y + 14f, strokePaint)
            y += 14f
        }

        y += 20f

        // --- SIGNATURE SECTION ---
        if (y > PAGE_HEIGHT - MARGIN - 80f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = MARGIN + 10f
        }

        val sigWidth = (PAGE_WIDTH - (MARGIN * 2)) / 3f
        subtextPaint.textSize = 8f
        boldPaint.textSize = 8.5f

        // Sig 1: Salesman
        canvas.drawText("Dibuat Oleh,", MARGIN + 20f, y, subtextPaint)
        canvas.drawText("Salesman Lapangan", MARGIN + 20f, y + 10f, boldPaint)
        canvas.drawLine(MARGIN + 10f, y + 48f, MARGIN + sigWidth - 10f, y + 48f, strokePaint)
        canvas.drawText(salesName, MARGIN + 20f, y + 58f, boldPaint)

        // Sig 2: Supervisor / Distributor
        canvas.drawText("Diperiksa Oleh,", MARGIN + sigWidth + 20f, y, subtextPaint)
        canvas.drawText("Supervisor / Depo", MARGIN + sigWidth + 20f, y + 10f, boldPaint)
        canvas.drawLine(MARGIN + sigWidth + 10f, y + 48f, MARGIN + (sigWidth * 2) - 10f, y + 48f, strokePaint)
        canvas.drawText("( ................................... )", MARGIN + sigWidth + 20f, y + 58f, textPaint)

        // Sig 3: Kasir / Rekonsiliasi
        canvas.drawText("Diterima Kasir,", MARGIN + (sigWidth * 2) + 20f, y, subtextPaint)
        canvas.drawText("Kasir / Finance", MARGIN + (sigWidth * 2) + 20f, y + 10f, boldPaint)
        canvas.drawLine(MARGIN + (sigWidth * 2) + 10f, y + 48f, PAGE_WIDTH - MARGIN - 10f, y + 48f, strokePaint)
        canvas.drawText("( ................................... )", MARGIN + (sigWidth * 2) + 20f, y + 58f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF to file
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        pdfFile
    }

    private fun drawMetricCard(
        canvas: Canvas,
        fillPaint: Paint,
        strokePaint: Paint,
        boldPaint: Paint,
        textPaint: Paint,
        subtextPaint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        subvalue: String,
        accentColor: Int
    ) {
        fillPaint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 6f, 6f, fillPaint)
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 6f, 6f, strokePaint)

        // Left accent bar
        fillPaint.color = accentColor
        canvas.drawRoundRect(RectF(x, y, x + 3.5f, y + height), 2f, 2f, fillPaint)

        subtextPaint.textSize = 7.5f
        subtextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 8f, y + 11f, subtextPaint)
        subtextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        boldPaint.textSize = 10f
        boldPaint.color = Color.rgb(15, 23, 42)
        canvas.drawText(value, x + 8f, y + 23f, boldPaint)

        textPaint.textSize = 7f
        textPaint.color = Color.rgb(100, 116, 139)
        canvas.drawText(subvalue, x + 8f, y + 32f, textPaint)
    }

    private fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    private fun formatCompact(amount: Double): String {
        return if (amount >= 1_000_000) {
            String.format(Locale.US, "%.1fM", amount / 1_000_000.0)
        } else if (amount >= 1_000) {
            String.format(Locale.US, "%.0fk", amount / 1_000.0)
        } else {
            amount.toLong().toString()
        }
    }

    fun sharePdfReport(context: Context, pdfFile: File, title: String = "Bagikan Laporan TracerPro") {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Distribusi TracerPro - ${pdfFile.name}")
                putExtra(Intent.EXTRA_TEXT, "Berikut adalah lampiran berkas resmi PDF Rekap Distribusi & Setoran Harian dari aplikasi TracerPro.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdfReport(context: Context, pdfFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            // If no dedicated viewer, share
            sharePdfReport(context, pdfFile, "Buka / Bagikan Laporan PDF")
        }
    }
}
