package com.example.data.repository

import com.example.data.local.BankAccountDao
import com.example.data.local.BudgetDao
import com.example.data.local.TransactionDao
import com.example.data.model.BankAccount
import com.example.data.model.Budget
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class FinanceRepository(
    private val bankAccountDao: BankAccountDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    val allAccounts: Flow<List<BankAccount>> = bankAccountDao.getAllAccounts()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(monthYear)
    }

    suspend fun getAccountById(id: Int): BankAccount? {
        return bankAccountDao.getAccountById(id)
    }

    suspend fun insertAccount(account: BankAccount): Long {
        return bankAccountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: BankAccount) {
        bankAccountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: BankAccount) {
        bankAccountDao.deleteAccount(account)
    }

    suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }

    // Double-entry helper to insert transaction, update bank balance and budgetspent
    suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)

        // Adjust Bank Account Balance
        val account = bankAccountDao.getAccountById(transaction.bankAccountId)
        if (account != null) {
            val multiplier = if (transaction.type == "INCOME") 1.0 else -1.0
            val newBalance = account.balance + (transaction.amount * multiplier)
            bankAccountDao.updateAccount(
                account.copy(
                    balance = newBalance,
                    lastSyncTime = System.currentTimeMillis()
                )
            )
        }

        // Adjust spent budget if it's an expense
        if (transaction.type == "EXPENSE") {
            val monthYear = getMonthYearFromTimestamp(transaction.timestamp)
            val budget = budgetDao.getBudgetByCategoryAndMonth(transaction.category, monthYear)
            if (budget != null) {
                val newSpent = budget.spentAmount + transaction.amount
                budgetDao.updateBudget(budget.copy(spentAmount = newSpent))
            } else {
                // Auto create budget if missing to make tracking friendly
                budgetDao.insertBudget(
                    Budget(
                        category = transaction.category,
                        limitAmount = 2000000.0, // default limit
                        spentAmount = transaction.amount,
                        monthYear = monthYear
                    )
                )
            }
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)

        // Reverse Bank Account Balance
        val account = bankAccountDao.getAccountById(transaction.bankAccountId)
        if (account != null) {
            val multiplier = if (transaction.type == "INCOME") -1.0 else 1.0
            val newBalance = account.balance + (transaction.amount * multiplier)
            bankAccountDao.updateAccount(
                account.copy(
                    balance = newBalance,
                    lastSyncTime = System.currentTimeMillis()
                )
            )
        }

        // Reverse Budget spent
        if (transaction.type == "EXPENSE") {
            val monthYear = getMonthYearFromTimestamp(transaction.timestamp)
            val budget = budgetDao.getBudgetByCategoryAndMonth(transaction.category, monthYear)
            if (budget != null) {
                val newSpent = (budget.spentAmount - transaction.amount).coerceAtLeast(0.0)
                budgetDao.updateBudget(budget.copy(spentAmount = newSpent))
            }
        }
    }

    suspend fun clearAllTransactions() {
        transactionDao.clearAllTransactions()
    }

    // Seed initial data if the DB is empty
    suspend fun seedInitialDataIfNecessary(currentMonthYear: String) {
        // Check if accounts exist
        // Note: we can read existing accounts as a list in a simple suspend block
    }

    fun getMonthYearFromTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
