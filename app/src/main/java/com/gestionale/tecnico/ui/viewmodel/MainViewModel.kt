package com.gestionale.tecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestionale.tecnico.data.local.entity.*
import com.gestionale.tecnico.data.repository.AppRepository
import com.gestionale.tecnico.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DashboardState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val pendingTotal: Double = 0.0,
    val pendingItems: List<Transaction> = emptyList(),
    val upcomingExpiries: List<ExpiryItem> = emptyList(),
    val upcomingSubscriptions: List<SubscriptionContract> = emptyList()
) {
    val net: Double get() = income - expenses
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repo: AppRepository) : ViewModel() {
    private val selected = MutableStateFlow(YearMonth.now())

    val clients = repo.observeClients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTransactions = repo.observeTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExpiries = repo.observeExpiries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSubscriptions = repo.observeSubscriptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val pending = repo.observePending().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val pendingTotal = repo.observePendingTotal().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val upcoming = repo.observeUpcomingExpiries(
        System.currentTimeMillis(),
        System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val upcomingSubs = repo.observeUpcomingSubscriptions(
        System.currentTimeMillis(),
        System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboard: StateFlow<DashboardState> = selected.flatMapLatest { ym ->
        val start = DateUtils.startOfMonth(ym.year, ym.monthValue)
        val end = DateUtils.endOfMonthExclusive(ym.year, ym.monthValue)
        combine(
            repo.observeIncomeTotal(start, end),
            repo.observeExpenseTotal(start, end),
            pendingTotal,
            pending,
            upcoming,
            upcomingSubs
        ) { arr ->
            val income = arr[0] as Double
            val exp = arr[1] as Double
            val pTot = arr[2] as Double
            @Suppress("UNCHECKED_CAST") val pItems = arr[3] as List<Transaction>
            @Suppress("UNCHECKED_CAST") val ups = arr[4] as List<ExpiryItem>
            @Suppress("UNCHECKED_CAST") val subs = arr[5] as List<SubscriptionContract>
            DashboardState(ym.year, ym.monthValue, income, exp, pTot, pItems, ups, subs)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    fun setMonth(year: Int, month: Int) { selected.value = YearMonth.of(year, month) }
    fun prevMonth() { selected.value = selected.value.minusMonths(1) }
    fun nextMonth() { selected.value = selected.value.plusMonths(1) }

    fun saveClient(c: Client) = viewModelScope.launch { repo.saveClient(c) }
    fun deleteClient(c: Client) = viewModelScope.launch { repo.deleteClient(c) }

    fun saveTransaction(t: Transaction, autoCommission: Boolean = true) = viewModelScope.launch { repo.saveTransaction(t, autoCommission) }
    fun updateTransaction(t: Transaction) = viewModelScope.launch { repo.updateTransaction(t) }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch { repo.deleteTransaction(t) }
    fun markPaid(id: Long) = viewModelScope.launch { repo.markPaid(id) }

    fun saveExpiry(e: ExpiryItem) = viewModelScope.launch { repo.saveExpiry(e) }
    fun deleteExpiry(e: ExpiryItem) = viewModelScope.launch { repo.deleteExpiry(e) }
    fun renewExpiry(e: ExpiryItem) = viewModelScope.launch {
        val annuale = e.durata == ExpiryDuration.ANNUALE
        repo.saveExpiry(e.copy(dataScadenza = DateUtils.plusDuration(e.dataScadenza, annuale), notificato = false))
    }

    fun saveSubscription(s: SubscriptionContract) = viewModelScope.launch { repo.saveSubscription(s) }
    fun deleteSubscription(s: SubscriptionContract) = viewModelScope.launch { repo.deleteSubscription(s) }
    fun renewSubscription(s: SubscriptionContract, registerPayment: Boolean = true) = viewModelScope.launch { repo.renewSubscription(s, registerPayment) }
    fun subscriptionsForClient(clientId: Long) = repo.observeSubscriptionsByClient(clientId)

    fun transactionsForClient(clientId: Long) = repo.observeTransactionsByClient(clientId)
    fun clientPending(clientId: Long) = repo.observeClientPending(clientId)
    fun clientEarnings(clientId: Long, start: Long, end: Long) = repo.observeClientEarnings(clientId, start, end)

    suspend fun sumByCategory(tipo: TransactionType, cat: TransactionCategory, start: Long, end: Long, onlyPaid: Boolean) =
        repo.sumByCategory(tipo, cat, start, end, onlyPaid)
    suspend fun sumExpenses(start: Long, end: Long) = repo.sumExpenses(start, end)
}

class MainVmFactory(private val repo: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
