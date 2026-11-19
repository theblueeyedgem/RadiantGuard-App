package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextRenewalTimestamp ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionItem>>

    @Query("SELECT * FROM subscriptions WHERE isAlarmActive = 1")
    suspend fun getActiveAlarmSubscriptions(): List<SubscriptionItem>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Int): SubscriptionItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(item: SubscriptionItem): Long

    @Update
    suspend fun updateSubscription(item: SubscriptionItem)

    @Delete
    suspend fun deleteSubscription(item: SubscriptionItem)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: Int)
}
