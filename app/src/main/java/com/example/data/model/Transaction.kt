package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankAccountId: Int,       // Reference to BankAccount.id
    val bankName: String,         // Cache for display convenience
    val amount: Double,
    val category: String,         // e.g., "Makanan", "Transportasi", "Belanja", "Gaji", "Kesehatan", "Lainnya"
    val type: String,             // "INCOME" or "EXPENSE"
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val referenceNumber: String? = null,
    val rawNotification: String? = null // For transparency/audit
)
