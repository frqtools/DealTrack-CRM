package com.frqtools.dealtrackcrm.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.frqtools.dealtrackcrm.data.FollowUp

object ReminderScheduler {
    const val EXTRA_FOLLOWUP_ID = "followup_id"
    const val EXTRA_CLIENT_NAME = "client_name"
    const val EXTRA_NOTE = "note"
    const val EXTRA_CLIENT_PHONE = "client_phone"
    const val EXTRA_BUTTON_TYPE = "notification_button_type"

    fun schedule(context: Context, followUp: FollowUp, clientName: String, clientPhone: String? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER
            putExtra(EXTRA_FOLLOWUP_ID, followUp.id)
            putExtra(EXTRA_CLIENT_NAME, clientName)
            putExtra(EXTRA_NOTE, followUp.note)
            putExtra(EXTRA_CLIENT_PHONE, clientPhone)
            putExtra(EXTRA_BUTTON_TYPE, followUp.notificationButtonType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            followUp.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = followUp.scheduledDateTime
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule exact alarm, falling back", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, followUpId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            followUpId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
