package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BankAccount
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.repository.FinanceRepository
import com.example.api.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(
        database.bankAccountDao(),
        database.transactionDao(),
        database.budgetDao()
    )

    // UI States
    val accounts: StateFlow<List<BankAccount>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentMonthYear = MutableStateFlow(getCurrentMonthYear())
    val currentMonthYear: StateFlow<String> = _currentMonthYear.asStateFlow()

    val budgets: StateFlow<List<Budget>> = _currentMonthYear
        .flatMapLatest { monthYear -> repository.getBudgetsForMonth(monthYear) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat with AI State
    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            Pair("Halo! Saya **KeuanganKu AI**, asisten finansial pribadimu. Saya dapat menganalisis pengeluaranmu, memberikan saran anggaran, atau membantu menghubungkan dan memproses notifikasi bank. Ada yang bisa saya bantu hari ini?", false)
        )
    )
    val chatMessages: StateFlow<List<Pair<String, Boolean>>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Parsing Status
    private val _parsingResult = MutableStateFlow<String?>(null)
    val parsingResult: StateFlow<String?> = _parsingResult.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    // Screen State for Navigation
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed sample accounts if empty
            accounts.collect { list ->
                if (list.isEmpty()) {
                    seedSampleData()
                }
            }
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    private fun getCurrentMonthYear(): String {
        val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private suspend fun seedSampleData() {
        // Core initial accounts
        val bca = BankAccount(
            bankName = "BCA",
            accountHolder = "BUDI SANTOSO",
            accountNumber = "8012345678",
            balance = 12500000.0,
            type = "SAVINGS",
            isLinked = true
        )
        val gopay = BankAccount(
            bankName = "GoPay",
            accountHolder = "Budi Santoso (Personal)",
            accountNumber = "081234567890",
            balance = 750000.0,
            type = "E_WALLET",
            isLinked = true
        )
        val mandiri = BankAccount(
            bankName = "Mandiri",
            accountHolder = "BUDI SANTOSO",
            accountNumber = "1310012345678",
            balance = 0.0,
            type = "SAVINGS",
            isLinked = false // Not linked initially to let users experience linking
        )

        val bcaId = repository.insertAccount(bca).toInt()
        val gopayId = repository.insertAccount(gopay).toInt()
        repository.insertAccount(mandiri)

        // Seed some initial budgets for current month
        val monthYear = getCurrentMonthYear()
        repository.insertBudget(Budget(category = "Makanan", limitAmount = 2500000.0, spentAmount = 650000.0, monthYear = monthYear))
        repository.insertBudget(Budget(category = "Transportasi", limitAmount = 1000000.0, spentAmount = 200000.0, monthYear = monthYear))
        repository.insertBudget(Budget(category = "Belanja", limitAmount = 3000000.0, spentAmount = 1200000.0, monthYear = monthYear))
        repository.insertBudget(Budget(category = "Tagihan", limitAmount = 2000000.0, spentAmount = 1500000.0, monthYear = monthYear))

        // Seed some initial transactions
        repository.addTransaction(
            Transaction(
                bankAccountId = bcaId,
                bankName = "BCA",
                amount = 15000000.0,
                category = "Gaji",
                type = "INCOME",
                description = "Gaji Bulanan PT Maju Jaya",
                timestamp = System.currentTimeMillis() - 86400000 * 5 // 5 days ago
            )
        )
        repository.addTransaction(
            Transaction(
                bankAccountId = bcaId,
                bankName = "BCA",
                amount = 1500000.0,
                category = "Tagihan",
                type = "EXPENSE",
                description = "Pembayaran Listrik & Wifi",
                timestamp = System.currentTimeMillis() - 86400000 * 4 // 4 days ago
            )
        )
        repository.addTransaction(
            Transaction(
                bankAccountId = gopayId,
                bankName = "GoPay",
                amount = 150000.0,
                category = "Makanan",
                type = "EXPENSE",
                description = "Makan Siang Nasi Padang",
                timestamp = System.currentTimeMillis() - 86400000 * 2 // 2 days ago
            )
        )
        repository.addTransaction(
            Transaction(
                bankAccountId = gopayId,
                bankName = "GoPay",
                amount = 500000.0,
                category = "Makanan",
                type = "EXPENSE",
                description = "Belanja Bulanan Groceries",
                timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
            )
        )
    }

    // Connect / Link Bank Account
    fun linkBankAccount(account: BankAccount, simTransactions: Boolean = true) {
        viewModelScope.launch {
            val updated = account.copy(
                isLinked = true,
                balance = if (account.balance == 0.0) 4200000.0 else account.balance,
                lastSyncTime = System.currentTimeMillis()
            )
            repository.updateAccount(updated)

            if (simTransactions) {
                // Generate a few realistic transactions from this newly linked account
                val accountId = updated.id
                repository.addTransaction(
                    Transaction(
                        bankAccountId = accountId,
                        bankName = updated.bankName,
                        amount = 3500000.0,
                        category = "Gaji",
                        type = "INCOME",
                        description = "Transfer Masuk Bank Lain",
                        timestamp = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
                    )
                )
                repository.addTransaction(
                    Transaction(
                        bankAccountId = accountId,
                        bankName = updated.bankName,
                        amount = 200000.0,
                        category = "Transportasi",
                        type = "EXPENSE",
                        description = "Pengisian Bahan Bakar Pertamax",
                        timestamp = System.currentTimeMillis() - 1800000 // 30 mins ago
                    )
                )
            }
        }
    }

    // Add Account manually
    fun addAccount(bankName: String, accountHolder: String, accountNumber: String, balance: Double, type: String) {
        viewModelScope.launch {
            val account = BankAccount(
                bankName = bankName,
                accountHolder = accountHolder,
                accountNumber = accountNumber,
                balance = balance,
                type = type,
                isLinked = true
            )
            repository.insertAccount(account)
        }
    }

    // Add Manual Transaction
    fun addManualTransaction(
        accountId: Int,
        amount: Double,
        category: String,
        type: String,
        description: String
    ) {
        viewModelScope.launch {
            val account = repository.getAccountById(accountId) ?: return@launch
            val tx = Transaction(
                bankAccountId = accountId,
                bankName = account.bankName,
                amount = amount,
                category = category,
                type = type,
                description = description,
                timestamp = System.currentTimeMillis()
            )
            repository.addTransaction(tx)
        }
    }

    // Add Budget
    fun addBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            val monthYear = getCurrentMonthYear()
            val existing = budgets.value.find { it.category == category && it.monthYear == monthYear }
            if (existing != null) {
                repository.updateBudget(existing.copy(limitAmount = limitAmount))
            } else {
                repository.insertBudget(Budget(category = category, limitAmount = limitAmount, monthYear = monthYear))
            }
        }
    }

    // Parse notification text / SMS copy-paste
    fun parseNotificationSms(text: String, onComplete: (Boolean) -> Unit) {
        if (text.isBlank()) return
        _isParsing.value = true
        _parsingResult.value = null

        viewModelScope.launch {
            try {
                val result = GeminiClient.parseBankSms(text)
                if (result != null) {
                    // Find a linked bank account matching the bank name, or pick the first linked one
                    val activeAccounts = accounts.value.filter { it.isLinked }
                    val matchedAccount = activeAccounts.find { it.bankName.equals(result.bankName, ignoreCase = true) }
                        ?: activeAccounts.firstOrNull()

                    if (matchedAccount != null) {
                        val transaction = Transaction(
                            bankAccountId = matchedAccount.id,
                            bankName = matchedAccount.bankName,
                            amount = result.amount,
                            category = result.category,
                            type = result.type,
                            description = result.description,
                            referenceNumber = result.referenceNumber,
                            rawNotification = text,
                            timestamp = System.currentTimeMillis()
                        )
                        repository.addTransaction(transaction)

                        val fmtAmount = formatRupiah(result.amount)
                        _parsingResult.value = "✅ Sukses mengidentifikasi transaksi!\n" +
                                "• Bank: ${matchedAccount.bankName}\n" +
                                "• Tipe: ${if (result.type == "INCOME") "Pemasukan 🟢" else "Pengeluaran 🔴"}\n" +
                                "• Jumlah: $fmtAmount\n" +
                                "• Kategori: ${result.category}\n" +
                                "• Keterangan: ${result.description}"
                        
                        // Add notification to chat for assistant awareness
                        _chatMessages.value = _chatMessages.value + Pair(
                            "Saya baru saja mengimpor otomatis transaksi berdasarkan notifikasi bank yang Anda masukkan:\n" +
                            "**${result.description}** sebesar **$fmtAmount** (${result.category}) dari rekening **${matchedAccount.bankName}**.",
                            false
                        )
                        onComplete(true)
                    } else {
                        _parsingResult.value = "⚠️ Transaksi dikenali sebagai ${result.bankName}, tetapi tidak ada Rekening Bank aktif yang terhubung. Mohon hubungkan rekening bank Anda terlebih dahulu."
                        onComplete(false)
                    }
                } else {
                    _parsingResult.value = "❌ AI tidak dapat mengidentifikasi format transaksi dari teks tersebut. Pastikan teks berisi informasi bank, jumlah uang, dan status transaksi."
                    onComplete(false)
                }
            } catch (e: Exception) {
                _parsingResult.value = "❌ Error saat memproses: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                _isParsing.value = false
            }
        }
    }

    // Reset parsing message
    fun clearParsingResult() {
        _parsingResult.value = null
    }

    // Sync all linked accounts with simulated updates
    fun syncAccounts() {
        viewModelScope.launch {
            val linked = accounts.value.filter { it.isLinked }
            for (acc in linked) {
                // Randomly add a small simulated transaction and update balance
                val randomAmount = (20000..150000).random().toDouble()
                val isIncome = (1..10).random() > 8 // 20% chance of income
                val category = if (isIncome) "Investasi" else listOf("Makanan", "Transportasi", "Belanja").random()
                val desc = if (isIncome) "Deviden Saham Bulanan" else listOf("Makan Bakso Lapangan", "Gojek Ride", "Minimarket Alfa").random()
                
                val tx = Transaction(
                    bankAccountId = acc.id,
                    bankName = acc.bankName,
                    amount = randomAmount,
                    category = category,
                    type = if (isIncome) "INCOME" else "EXPENSE",
                    description = "$desc (Auto-Sync)",
                    timestamp = System.currentTimeMillis()
                )
                repository.addTransaction(tx)
                
                // Update Last Sync Time
                repository.updateAccount(acc.copy(lastSyncTime = System.currentTimeMillis()))
            }
        }
    }

    // Chat Financial Coach using Gemini API
    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return

        // Append user message
        val updatedHistory = _chatMessages.value + Pair(messageText, true)
        _chatMessages.value = updatedHistory
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                // Prepare context summarizing current financial status to supply to Gemini
                val totalBalance = accounts.value.filter { it.isLinked }.sumOf { it.balance }
                val accountsSummary = accounts.value.joinToString("\n") { 
                    "- ${it.bankName} (${it.type}): Balance ${formatRupiah(it.balance)} (${if (it.isLinked) "Terhubung" else "Belum Terhubung"})"
                }
                
                val latestTxs = transactions.value.take(5).joinToString("\n") {
                    "- [${if (it.type == "INCOME") "PEMASUKAN" else "PENGELUARAN"}] ${it.description} (${it.category}): ${formatRupiah(it.amount)}"
                }

                val budgetsSummary = budgets.value.joinToString("\n") {
                    "- ${it.category}: Terpakai ${formatRupiah(it.spentAmount)} / Batas ${formatRupiah(it.limitAmount)} (${(it.spentAmount / it.limitAmount * 100).toInt()}% terlaksana)"
                }

                val systemInstruction = """
                    Anda adalah **KeuanganKu AI**, asisten finansial cerdas dan konsultan keuangan pribadi pengguna.
                    Gunakan data keuangan berikut sebagai referensi real-time Anda untuk menjawab pertanyaan pengguna dengan sangat personal, akurat, santun, dan dalam bahasa Indonesia yang alami:
                    
                    === DATA REKENING BANK ===
                    $accountsSummary
                    Total Saldo Aktif: ${formatRupiah(totalBalance)}
                    
                    === ANGGARAN (BULAN INI) ===
                    $budgetsSummary
                    
                    === 5 TRANSAKSI TERAKHIR ===
                    $latestTxs
                    
                    === PETUNJUK ===
                    1. Jawab pertanyaan pengguna dengan panduan finansial yang realistis dan bijak.
                    2. Jika pengguna meminta saran anggaran, analisis data anggaran di atas dan berikan tips pemotongan biaya.
                    3. Format jawaban dengan markdown agar rapi, menarik, dan mudah dibaca (gunakan bold, list bullet, dll).
                    4. Ingatkan pengguna untuk selalu mencatat atau mengunggah notifikasi bank agar data tetap sinkron.
                """.trimIndent()

                // Call Gemini Client API using conversational history
                val response = GeminiClient.generateChatResponse(
                    history = updatedHistory.drop(1), // ignore greeting message role structure if too long, or pass all
                    userPrompt = messageText,
                    systemInstruction = systemInstruction
                )

                _chatMessages.value = _chatMessages.value + Pair(response, false)
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + Pair("Maaf, terjadi kesalahan saat menghubungi asisten AI: ${e.localizedMessage}", false)
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            Pair("Halo! Saya **KeuanganKu AI**, asisten finansial pribadimu. Ada yang bisa saya bantu hari ini?", false)
        )
    }

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }
}
