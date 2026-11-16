package com.gestionale.tecnico.data.local.dao

import androidx.room.*
import com.gestionale.tecnico.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class ClientWithStats(
    @Embedded val client: Client,
    val sospesi: Double?,
    val totaleGuadagni: Double?
)

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY nome COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    fun observeById(id: Long): Flow<Client?>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: Long): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client): Long

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT * FROM clients")
    suspend fun getAllOnce(): List<Client>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY data DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE clientId = :clientId ORDER BY data DESC")
    fun observeByClient(clientId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE statoPagamento = 'IN_SOSPESO' AND tipo = 'ENTRATA' ORDER BY data DESC")
    fun observePending(): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE data >= :start AND data < :end 
        ORDER BY data DESC
    """)
    fun observeByPeriod(start: Long, end: Long): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(SUM(importo), 0) FROM transactions 
        WHERE tipo = 'ENTRATA' AND statoPagamento = 'PAGATO' AND data >= :start AND data < :end
    """)
    fun observeIncomeTotal(start: Long, end: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(importo), 0) FROM transactions 
        WHERE tipo = 'USCITA' AND data >= :start AND data < :end
    """)
    fun observeExpenseTotal(start: Long, end: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(importo), 0) FROM transactions 
        WHERE tipo = 'ENTRATA' AND statoPagamento = 'IN_SOSPESO'
    """)
    fun observePendingTotal(): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(importo), 0) FROM transactions 
        WHERE clientId = :clientId AND tipo = 'ENTRATA' AND statoPagamento = 'IN_SOSPESO'
    """)
    fun observeClientPending(clientId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(importo), 0) FROM transactions 
        WHERE clientId = :clientId AND tipo = 'ENTRATA' AND statoPagamento = 'PAGATO' AND data >= :start AND data < :end
    """)
    fun observeClientEarnings(clientId: Long, start: Long, end: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: Transaction): Long

    @Update
    suspend fun update(t: Transaction)

    @Delete
    suspend fun delete(t: Transaction)

    @Query("UPDATE transactions SET statoPagamento = 'PAGATO' WHERE id = :id")
    suspend fun markPaid(id: Long)

    @Query("SELECT * FROM transactions")
    suspend fun getAllOnce(): List<Transaction>

    @Query("""
        SELECT COALESCE(SUM(importo),0) FROM transactions
        WHERE tipo = :tipo AND categoria = :categoria AND data >= :start AND data < :end
        AND (:onlyPaid = 0 OR statoPagamento = 'PAGATO')
    """)
    suspend fun sumByCategory(tipo: TransactionType, categoria: TransactionCategory, start: Long, end: Long, onlyPaid: Int): Double

    @Query("""
        SELECT COALESCE(SUM(importo),0) FROM transactions
        WHERE tipo = 'USCITA' AND data >= :start AND data < :end
    """)
    suspend fun sumExpenses(start: Long, end: Long): Double
}

@Dao
interface ExpiryDao {
    @Query("SELECT * FROM expiry_items ORDER BY dataScadenza ASC")
    fun observeAll(): Flow<List<ExpiryItem>>

    @Query("SELECT * FROM expiry_items WHERE dataScadenza >= :now AND dataScadenza <= :limit ORDER BY dataScadenza ASC")
    fun observeUpcoming(now: Long, limit: Long): Flow<List<ExpiryItem>>

    @Query("SELECT * FROM expiry_items WHERE id = :id")
    suspend fun getById(id: Long): ExpiryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExpiryItem): Long

    @Update
    suspend fun update(item: ExpiryItem)

    @Delete
    suspend fun delete(item: ExpiryItem)

    @Query("SELECT * FROM expiry_items")
    suspend fun getAllOnce(): List<ExpiryItem>
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY attivo DESC, prossimoRinnovo ASC")
    fun observeAll(): Flow<List<SubscriptionContract>>

    @Query("SELECT * FROM subscriptions WHERE clientId = :clientId ORDER BY prossimoRinnovo ASC")
    fun observeByClient(clientId: Long): Flow<List<SubscriptionContract>>

    @Query("SELECT * FROM subscriptions WHERE attivo = 1 AND prossimoRinnovo >= :now AND prossimoRinnovo <= :limit ORDER BY prossimoRinnovo ASC")
    fun observeUpcoming(now: Long, limit: Long): Flow<List<SubscriptionContract>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): SubscriptionContract?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SubscriptionContract): Long

    @Update
    suspend fun update(item: SubscriptionContract)

    @Delete
    suspend fun delete(item: SubscriptionContract)

    @Query("SELECT * FROM subscriptions")
    suspend fun getAllOnce(): List<SubscriptionContract>
}
