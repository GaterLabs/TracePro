package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SfaDao {

    // --- PRODUCTS ---
    @Query("SELECT * FROM products ORDER BY nama ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    // --- WARUNGS ---
    @Query("SELECT * FROM warungs ORDER BY urutanKunjungan ASC, namaWarung ASC")
    fun getAllWarungs(): Flow<List<WarungEntity>>

    @Query("SELECT * FROM warungs WHERE ruteId = :ruteId AND status != 'Blacklist' ORDER BY urutanKunjungan ASC")
    fun getWarungsByRute(ruteId: String): Flow<List<WarungEntity>>

    @Query("SELECT * FROM warungs WHERE id = :id")
    suspend fun getWarungById(id: String): WarungEntity?

    @Query("SELECT * FROM warungs WHERE pendingAddressSync = 1")
    suspend fun getWarungsPendingAddressSync(): List<WarungEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarungs(warungs: List<WarungEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarung(warung: WarungEntity)

    @Update
    suspend fun updateWarung(warung: WarungEntity)

    @Delete
    suspend fun deleteWarung(warung: WarungEntity)

    // --- RUTES ---
    @Query("SELECT * FROM rutes ORDER BY namaRute ASC")
    fun getAllRutes(): Flow<List<RuteEntity>>

    @Query("SELECT * FROM rutes WHERE id = :id")
    suspend fun getRuteById(id: String): RuteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRutes(rutes: List<RuteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRute(rute: RuteEntity)

    @Update
    suspend fun updateRute(rute: RuteEntity)

    @Delete
    suspend fun deleteRute(rute: RuteEntity)

    // --- 4 VIRTUAL INVENTORY DRAWERS ---
    @Query("SELECT * FROM inventory_drawers")
    fun getAllInventoryDrawers(): Flow<List<InventoryDrawerEntity>>

    @Query("SELECT * FROM inventory_drawers WHERE productId = :productId LIMIT 1")
    suspend fun getDrawerByProductId(productId: String): InventoryDrawerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawer(drawer: InventoryDrawerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawers(drawers: List<InventoryDrawerEntity>)

    @Update
    suspend fun updateDrawer(drawer: InventoryDrawerEntity)

    // --- DAILY LOADINGS ---
    @Query("SELECT * FROM daily_loadings ORDER BY createdAt DESC")
    fun getAllDailyLoadings(): Flow<List<DailyLoadingEntity>>

    @Query("SELECT * FROM daily_loadings WHERE id = :id LIMIT 1")
    suspend fun getDailyLoadingById(id: String): DailyLoadingEntity?

    @Query("SELECT * FROM daily_loadings WHERE tanggal = :tanggal")
    fun getDailyLoadingsByDate(tanggal: String): Flow<List<DailyLoadingEntity>>

    @Query("SELECT * FROM daily_loadings WHERE tanggal = :tanggal")
    suspend fun getDailyLoadingsByDateSync(tanggal: String): List<DailyLoadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLoading(loading: DailyLoadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLoadings(loadings: List<DailyLoadingEntity>)

    @Update
    suspend fun updateDailyLoading(loading: DailyLoadingEntity)

    @Query("UPDATE daily_loadings SET jumlahBayarMuat = :jumlahBayar, sisaHutangMuat = :sisaHutang, statusLunasHutang = :statusLunas WHERE id = :id")
    suspend fun updateLoadingDebtPayment(id: String, jumlahBayar: Double, sisaHutang: Double, statusLunas: Boolean)

    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions WHERE warungId != 'CLOSING_SALES' AND jenis != 'CLOSING_HARIAN' ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE tanggal = :tanggal AND warungId != 'CLOSING_SALES' AND jenis != 'CLOSING_HARIAN' ORDER BY timestamp DESC")
    fun getTransactionsByDate(tanggal: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE warungId = :warungId AND jenis != 'CLOSING_HARIAN' ORDER BY timestamp DESC")
    fun getTransactionsByWarung(warungId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE warungId = :warungId AND jenis != 'CLOSING_HARIAN' ORDER BY timestamp DESC")
    suspend fun getTransactionsByWarungSync(warungId: String): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE warungId = 'CLOSING_SALES' OR jenis = 'CLOSING_HARIAN'")
    suspend fun deleteLegacyClosingTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    // --- BS SORTIRS ---
    @Query("SELECT * FROM bs_sortirs ORDER BY timestamp DESC")
    fun getAllBsSortirs(): Flow<List<BsSortirEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBsSortir(sortir: BsSortirEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBsSortirs(sortirs: List<BsSortirEntity>)

    // --- PABRIKS ---
    @Query("SELECT * FROM pabriks")
    fun getAllPabriks(): Flow<List<PabrikEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPabriks(pabriks: List<PabrikEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPabrik(pabrik: PabrikEntity)

    @Update
    suspend fun updatePabrik(pabrik: PabrikEntity)

    @Delete
    suspend fun deletePabrik(pabrik: PabrikEntity)

    // --- WRITE OFFS ---
    @Query("SELECT * FROM write_offs ORDER BY timestamp DESC")
    fun getAllWriteOffs(): Flow<List<WriteOffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWriteOff(writeOff: WriteOffEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWriteOffs(writeOffs: List<WriteOffEntity>)

    // --- RESET / CLEAN TRANSACTIONAL DATA FOR PRODUCTION ---
    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM daily_loadings")
    suspend fun clearAllDailyLoadings()

    @Query("DELETE FROM bs_sortirs")
    suspend fun clearAllBsSortirs()

    @Query("DELETE FROM write_offs")
    suspend fun clearAllWriteOffs()

    @Query("UPDATE warungs SET saldoPiutang = 0.0, stokTitipanPcs = 0, tglMulaiHutang = 0, tglKunjunganTerakhir = 0")
    suspend fun resetAllWarungBalances()

    @Query("UPDATE inventory_drawers SET stokFreshPabrikPcs = 0, stokBsBelumSortirPcs = 0, stokPribadiLayakJualPcs = 0, stokPribadiRusakPcs = 0")
    suspend fun resetAllDrawers()

    @Query("DELETE FROM warungs")
    suspend fun clearAllWarungs()

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Query("DELETE FROM rutes")
    suspend fun clearAllRutes()

    @Query("DELETE FROM pabriks")
    suspend fun clearAllPabriks()

    @Query("DELETE FROM inventory_drawers")
    suspend fun clearAllDrawersCompletely()

    @Query("DELETE FROM inventory_drawers WHERE productId = :productId")
    suspend fun deleteDrawerByProductId(productId: String)

    // --- USER PROFILE & FIRST-OPEN SETUP ---
    @Query("SELECT * FROM user_profile WHERE id = 'PRIMARY_PROFILE' LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'PRIMARY_PROFILE' LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    // --- WARUNG CUSTOM PRICING ---
    @Query("SELECT * FROM warung_custom_prices")
    fun getAllCustomPrices(): Flow<List<WarungCustomPriceEntity>>

    @Query("SELECT * FROM warung_custom_prices WHERE warungId = :warungId")
    fun getCustomPricesByWarung(warungId: String): Flow<List<WarungCustomPriceEntity>>

    @Query("SELECT * FROM warung_custom_prices WHERE warungId = :warungId AND productId = :productId LIMIT 1")
    suspend fun getCustomPrice(warungId: String, productId: String): WarungCustomPriceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPrice(price: WarungCustomPriceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPrices(prices: List<WarungCustomPriceEntity>)

    @Query("DELETE FROM warung_custom_prices WHERE warungId = :warungId AND productId = :productId")
    suspend fun deleteCustomPrice(warungId: String, productId: String)

    @Query("DELETE FROM warung_custom_prices")
    suspend fun clearAllCustomPrices()

    // --- FULL DIRECT DATA GETTERS FOR JSON BACKUP / EXPORT ---
    @Query("SELECT * FROM products")
    suspend fun getAllProductsDirect(): List<ProductEntity>

    @Query("SELECT * FROM warungs")
    suspend fun getAllWarungsDirect(): List<WarungEntity>

    @Query("SELECT * FROM rutes")
    suspend fun getAllRutesDirect(): List<RuteEntity>

    @Query("SELECT * FROM pabriks")
    suspend fun getAllPabriksDirect(): List<PabrikEntity>

    @Query("SELECT * FROM inventory_drawers")
    suspend fun getAllDrawersDirect(): List<InventoryDrawerEntity>

    @Query("SELECT * FROM daily_loadings")
    suspend fun getAllDailyLoadingsDirect(): List<DailyLoadingEntity>

    @Query("SELECT * FROM transactions WHERE warungId != 'CLOSING_SALES' AND jenis != 'CLOSING_HARIAN'")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>

    @Query("SELECT * FROM bs_sortirs")
    suspend fun getAllBsSortirsDirect(): List<BsSortirEntity>

    @Query("SELECT * FROM write_offs")
    suspend fun getAllWriteOffsDirect(): List<WriteOffEntity>

    @Query("SELECT * FROM warung_custom_prices")
    suspend fun getAllCustomPricesDirect(): List<WarungCustomPriceEntity>

    // --- WEEKLY SHIPMENTS (JATAH MINGGUAN / POOL RUMAH) ---
    @Query("SELECT * FROM weekly_shipments ORDER BY createdAt DESC")
    fun getAllWeeklyShipments(): Flow<List<WeeklyShipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyShipment(shipment: WeeklyShipmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyShipments(shipments: List<WeeklyShipmentEntity>)

    @Query("SELECT * FROM weekly_shipments")
    suspend fun getAllWeeklyShipmentsDirect(): List<WeeklyShipmentEntity>

    // --- PERSONAL FINANCE (KEUANGAN PRIBADI: AKUN, PENGELUARAN, HUTANG/PIUTANG) ---
    @Query("SELECT * FROM personal_accounts ORDER BY isPaylater ASC, namaAkun ASC")
    fun getAllPersonalAccounts(): Flow<List<PersonalAccountEntity>>

    @Query("SELECT * FROM personal_accounts WHERE id = :id")
    suspend fun getPersonalAccountById(id: String): PersonalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalAccount(account: PersonalAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalAccounts(accounts: List<PersonalAccountEntity>)

    @Update
    suspend fun updatePersonalAccount(account: PersonalAccountEntity)

    @Delete
    suspend fun deletePersonalAccount(account: PersonalAccountEntity)

    @Query("DELETE FROM personal_accounts WHERE id = :id")
    suspend fun deletePersonalAccountById(id: String)

    @Query("SELECT * FROM personal_accounts")
    suspend fun getAllPersonalAccountsDirect(): List<PersonalAccountEntity>

    // Personal Expenses / Incomes
    @Query("SELECT * FROM personal_expenses ORDER BY tanggal DESC, timestamp DESC")
    fun getAllPersonalExpenses(): Flow<List<PersonalExpenseEntity>>

    @Query("SELECT * FROM personal_expenses WHERE tanggal = :tanggal ORDER BY timestamp DESC")
    fun getPersonalExpensesByDate(tanggal: String): Flow<List<PersonalExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalExpense(expense: PersonalExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalExpenses(expenses: List<PersonalExpenseEntity>)

    @Update
    suspend fun updatePersonalExpense(expense: PersonalExpenseEntity)

    @Delete
    suspend fun deletePersonalExpense(expense: PersonalExpenseEntity)

    @Query("DELETE FROM personal_expenses WHERE id = :id")
    suspend fun deletePersonalExpenseById(id: String)

    @Query("SELECT * FROM personal_expenses")
    suspend fun getAllPersonalExpensesDirect(): List<PersonalExpenseEntity>

    // Personal Debts & Receivables
    @Query("SELECT * FROM personal_debts ORDER BY status ASC, updatedAt DESC")
    fun getAllPersonalDebts(): Flow<List<PersonalDebtEntity>>

    @Query("SELECT * FROM personal_debts WHERE id = :id")
    suspend fun getPersonalDebtById(id: String): PersonalDebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalDebt(debt: PersonalDebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalDebts(debts: List<PersonalDebtEntity>)

    @Update
    suspend fun updatePersonalDebt(debt: PersonalDebtEntity)

    @Delete
    suspend fun deletePersonalDebt(debt: PersonalDebtEntity)

    @Query("DELETE FROM personal_debts WHERE id = :id")
    suspend fun deletePersonalDebtById(id: String)

    @Query("SELECT * FROM personal_debts")
    suspend fun getAllPersonalDebtsDirect(): List<PersonalDebtEntity>

    @Query("DELETE FROM personal_accounts")
    suspend fun clearAllPersonalAccounts()

    @Query("DELETE FROM personal_expenses")
    suspend fun clearAllPersonalExpenses()

    @Query("DELETE FROM personal_debts")
    suspend fun clearAllPersonalDebts()

    @Query("DELETE FROM personal_expenses WHERE accountId IN ('ACC_CASH_DOMPET', 'ACC_BANK_BCA', 'ACC_EWALLET_GOPAY', 'ACC_PAYLATER_SPAY') OR toAccountId IN ('ACC_CASH_DOMPET', 'ACC_BANK_BCA', 'ACC_EWALLET_GOPAY', 'ACC_PAYLATER_SPAY')")
    suspend fun deleteMockPersonalExpenses()
}
