package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: String = "USD"
)

@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey val ticker: String, // e.g. "AAPL"
    val name: String,              // e.g. "Apple Inc."
    val category: String,          // e.g. "US Equity", "Fixed Income", "Real Estate", "Crypto"
    val sector: String,            // e.g. "Technology", "Healthcare", "Financials"
    val targetAllocation: Double,  // percentage e.g. 15.0 for 15%
    val currentPrice: Double,      // in investment's base currency
    val baseCurrency: String = "USD"
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val ticker: String,
    val type: String,             // BUY, SELL, SPLIT, DIVIDEND, ROC (Return of Capital)
    val shares: Double,           // positive for Buy/Split, negative for Sell, 0 for Dividend/ROC
    val price: Double,            // price per share
    val totalAmount: Double,      // negative for BUY, positive for SELL/DIVIDEND/ROC (cash impact)
    val accountId: Long,          // reference to Account.id
    val notes: String = ""
)

@Entity(tableName = "upcoming_dividends")
data class UpcomingDividend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticker: String,
    val exDateMillis: Long,
    val payoutDateMillis: Long,
    val amountPerShare: Double,
    val notes: String = "",
    val isAlertEnabled: Boolean = true,
    val reminded: Boolean = false
)

@Entity(tableName = "currency_rates")
data class CurrencyRate(
    @PrimaryKey val currencyPair: String, // e.g. "USD/EUR", "USD/CAD"
    val rate: Double
)
