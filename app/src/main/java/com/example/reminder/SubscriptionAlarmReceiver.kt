package com.example.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subscriptionId = intent.getIntExtra("subscription_id", 0)
        val name = intent.getStringExtra("subscription_name") ?: "Subscription"
        val price = intent.getDoubleExtra("subscription_price", 0.0)
        val currency = intent.getStringExtra("subscription_currency") ?: "$"
        val cycle = intent.getStringExtra("subscription_cycle") ?: "Monthly"
        val renewalDateMs = intent.getLongExtra("renewal_date", System.currentTimeMillis())
        val isTest = intent.getBooleanExtra("is_test", false)

        SubscriptionAlarmScheduler.createNotificationChannel(context)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(renewalDateMs))

        val title = if (isTest) {
            "🔔 Radiant Guard Alert: $name"
        } else {
            "⚠️ Renewal Reminder: $name"
        }

        val contentText = if (isTest) {
            "Test Alarm: $currency${String.format(Locale.US, "%.2f", price)} ($cycle) renewing on $formattedDate"
        } else {
            "$currency${String.format(Locale.US, "%.2f", price)} is due for renewal on $formattedDate"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            subscriptionId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, SubscriptionAlarmScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_radiant_guard)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\n\nRadiant Guard has sent this reminder so you can cancel or review your payment ahead of time."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(subscriptionId + 10000, notification)
    }
}
