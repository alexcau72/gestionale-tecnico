package com.gestionale.tecnico.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gestionale.tecnico.data.local.converter.Converters
import com.gestionale.tecnico.data.local.dao.ClientDao
import com.gestionale.tecnico.data.local.dao.ExpiryDao
import com.gestionale.tecnico.data.local.dao.SubscriptionDao
import com.gestionale.tecnico.data.local.dao.TransactionDao
import com.gestionale.tecnico.data.local.entity.Client
import com.gestionale.tecnico.data.local.entity.ExpiryItem
import com.gestionale.tecnico.data.local.entity.SubscriptionContract
import com.gestionale.tecnico.data.local.entity.Transaction

@Database(
    entities = [Client::class, Transaction::class, ExpiryItem::class, SubscriptionContract::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun transactionDao(): TransactionDao
    abstract fun expiryDao(): ExpiryDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gestionale_tecnico.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
