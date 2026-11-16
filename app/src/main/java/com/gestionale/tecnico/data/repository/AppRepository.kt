package com.gestionale.tecnico.data.repository

import com.gestionale.tecnico.data.local.dao.ClientDao
import com.gestionale.tecnico.data.local.dao.ExpiryDao
import com.gestionale.tecnico.data.local.dao.SubscriptionDao
import com.gestionale.tecnico.data.local.dao.TransactionDao
import com.gestionale.tecnico.data.local.entity.*
import com.gestionale.tecnico.util.DateUtils
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val clientDao: ClientDao,
    private val transactionDao: TransactionDao,
    private val expiryDao: ExpiryDao,
    private val subscriptionDao: SubscriptionDao
) {
    // Clients
    fun observeClients(): Flow<List<Client>> = clientDao.observeAll()
    fun observeClient(id: Long): Flow<Client?> = clientDao.observeById(id)
    suspend fun getClient(id: Long) = clientDao.getById(id)
    suspend fun saveClient(c: Client): Long = if (c.id == 0L) clientDao.insert(c) else { clientDao.update(c); c.id }
    suspend fun deleteClient(c: Client) = clientDao.delete(c)

    // Transactions
    fun observeTransactions(): Flow<List<Transaction>> = transactionDao.observeAll()
    fun observeTransactionsByPeriod(start: Long, end: Long) = transactionDao.observeByPeriod(start, end)
    fun observeTransactionsByClient(clientId: Long) = transactionDao.observeByClient(clientId)
    fun observePending() = transactionDao.observePending()
    fun observePendingTotal() = transactionDao.observePendingTotal()
    fun observeIncomeTotal(start: Long, end: Long) = transactionDao.observeIncomeTotal(start, end)
    fun observeExpenseTotal(start: Long, end: Long) = transactionDao.observeExpenseTotal(start, end)
    fun observeClientPending(clientId: Long) = transactionDao.observeClientPending(clientId)
    fun observeClientEarnings(clientId: Long, start: Long, end: Long) = transactionDao.observeClientEarnings(clientId, start, end)

    suspend fun getTransaction(id: Long) = transactionDao.getById(id)

    /**
     * Saves a transaction. If it's an income procured by a colleague with a commission amount,
     * also creates/updates a corresponding USCITA PROVVIGIONE_COLLEGA expense.
     * For simplicity on edit, caller should handle; here we auto-create commission on new insert.
     */
    suspend fun saveTransaction(t: Transaction, autoCreateCommission: Boolean = true): Long {
        val id = if (t.id == 0L) transactionDao.insert(t) else { transactionDao.update(t); t.id }
        if (autoCreateCommission && t.id == 0L && t.tipo == TransactionType.ENTRATA) {
            val prov = t.importoProvvigioneCollega
            if (!t.collegaProcuratore.isNullOrBlank() && prov != null && prov > 0.0) {
                val commission = Transaction(
                    data = t.data,
                    tipo = TransactionType.USCITA,
                    categoria = TransactionCategory.PROVVIGIONE_COLLEGA,
                    importo = prov,
                    descrizione = "Provvigione a ${t.collegaProcuratore} per: ${t.descrizione}".trim(),
                    clientId = t.clientId,
                    statoPagamento = PaymentStatus.PAGATO,
                    collegaProcuratore = t.collegaProcuratore,
                    importoProvvigioneCollega = null
                )
                transactionDao.insert(commission)
            }
        }
        return id
    }

    suspend fun updateTransaction(t: Transaction) = transactionDao.update(t)
    suspend fun deleteTransaction(t: Transaction) = transactionDao.delete(t)
    suspend fun markPaid(id: Long) = transactionDao.markPaid(id)

    // Expiries
    fun observeExpiries() = expiryDao.observeAll()
    fun observeUpcomingExpiries(now: Long, limit: Long) = expiryDao.observeUpcoming(now, limit)
    suspend fun getExpiry(id: Long) = expiryDao.getById(id)
    suspend fun saveExpiry(e: ExpiryItem): Long = if (e.id == 0L) expiryDao.insert(e) else { expiryDao.update(e); e.id }
    suspend fun deleteExpiry(e: ExpiryItem) = expiryDao.delete(e)

    // Subscriptions
    fun observeSubscriptions() = subscriptionDao.observeAll()
    fun observeSubscriptionsByClient(clientId: Long) = subscriptionDao.observeByClient(clientId)
    fun observeUpcomingSubscriptions(now: Long, limit: Long) = subscriptionDao.observeUpcoming(now, limit)
    suspend fun getSubscription(id: Long) = subscriptionDao.getById(id)
    suspend fun saveSubscription(s: SubscriptionContract): Long = if (s.id == 0L) subscriptionDao.insert(s) else { subscriptionDao.update(s); s.id }
    suspend fun deleteSubscription(s: SubscriptionContract) = subscriptionDao.delete(s)

    suspend fun renewSubscription(s: SubscriptionContract, registerPayment: Boolean = true) {
        val months = if (s.frequenza == SubscriptionFrequency.MENSILE) 1L else 6L
        val next = DateUtils.plusMonths(s.prossimoRinnovo, months)
        subscriptionDao.update(s.copy(prossimoRinnovo = next))
        if (registerPayment) {
            transactionDao.insert(
                Transaction(
                    data = System.currentTimeMillis(),
                    tipo = TransactionType.ENTRATA,
                    categoria = TransactionCategory.ASSISTENZA,
                    importo = s.importo,
                    descrizione = "Abbonamento teleassistenza ${s.frequenza.name.lowercase()} - ${s.titolo}",
                    clientId = s.clientId,
                    statoPagamento = PaymentStatus.PAGATO
                )
            )
        }
    }

    // Backup helpers
    suspend fun allClients() = clientDao.getAllOnce()
    suspend fun allTransactions() = transactionDao.getAllOnce()
    suspend fun allExpiries() = expiryDao.getAllOnce()
    suspend fun allSubscriptions() = subscriptionDao.getAllOnce()
    suspend fun insertClientRaw(c: Client) = clientDao.insert(c)
    suspend fun insertTransactionRaw(t: Transaction) = transactionDao.insert(t)
    suspend fun insertExpiryRaw(e: ExpiryItem) = expiryDao.insert(e)
    suspend fun insertSubscriptionRaw(s: SubscriptionContract) = subscriptionDao.insert(s)
    suspend fun clearForRestore(dbClear: suspend () -> Unit) = dbClear()

    suspend fun sumByCategory(tipo: TransactionType, cat: TransactionCategory, start: Long, end: Long, onlyPaid: Boolean) =
        transactionDao.sumByCategory(tipo, cat, start, end, if (onlyPaid) 1 else 0)
    suspend fun sumExpenses(start: Long, end: Long) = transactionDao.sumExpenses(start, end)
}
