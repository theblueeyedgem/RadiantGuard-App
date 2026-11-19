package com.example.data

import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(private val subscriptionDao: SubscriptionDao) {
    val allSubscriptions: Flow<List<SubscriptionItem>> = subscriptionDao.getAllSubscriptions()

    suspend fun getById(id: Int): SubscriptionItem? = subscriptionDao.getSubscriptionById(id)

    suspend fun insert(item: SubscriptionItem): Long = subscriptionDao.insertSubscription(item)

    suspend fun update(item: SubscriptionItem) = subscriptionDao.updateSubscription(item)

    suspend fun delete(item: SubscriptionItem) = subscriptionDao.deleteSubscription(item)

    suspend fun deleteById(id: Int) = subscriptionDao.deleteById(id)
}
