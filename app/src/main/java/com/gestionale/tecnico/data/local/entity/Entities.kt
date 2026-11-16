package com.gestionale.tecnico.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType { ENTRATA, USCITA }

enum class TransactionCategory {
    ASSISTENZA, RIVENDITA,           // entrate
    ACQUISTO_MATERIALE, SPESE_GESTIONE, PROVVIGIONE_COLLEGA, ALTRO // uscite
}

enum class PaymentStatus { PAGATO, IN_SOSPESO }

enum class ExpiryType { FIRMA_DIGITALE, SOFTWARE }

enum class ExpiryDuration { ANNUALE, TRIENNALE }

enum class SubscriptionFrequency { MENSILE, SEMESTRALE }

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val telefono: String = "",
    val note: String = ""
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("clientId"), Index("data"), Index("statoPagamento")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: Long,
    val tipo: TransactionType,
    val categoria: TransactionCategory,
    val importo: Double,
    val descrizione: String = "",
    val clientId: Long? = null,
    val statoPagamento: PaymentStatus = PaymentStatus.PAGATO,
    val collegaProcuratore: String? = null,
    val importoProvvigioneCollega: Double? = null
)

@Entity(
    tableName = "expiry_items",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId"), Index("dataScadenza")]
)
data class ExpiryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titolo: String,
    val clientId: Long,
    val tipoScadenza: ExpiryType,
    val dataScadenza: Long,
    val durata: ExpiryDuration,
    val notificato: Boolean = false
)

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId"), Index("prossimoRinnovo"), Index("attivo")]
)
data class SubscriptionContract(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val titolo: String = "Teleassistenza",
    val frequenza: SubscriptionFrequency,
    val importo: Double,
    val dataInizio: Long,
    val prossimoRinnovo: Long,
    val attivo: Boolean = true,
    val note: String = ""
)
