package com.gestionale.tecnico.data.local.converter

import androidx.room.TypeConverter
import com.gestionale.tecnico.data.local.entity.*

class Converters {
    @TypeConverter fun fromTransactionType(v: TransactionType?): String? = v?.name
    @TypeConverter fun toTransactionType(v: String?): TransactionType? = v?.let { TransactionType.valueOf(it) }

    @TypeConverter fun fromTransactionCategory(v: TransactionCategory?): String? = v?.name
    @TypeConverter fun toTransactionCategory(v: String?): TransactionCategory? = v?.let { TransactionCategory.valueOf(it) }

    @TypeConverter fun fromPaymentStatus(v: PaymentStatus?): String? = v?.name
    @TypeConverter fun toPaymentStatus(v: String?): PaymentStatus? = v?.let { PaymentStatus.valueOf(it) }

    @TypeConverter fun fromExpiryType(v: ExpiryType?): String? = v?.name
    @TypeConverter fun toExpiryType(v: String?): ExpiryType? = v?.let { ExpiryType.valueOf(it) }

    @TypeConverter fun fromExpiryDuration(v: ExpiryDuration?): String? = v?.name
    @TypeConverter fun toExpiryDuration(v: String?): ExpiryDuration? = v?.let { ExpiryDuration.valueOf(it) }

    @TypeConverter fun fromSubscriptionFrequency(v: SubscriptionFrequency?): String? = v?.name
    @TypeConverter fun toSubscriptionFrequency(v: String?): SubscriptionFrequency? = v?.let { SubscriptionFrequency.valueOf(it) }
}
