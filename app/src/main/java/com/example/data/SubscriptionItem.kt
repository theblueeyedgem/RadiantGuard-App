package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // Entertainment, Cloud Storage, Productivity, Music, Utilities, Other
    val price: Double,
    /** ISO 4217 currency code, e.g. "PKR", "USD", "INR". Legacy "$" values are mapped on read. */
    val currency: String = "PKR",
    val billingCycle: String = "Monthly", // Monthly, Yearly, Weekly, Quarterly
    val nextRenewalTimestamp: Long,
    val reminderDaysBefore: Int = 1, // 0 = same day, 1 = 1 day before, 2, 3, 7
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val notes: String = "",
    val colorHex: String = "#8E4C1E",
    val isAlarmActive: Boolean = true,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
