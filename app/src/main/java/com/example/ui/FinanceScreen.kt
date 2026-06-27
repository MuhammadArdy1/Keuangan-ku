package com.example.ui

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccount
import com.example.data.model.Budget
import com.example.data.model.Transaction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(viewModel: FinanceViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showLinkBankDialog by remember { mutableStateOf(false) }
    var selectedBankForLink by remember { mutableStateOf<BankAccount?>(null) }

    // Modern color scheme gradient background
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = currentScreen == "dashboard",
                    onClick = { viewModel.navigateTo("dashboard") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dasbor", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentScreen == "bank",
                    onClick = { viewModel.navigateTo("bank") },
                    icon = { Icon(Icons.Filled.AccountBalance, contentDescription = "Bank") },
                    label = { Text("Rekening", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentScreen == "parser",
                    onClick = { viewModel.navigateTo("parser") },
                    icon = { Icon(Icons.Filled.Receipt, contentDescription = "Deteksi AI") },
                    label = { Text("Deteksi AI", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentScreen == "coach",
                    onClick = { viewModel.navigateTo("coach") },
                    icon = { Icon(Icons.Filled.Chat, contentDescription = "AI Coach") },
                    label = { Text("AI Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                "dashboard" -> DashboardTab(
                    viewModel = viewModel,
                    accounts = accounts,
                    transactions = transactions,
                    budgets = budgets,
                    onAddTransactionClick = { showAddTransactionDialog = true },
                    onAddBudgetClick = { showAddBudgetDialog = true }
                )
                "bank" -> BankTab(
                    viewModel = viewModel,
                    accounts = accounts,
                    onAddAccountClick = { showAddAccountDialog = true },
                    onLinkClick = { account ->
                        selectedBankForLink = account
                        showLinkBankDialog = true
                    }
                )
                "parser" -> ParserTab(
                    viewModel = viewModel
                )
                "coach" -> CoachTab(
                    viewModel = viewModel
                )
            }
        }
    }

    // Modal Dialogs
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { bank, holder, number, balance, type ->
                viewModel.addAccount(bank, holder, number, balance, type)
                showAddAccountDialog = false
            }
        )
    }

    if (showAddTransactionDialog) {
        val linkedAccounts = accounts.filter { it.isLinked }
        if (linkedAccounts.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showAddTransactionDialog = false },
                title = { Text("Belum Ada Rekening") },
                text = { Text("Mohon hubungkan atau buat rekening bank aktif terlebih dahulu di tab Rekening.") },
                confirmButton = {
                    Button(onClick = {
                        showAddTransactionDialog = false
                        viewModel.navigateTo("bank")
                    }) {
                        Text("Hubungkan Sekarang")
                    }
                }
            )
        } else {
            AddTransactionDialog(
                accounts = linkedAccounts,
                onDismiss = { showAddTransactionDialog = false },
                onConfirm = { accountId, amount, category, type, desc ->
                    viewModel.addManualTransaction(accountId, amount, category, type, desc)
                    showAddTransactionDialog = false
                }
            )
        }
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { category, limit ->
                viewModel.addBudget(category, limit)
                showAddBudgetDialog = false
            }
        )
    }

    if (showLinkBankDialog && selectedBankForLink != null) {
        LinkBankSimDialog(
            account = selectedBankForLink!!,
            onDismiss = {
                showLinkBankDialog = false
                selectedBankForLink = null
            },
            onConfirm = {
                viewModel.linkBankAccount(selectedBankForLink!!)
                showLinkBankDialog = false
                selectedBankForLink = null
            }
        )
    }
}

