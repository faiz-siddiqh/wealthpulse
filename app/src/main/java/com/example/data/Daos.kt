package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccountsFlow(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllAccounts(): List<Account>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)
}

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY ticker ASC")
    fun getAllInvestmentsFlow(): Flow<List<Investment>>

    @Query("SELECT * FROM investments WHERE ticker = :ticker LIMIT 1")
    suspend fun getInvestmentByTicker(ticker: String): Investment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: Investment)

    @Update
    suspend fun updateInvestment(investment: Investment)

    @Delete
    suspend fun deleteInvestment(investment: Investment)

    @Query("SELECT * FROM investments ORDER BY ticker ASC")
    suspend fun getAllInvestments(): List<Investment>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE ticker = :ticker ORDER BY dateMillis ASC, id ASC")
    fun getTransactionsByTickerFlow(ticker: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateMillis DESC")
    fun getTransactionsByAccountFlow(accountId: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY dateMillis ASC")
    suspend fun getAllTransactions(): List<Transaction>
}

@Dao
interface UpcomingDividendDao {
    @Query("SELECT * FROM upcoming_dividends ORDER BY payoutDateMillis ASC")
    fun getAllUpcomingDividendsFlow(): Flow<List<UpcomingDividend>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpcomingDividend(upcomingDividend: UpcomingDividend): Long

    @Update
    suspend fun updateUpcomingDividend(upcomingDividend: UpcomingDividend)

    @Delete
    suspend fun deleteUpcomingDividend(upcomingDividend: UpcomingDividend)

    @Query("SELECT * FROM upcoming_dividends WHERE payoutDateMillis >= :timeMillis AND reminded = 0")
    suspend fun getUnremindedUpcomingDividends(timeMillis: Long): List<UpcomingDividend>
}

@Dao
interface CurrencyRateDao {
    @Query("SELECT * FROM currency_rates")
    fun getAllRatesFlow(): Flow<List<CurrencyRate>>

    @Query("SELECT * FROM currency_rates")
    suspend fun getAllRates(): List<CurrencyRate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: CurrencyRate)
}
