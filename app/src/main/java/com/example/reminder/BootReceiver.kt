package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.RadiantGuardApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val app = context.applicationContext as? RadiantGuardApplication ?: return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeSubs = app.database.subscriptionDao().getActiveAlarmSubscriptions()
                    activeSubs.forEach { sub ->
                        SubscriptionAlarmScheduler.scheduleAlarm(context, sub)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
