package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.example.data.SubscriptionItem
import java.util.Calendar

object SubscriptionAlarmScheduler {
    const val CHANNEL_ID = "subscription_renewal_alarms"
    const val ACTION_TRIGGER_ALARM = "com.example.reminder.ACTION_SUBSCRIPTION_ALARM"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Subscription Renewal Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications and alerts for upcoming subscription renewals"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(alarmSound, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleAlarm(context: Context, item: SubscriptionItem) {
        if (!item.isAlarmActive) {
            cancelAlarm(context, item.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val triggerTime = computeTriggerTime(item)
        if (triggerTime <= System.currentTimeMillis()) {
            // If already in the past, don't schedule an old alarm
            return
        }

        val intent = Intent(context, SubscriptionAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra("subscription_id", item.id)
            putExtra("subscription_name", item.name)
            putExtra("subscription_price", item.price)
            putExtra("subscription_currency", item.currency)
            putExtra("subscription_cycle", item.billingCycle)
            putExtra("renewal_date", item.nextRenewalTimestamp)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("SubscriptionAlarm", "Scheduled alarm for ${item.name} at timestamp $triggerTime")
        } catch (e: SecurityException) {
            Log.e("SubscriptionAlarm", "Exact alarm permission restricted, falling back to setAndAllowWhileIdle: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (fallbackEx: Exception) {
                Log.e("SubscriptionAlarm", "Fallback alarm schedule error: ${fallbackEx.message}")
            }
        }
    }

    fun triggerTestNotification(context: Context, item: SubscriptionItem) {
        createNotificationChannel(context)
        val intent = Intent(context, SubscriptionAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra("subscription_id", item.id)
            putExtra("subscription_name", item.name)
            putExtra("subscription_price", item.price)
            putExtra("subscription_currency", item.currency)
            putExtra("subscription_cycle", item.billingCycle)
            putExtra("renewal_date", item.nextRenewalTimestamp)
            putExtra("is_test", true)
        }
        context.sendBroadcast(intent)
    }

    fun cancelAlarm(context: Context, subscriptionId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SubscriptionAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            subscriptionId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun computeTriggerTime(item: SubscriptionItem): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = item.nextRenewalTimestamp
        cal.add(Calendar.DAY_OF_YEAR, -item.reminderDaysBefore)
        cal.set(Calendar.HOUR_OF_DAY, item.reminderHour)
        cal.set(Calendar.MINUTE, item.reminderMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
