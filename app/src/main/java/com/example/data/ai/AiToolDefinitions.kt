package com.example.data.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Representasi pemanggilan fungsi/alat (Tool Call) oleh AI Agent.
 */
data class AiToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

/**
 * Hasil eksekusi sebuah tool oleh aplikasi Android.
 */
data class AiToolExecutionResult(
    val toolCallId: String,
    val toolName: String,
    val isSuccess: Boolean,
    val summary: String,
    val detailDataJson: String = "{}"
)

/**
 * Metadata definisi tool / fungsi untuk dikirimkan ke OpenAI / LLM API dalam format tools JSON Schema.
 */
object AiToolDefinitions {

    fun getToolsJsonArray(): JSONArray {
        val toolsArray = JSONArray()

        // 1. add_warung
        toolsArray.put(
            createToolJson(
                name = "add_warung",
                description = "Menambahkan data toko/warung kelontong baru ke sistem dan rute sales.",
                parametersObj = JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("nama_warung", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama toko atau warung, misalnya: Warung Bu Siti, Toko Berkah")
                        })
                        put("nama_pemilik", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama pemilik warung jika ada")
                        })
                        put("no_hp", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nomor HP atau WhatsApp warung")
                        })
                        put("alamat", JSONObject().apply {
                            put("type", "string")
                            put("description", "Alamat lengkap atau patokan lokasi warung")
                        })
                        put("kategori", JSONObject().apply {
                            put("type", "string")
                            put("enum", JSONArray(listOf("WARUNG_KELONTONG", "TOKO_SEMBAKO", "KANTIN_SEKOLAH", "AGEN_GROSIR", "MINIMARKET")))
                            put("description", "Kategori warung (default: WARUNG_KELONTONG)")
                        })
                        put("rute_name", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama rute yang ingin dimasukkan (misal: Rute Senin, Rute Utara). Jika kosong akan dimasukkan ke rute pertama/default")
                        })
                        put("limit_hutang", JSONObject().apply {
                            put("type", "number")
                            put("description", "Batas maksimal bon/piutang yang diizinkan dalam Rupiah (default: 500000)")
                        })
                    })
                    put("required", JSONArray(listOf("nama_warung")))
                }
            )
        )

        // 2. update_warung
        toolsArray.put(
            createToolJson(
                name = "update_warung",
                description = "Mengubah data warung yang sudah ada (nama pemilik, nomor HP, alamat, kategori, limit hutang, atau rute).",
                parametersObj = JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("warung_query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama warung yang ingin diubah (atau kata kuncinya)")
                        })
                        put("nama_warung_baru", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama warung baru jika ingin diubah")
                        })
                        put("nama_pemilik", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama pemilik baru")
                        })
                        put("no_hp", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nomor WhatsApp baru")
                        })
                        put("alamat", JSONObject().apply {
                            put("type", "string")
                            put("description", "Alamat baru")
                        })
                        put("limit_hutang", JSONObject().apply {
                            put("type", "number")
                            put("description", "Limit bon hutang baru dalam Rupiah")
                        })
                    })
                    put("required", JSONArray(listOf("warung_query")))
                }
            )
        )

        // 3. record_transaction (Pencatatan Transaksi / Penjualan Konsinyasi)
        toolsArray.put(
            createToolJson(
                name = "record_transaction",
                description = "Mencatat transaksi penjualan/konsinyasi di warung: hitung barang laku, bayar tunai/tempo, dan restock.",
                parametersObj = JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("warung_query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama warung yang ditransaksikan")
                        })
                        put("product_query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama produk yang laku/dititip")
                        })
                        put("pcs_laku", JSONObject().apply {
                            put("type", "integer")
                            put("description", "Jumlah pcs produk yang laku terjual")
                        })
                        put("uang_diterima", JSONObject().apply {
                            put("type", "number")
                            put("description", "Jumlah uang tunai yang dibayarkan pemilik warung saat ini (Rp)")
                        })
                        put("restock_pcs", JSONObject().apply {
                            put("type", "integer")
                            put("description", "Jumlah pcs produk baru yang dititipkan kembali ke warung (opsional, default: sama dengan pcs_laku)")
                        })
                        put("sumber_restock", JSONObject().apply {
                            put("type", "string")
                            put("enum", JSONArray(listOf("FRESH_PABRIK", "PRIBADI_REPACK")))
                            put("description", "Laci sumber barang restock: FRESH_PABRIK (default) atau PRIBADI_REPACK")
                        })
                        put("catatan", JSONObject().apply {
                            put("type", "string")
                            put("description", "Catatan tambahan transaksi jika ada")
                        })
                    })
                    put("required", JSONArray(listOf("warung_query", "product_query")))
                }
            )
        )

        // 4. pay_outlet_debt (Pelunasan Piutang / Bon Warung)
        toolsArray.put(
            createToolJson(
                name = "pay_outlet_debt",
                description = "Mencatat pembayaran atau pelunasan bon piutang dari warung tanpa penjualan barang baru.",
                parametersObj = JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("warung_query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama warung yang membayar bon")
                        })
                        put("nominal_bayar", JSONObject().apply {
                            put("type", "number")
                            put("description", "Nominal rupiah uang yang disetor untuk melunasi/mencicil hutang")
                        })
                        put("catatan", JSONObject().apply {
                            put("type", "string")
                            put("description", "Catatan pelunasan, misalnya 'Titip ke kasir/anaknya'")
                        })
                    })
                    put("required", JSONArray(listOf("warung_query", "nominal_bayar")))
                }
            )
        )

        // 5. set_custom_price (Set Harga Khusus Per Warung)
        toolsArray.put(
            createToolJson(
                name = "set_custom_price",
                description = "Menetapkan atau menghapus harga jual khusus per pcs untuk produk tertentu di warung tertentu.",
                parametersObj = JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("warung_query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama warung")
                        })
                        put("product_query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama produk")
                        })
                        put("harga_jual_pcs", JSONObject().apply {
                            put("type", "number")
                            put("description", "Harga khusus baru per pcs dalam rupiah. Jika 0 atau negatif maka harga khusus dihapus (kembali ke default).")
                        })
                    })
                    put("required", JSONArray(listOf("warung_query", "product_query", "harga_jual_pcs")))
                }
            )
        )

        // 6. add_product (Tambah Master Produk)
        toolsArray.put(
            createToolJson(
                name = "add_product",
                description = "Menambahkan master produk baru ke katalog barang distribusi FMCG.",
                parametersObj = JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("nama_produk", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama barang / produk, contoh: 'Kopi Kapal Api 25g'")
                        })
                        put("kategori", JSONObject().apply {
                            put("type", "string")
                            put("description", "Kategori produk, misal: Minuman, Makanan Ringan, Bumbu Dapur")
                        })
                        put("satuan_kemasan", JSONObject().apply {
                            put("type", "string")
                            put("description", "Satuan kemasan besar, misal: Dus, Bal, Slop, Karton")
                        })
                        put("rasio_konversi", JSONObject().apply {
                            put("type", "integer")
                            put("description", "Berapa pcs isi per satuan dus/kemasan besar (default: 24 atau 120)")
                        })
                        put("harga_beli_dus", JSONObject().apply {
                            put("type", "number")
                            put("description", "Harga modal beli per dus dari pabrik/supplier")
                        })
                        put("harga_jual_pcs", JSONObject().apply {
                            put("type", "number")
                            put("description", "Harga jual standar konsinyasi per pcs ke warung")
                        })
                        put("nama_pabrik", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama pabrik atau supplier jika diketahui")
                        })
                    })
                    put("required", JSONArray(listOf("nama_produk", "harga_jual_pcs")))
                }
            )
        )

        // 7. add_rute (Tambah Master Rute)
        toolsArray.put(
            createToolJson(
                name = "add_rute",
                description = "Membuat rute distribusi kunjungan baru (contoh: Rute Barat, Rute Jalur Pantura).",
                parametersObj = JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("nama_rute", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama rute kunjungan")
                        })
                        put("hari_kunjungan", JSONObject().apply {
                            put("type", "string")
                            put("description", "Hari kunjungan terjadwal (misal: Senin, Selasa, Setiap Hari)")
                        })
                        put("keterangan", JSONObject().apply {
                            put("type", "string")
                            put("description", "Deskripsi atau area rute")
                        })
                    })
                    put("required", JSONArray(listOf("nama_rute")))
                }
            )
        )

        // 8. mark_outlet_visited (Tandai Kunjungan Selesai / Skip)
        toolsArray.put(
            createToolJson(
                name = "mark_outlet_visited",
                description = "Memperbarui stempel waktu kunjungan terakhir warung menjadi hari ini.",
                parametersObj = JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("warung_query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Nama warung yang ingin ditandai sudah dikunjungi")
                        })
                    })
                    put("required", JSONArray(listOf("warung_query")))
                }
            )
        )

        return toolsArray
    }

    private fun createToolJson(name: String, description: String, parametersObj: JSONObject): JSONObject {
        return JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", name)
                put("description", description)
                put("parameters", parametersObj)
            })
        }
    }
}
