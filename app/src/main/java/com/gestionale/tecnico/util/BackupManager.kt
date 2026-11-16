package com.gestionale.tecnico.util

import android.content.Context
import android.net.Uri
import com.gestionale.tecnico.data.local.database.AppDatabase
import com.gestionale.tecnico.data.local.entity.Client
import com.gestionale.tecnico.data.local.entity.ExpiryItem
import com.gestionale.tecnico.data.local.entity.SubscriptionContract
import com.gestionale.tecnico.data.local.entity.Transaction
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupPayload(
    val clients: List<Client> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val expiryItems: List<ExpiryItem> = emptyList(),
    val subscriptions: List<SubscriptionContract> = emptyList()
)

object BackupManager {
    private val gson = Gson()

    suspend fun export(context: Context, db: AppDatabase, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupPayload(
                clients = db.clientDao().getAllOnce(),
                transactions = db.transactionDao().getAllOnce(),
                expiryItems = db.expiryDao().getAllOnce(),
                subscriptions = db.subscriptionDao().getAllOnce()
            )
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.writer().use { it.write(gson.toJson(payload)) }
            } ?: error("Impossibile aprire il file")
        }
    }

    suspend fun import(context: Context, db: AppDatabase, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                ?: error("Impossibile leggere il file")
            val payload = gson.fromJson(json, BackupPayload::class.java) ?: error("Backup non valido")
            db.runInTransaction {
                // simple clear+restore via blocking calls is not possible in runInTransaction with suspend;
                // handled outside
            }
            // Clear and restore
            db.clearAllTables()
            payload.clients.forEach { db.clientDao().insert(it.copy()) }
            payload.transactions.forEach { db.transactionDao().insert(it.copy()) }
            payload.expiryItems.forEach { db.expiryDao().insert(it.copy()) }
            payload.subscriptions.forEach { db.subscriptionDao().insert(it.copy()) }
        }
    }
}
