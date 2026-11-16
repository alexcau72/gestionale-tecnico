package com.gestionale.tecnico

import android.app.Application
import com.gestionale.tecnico.data.local.database.AppDatabase
import com.gestionale.tecnico.data.repository.AppRepository

class GestionaleApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: AppRepository by lazy {
        AppRepository(database.clientDao(), database.transactionDao(), database.expiryDao(), database.subscriptionDao())
    }
}
