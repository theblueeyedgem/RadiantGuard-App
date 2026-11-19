package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SubscriptionItem
import com.example.data.SubscriptionRepository
import com.example.reminder.SubscriptionAlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class SubscriptionSummary(
    val totalMonthlySpend: Double = 0.0,
    val activeCount: Int = 0,
    val upcomingCount: Int = 0
)

class SubscriptionViewModel(
    application: Application,
    private val repository: SubscriptionRepository
) : AndroidViewModel(application) {

    val subscriptions: StateFlow<List<SubscriptionItem>> = repository.allSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val summary: StateFlow<SubscriptionSummary> = subscriptions.map { list ->
        var monthlyTotal = 0.0
        val now = System.currentTimeMillis()
        val sevenDaysAhead = now + (7L * 24 * 60 * 60 * 1000L)
        var upcoming = 0

        list.forEach { item ->
            val normalizedMonthly = when (item.billingCycle.lowercase()) {
                "yearly" -> item.price / 12.0
                "weekly" -> item.price * 4.33
                "quarterly" -> item.price / 3.0
                else -> item.price
            }
            monthlyTotal += normalizedMonthly

            if (item.nextRenewalTimestamp in now..sevenDaysAhead) {
                upcoming++
            }
        }

        SubscriptionSummary(
            totalMonthlySpend = monthlyTotal,
            activeCount = list.size,
            upcomingCount = upcoming
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionSummary()
    )

    fun addOrUpdateSubscription(item: SubscriptionItem) {
        viewModelScope.launch {
            val id = if (item.id == 0) {
                repository.insert(item).toInt()
            } else {
                repository.update(item)
                item.id
            }

            val updatedItem = item.copy(id = id)
            if (updatedItem.isAlarmActive) {
                SubscriptionAlarmScheduler.scheduleAlarm(getApplication(), updatedItem)
            } else {
                SubscriptionAlarmScheduler.cancelAlarm(getApplication(), updatedItem.id)
            }
        }
    }

    fun deleteSubscription(item: SubscriptionItem) {
        viewModelScope.launch {
            SubscriptionAlarmScheduler.cancelAlarm(getApplication(), item.id)
            repository.delete(item)
        }
    }

    fun toggleAlarm(item: SubscriptionItem) {
        viewModelScope.launch {
            val updated = item.copy(isAlarmActive = !item.isAlarmActive)
            repository.update(updated)
            if (updated.isAlarmActive) {
                SubscriptionAlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                SubscriptionAlarmScheduler.cancelAlarm(getApplication(), updated.id)
            }
        }
    }

    fun triggerTestAlarm(item: SubscriptionItem) {
        SubscriptionAlarmScheduler.triggerTestNotification(getApplication(), item)
    }
}