// ==========================================
// TAB 1: DASHBOARD
// ==========================================
@Composable
fun DashboardTab(
    viewModel: FinanceViewModel,
    accounts: List<BankAccount>,
    transactions: List<Transaction>,
    budgets: List<Budget>,
    onAddTransactionClick: () -> Unit,
    onAddBudgetClick: () -> Unit
) {
    val linkedAccounts = accounts.filter { it.isLinked }
    val totalBalance = linkedAccounts.sumOf { it.balance }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KeuanganKu",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Asisten Finansial & Sinkronisasi Bank",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { viewModel.syncAccounts() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Sync Accounts",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Total Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Total Saldo Gabungan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.formatRupiah(totalBalance),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${linkedAccounts.size} Rekening Terhubung",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Tersinkronisasi Baru Saja",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddTransactionClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Transaksi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddBudgetClick,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Atur Anggaran")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Atur Limit Anggaran", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Anggaran Bulanan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pemantauan Anggaran Bulanan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (budgets.isEmpty()) {
                        Text(
                            text = "Belum ada limit anggaran bulan ini. Ketuk tombol 'Atur Limit Anggaran' untuk mengontrol pengeluaran Anda.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        budgets.forEach { budget ->
                            val progress = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat() else 0f
                            val progressColor = when {
                                progress >= 1.0f -> MaterialTheme.colorScheme.error
                                progress >= 0.8f -> Color(0xFFE65100) // Dark orange
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = budget.category,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${viewModel.formatRupiah(budget.spentAmount)} / ${viewModel.formatRupiah(budget.limitAmount)}",
                                        fontSize = 12.sp,
                                        color = if (progress >= 1.0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = progressColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                if (progress >= 1.0f) {
                                    Text(
                                        text = "⚠️ Pengeluaran melampaui limit anggaran!",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Riwayat Transaksi Terbaru
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Transaksi Terbaru",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada transaksi. Silakan tambah transaksi manual atau deteksi otomatis lewat SMS/notifikasi bank.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(transactions.take(15)) { tx ->
                TransactionRow(tx = tx, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction, viewModel: FinanceViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (tx.type == "INCOME") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tx.type == "INCOME") Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = tx.type,
                        tint = if (tx.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = tx.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = tx.bankName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = tx.category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${if (tx.type == "INCOME") "+" else "-"} ${viewModel.formatRupiah(tx.amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (tx.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                
                IconButton(
                    onClick = { viewModel.deleteTransaction(tx) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


// ==========================================
// TAB 2: BANK ACCOUNTS MANAGEMENT
// ==========================================
@Composable
fun BankTab(
    viewModel: FinanceViewModel,
    accounts: List<BankAccount>,
    onAddAccountClick: () -> Unit,
    onLinkClick: (BankAccount) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rekening Bank Anda",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kelola akun bank atau e-wallet yang terhubung.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddAccountClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Text("Buat Rekening", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(accounts) { acc ->
            // Card visual styling for bank account
            val cardBrush = if (acc.isLinked) {
                Brush.linearGradient(
                    colors = when (acc.bankName.uppercase()) {
                        "BCA" -> listOf(Color(0xFF1565C0), Color(0xFF1E88E5))
                        "MANDIRI" -> listOf(Color(0xFFF9A825), Color(0xFFFBC02D))
                        "GOPAY" -> listOf(Color(0xFF00C853), Color(0xFF66BB6A))
                        "OVO" -> listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))
                        else -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    }
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color(0xFF757575), Color(0xFF9E9E9E))
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cardBrush)
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row: Bank Name & Linked Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = acc.bankName,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = acc.type,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (acc.isLinked) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.Green, CircleShape)
                                        )
                                        Text(
                                            text = "TERHUBUNG",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onLinkClick(acc) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Hubungkan", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hubungkan Akun", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Middle row: Account Number & Holder
                        Column {
                            Text(
                                text = acc.accountNumber.chunked(4).joinToString(" "),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = acc.accountHolder.uppercase(),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Bottom row: Balance & Last Sync
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = if (acc.isLinked) viewModel.formatRupiah(acc.balance) else "Saldo Tersembunyi",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            if (acc.isLinked) {
                                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                Text(
                                    text = "Sync: ${sdf.format(Date(acc.lastSyncTime))}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// TAB 3: DETEKSI OTOMATIS AI (PARSER)
// ==========================================
@Composable
fun ParserTab(viewModel: FinanceViewModel) {
    var rawText by remember { mutableStateOf("") }
    val isParsing by viewModel.isParsing.collectAsStateWithLifecycle()
    val parsingResult by viewModel.parsingResult.collectAsStateWithLifecycle()

    val sampleAlerts = listOf(
        "M-TRANS: 27/06/2026 TRANSFER DR REK 8012345678 SEBESAR RP 85.000 KE GRABFOOD CO. REFF: 092831.",
        "GOPAY: PEMBAYARAN KEPADA KOPI KENANGAN SEBESAR RP 36.000 BERHASIL PADA 27/06/2026 12:40. REFF: 9283120.",
        "NOTIFIKASI BANK MANDIRI: TRANSFER MASUK SEBESAR RP 2.500.000 DARI PT MAJU JAYA UNTUK GAJI BULANAN.",
        "OVO INFO: TRF DANA KELUAR RP 125.000 KE REK BCA 8012345678 BERHASIL. BIAYA ADM RP 2.500."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Deteksi Transaksi Bank Otomatis (AI)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Salin & tempel teks SMS notifikasi atau konfirmasi bank Anda di bawah ini. AI KeuanganKu akan membaca transaksi dan mengupdate saldo rekening Anda secara otomatis.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        label = { Text("Tempel SMS atau Notifikasi Bank") },
                        placeholder = { Text("Contoh: M-TRANS: TRANSFER SEBESAR RP 50.000...") },
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.parseNotificationSms(rawText) { success ->
                                if (success) rawText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rawText.isNotBlank() && !isParsing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isParsing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menganalisis Teks dengan AI...")
                        } else {
                            Icon(Icons.Filled.Sms, contentDescription = "Deteksi")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analisis & Sinkronkan Otomatis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Display results
        if (parsingResult != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (parsingResult!!.contains("✅")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hasil Analisis AI",
                                fontWeight = FontWeight.Bold,
                                color = if (parsingResult!!.contains("✅")) Color(0xFF1B5E20) else Color(0xFFE65100),
                                fontSize = 15.sp
                            )
                            IconButton(
                                onClick = { viewModel.clearParsingResult() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parsingResult!!,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Interactive Sandbox Templates
        item {
            Text(
                text = "💡 Sandbox: Klik Contoh Notifikasi di Bawah Ini untuk Tes",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(sampleAlerts) { alertText ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rawText = alertText },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Sms,
                        contentDescription = "SMS Template",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = alertText,
                        fontSize = 11.sp,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// ==========================================
// TAB 4: KONSULTAN FINANSIAL AI (COACH)
// ==========================================
@Composable
fun CoachTab(viewModel: FinanceViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Asisten Finansial AI",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Konsultasi pengeluaran, tips tabungan, & nasihat anggaran.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextButton(onClick = { viewModel.clearChat() }) {
                Text("Hapus Riwayat", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chats History Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg.first, isUser = msg.second)
                }

                if (isChatLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp))
                            Text("KeuanganKu AI sedang mengetik...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input send box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Tanyakan rekomendasi hemat, dll...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isChatLoading) {
                            viewModel.sendChatMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    }
                )
            )

            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank() && !isChatLoading) {
                        viewModel.sendChatMessage(inputText)
                        inputText = ""
                        keyboardController?.hide()
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Kirim")
            }
        }
    }
}

@Composable
fun ChatBubble(message: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Clean markdown parser helper for basic bold styling (**)
            Column(modifier = Modifier.padding(12.dp)) {
                val processedText = remember(message) {
                    parseMarkdownBold(message)
                }
                
                Text(
                    text = processedText,
                    fontSize = 13.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Simple local markdown bold parsing for UI
fun parseMarkdownBold(input: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    val parts = input.split("**")
    for (i in parts.indices) {
        if (i % 2 == 1) {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
            builder.append(parts[i])
            builder.pop()
        } else {
            builder.append(parts[i])
        }
    }
    return builder.toAnnotatedString()
}


// ==========================================
// MODAL DIALOGS IMPLEMENTATIONS
// ==========================================

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, String) -> Unit
) {
    var bankName by remember { mutableStateOf("BCA") }
    var accountHolder by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("SAVINGS") }

    val bankOptions = listOf("BCA", "Mandiri", "BRI", "BNI", "GoPay", "OVO", "Bank Jago")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Buat Rekening Baru", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                // Bank options select row
                Text("Pilih Bank / E-Wallet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val firstThree = bankOptions.take(4)
                    firstThree.forEach { bank ->
                        val isSel = bankName == bank
                        FilterChip(
                            selected = isSel,
                            onClick = { bankName = bank },
                            label = { Text(bank, fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val remaining = bankOptions.drop(4)
                    remaining.forEach { bank ->
                        val isSel = bankName == bank
                        FilterChip(
                            selected = isSel,
                            onClick = { bankName = bank },
                            label = { Text(bank, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = accountHolder,
                    onValueChange = { accountHolder = it },
                    label = { Text("Nama Pemilik Rekening") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Nomor Rekening / HP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Saldo Awal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val types = listOf("SAVINGS" to "Tabungan", "E_WALLET" to "E-Wallet", "CREDIT_CARD" to "Kartu Kredit")
                    types.forEach { (typeKey, typeLabel) ->
                        val isSelected = selectedType == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = typeKey },
                            label = { Text(typeLabel, fontSize = 10.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val balVal = balance.toDoubleOrNull() ?: 0.0
                            onConfirm(bankName, accountHolder, accountNumber, balVal, selectedType)
                        },
                        enabled = accountHolder.isNotBlank() && accountNumber.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, String, String, String) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.first().id) }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Makanan") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Gaji", "Investasi", "Lainnya")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Catat Transaksi Baru", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                // Select Account
                Text("Pilih Sumber Rekening", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text("${acc.bankName} (${acc.accountNumber.takeLast(4)})", fontSize = 9.sp) }
                        )
                    }
                }

                // Expense / Income Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { selectedType = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "EXPENSE") Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "EXPENSE") Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pengeluaran")
                    }
                    Button(
                        onClick = { selectedType = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INCOME") Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pemasukan")
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Jumlah Uang (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Keterangan") },
                    singleLine = true
                )

                Text("Kategori", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 9.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.drop(4).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 9.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            onConfirm(selectedAccountId, amtVal, selectedCategory, selectedType, description)
                        },
                        enabled = amount.isNotBlank() && description.isNotBlank()
                    ) {
                        Text("Catat")
                    }
                }
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var category by remember { mutableStateOf("Makanan") }
    var limitAmount by remember { mutableStateOf("") }
    val categories = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Lainnya")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Set Batas Anggaran Bulanan", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Text("Pilih Kategori", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = limitAmount,
                    onValueChange = { limitAmount = it },
                    label = { Text("Batas Maksimal Anggaran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val limit = limitAmount.toDoubleOrNull() ?: 0.0
                            onConfirm(category, limit)
                        },
                        enabled = limitAmount.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun LinkBankSimDialog(
    account: BankAccount,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hubungkan Akun ${account.bankName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Masukkan kredensial internet banking Anda untuk menghubungkan mutasi secara langsung (Sandbox Demo).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("User ID Internet Banking") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi / PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isVerifying) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isVerifying = true
                            errorMsg = null
                            scope.launch {
                                kotlinx.coroutines.delay(1500) // Simulating checking banking API credentials
                                isVerifying = false
                                onConfirm()
                            }
                        },
                        enabled = username.isNotBlank() && password.isNotBlank() && !isVerifying
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memverifikasi...")
                        } else {
                            Text("Hubungkan Aman")
                        }
                    }
                }
            }
        }
    }
}
