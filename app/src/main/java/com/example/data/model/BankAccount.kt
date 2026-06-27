package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,         // e.g., "BCA", "Mandiri", "BRI", "BNI", "GoPay", "OVO"
    val accountHolder: String,
    val accountNumber: String,
    val balance: Double,
    val type: String,             // "SAVINGS", "E_WALLET", "CREDIT_CARD"
    val isLinked: Boolean = false,
    val lastSyncTime: Long = System.currentTimeMillis()
)
