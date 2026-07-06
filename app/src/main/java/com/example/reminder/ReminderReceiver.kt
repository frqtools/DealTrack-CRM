package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.FollowUp
import com.example.data.Interaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TRIGGER = "com.example.reminder.ACTION_TRIGGER"
        const val ACTION_DONE = "com.example.reminder.ACTION_DONE"
        const val ACTION_SNOOZE = "com.example.reminder.ACTION_SNOOZE"
        const val CHANNEL_ID = "followup_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val db = AppDatabase.getDatabase(context)
        val repository = AppRepository(db)
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            coroutineScope.launch {
                val followUps = repository.allFollowUps.firstOrNull() ?: emptyList()
                val now = System.currentTimeMillis()
                for (f in followUps) {
                    if (!f.isDone && f.scheduledDateTime > now) {
                        val client = repository.getClientByIdDirect(f.clientId)
                        val clientName = client?.name ?: "Client"
                        ReminderScheduler.schedule(context, f, clientName, client?.phone)
                    }
                }
            }
            return
        }

        val followUpId = intent.getIntExtra(ReminderScheduler.EXTRA_FOLLOWUP_ID, -1)
        if (followUpId == -1) return

        when (action) {
            ACTION_TRIGGER -> {
                val clientName = intent.getStringExtra(ReminderScheduler.EXTRA_CLIENT_NAME) ?: "Client"
                val note = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE) ?: "Follow-up reminder"
                val clientPhone = intent.getStringExtra(ReminderScheduler.EXTRA_CLIENT_PHONE)
                showNotification(context, followUpId, clientName, note, clientPhone)
            }
            ACTION_DONE -> {
                coroutineScope.launch {
                    val followUp = repository.getFollowUpByIdDirect(followUpId)
                    if (followUp != null) {
                        val updated = followUp.copy(
                            isDone = true,
                            completedDateTime = System.currentTimeMillis()
                        )
                        repository.updateFollowUp(updated)

                        // Log as interaction
                        repository.insertInteraction(
                            Interaction(
                                clientId = followUp.clientId,
                                dealId = followUp.dealId,
                                dateTime = System.currentTimeMillis(),
                                contactMethod = "Call",
                                discussion = "Completed follow-up: ${followUp.note}",
                                clientResponse = "Closed",
                                myNextStep = ""
                            )
                        )
                    }
                }
                cancelNotification(context, followUpId)
            }
            ACTION_SNOOZE -> {
                coroutineScope.launch {
                    val followUp = repository.getFollowUpByIdDirect(followUpId)
                    val settings = repository.getSettingsDirect()
                    if (followUp != null) {
                        val snoozeMillis = settings.snoozeMinutes * 60 * 1000L
                        val newTime = System.currentTimeMillis() + snoozeMillis
                        val updated = followUp.copy(scheduledDateTime = newTime)
                        repository.updateFollowUp(updated)

                        val client = repository.getClientByIdDirect(followUp.clientId)
                        val clientName = client?.name ?: "Client"
                        ReminderScheduler.schedule(context, updated, clientName, client?.phone)
                    }
                }
                cancelNotification(context, followUpId)
            }
        }
    }

    private fun showNotification(context: Context, followUpId: Int, clientName: String, note: String, clientPhone: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Follow-Up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for scheduled follow-up calls and meetings"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPI = PendingIntent.getActivity(
            context,
            followUpId * 10 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra(ReminderScheduler.EXTRA_FOLLOWUP_ID, followUpId)
        }
        val donePI = PendingIntent.getBroadcast(
            context,
            followUpId * 10 + 2,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(ReminderScheduler.EXTRA_FOLLOWUP_ID, followUpId)
        }
        val snoozePI = PendingIntent.getBroadcast(
            context,
            followUpId * 10 + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(clientName)
            .setContentText(note)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPI)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePI)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePI)

        if (!clientPhone.isNullOrBlank()) {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clientPhone"))
            val callPI = PendingIntent.getActivity(
                context,
                followUpId * 10 + 4,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.sym_action_call, "Call", callPI)
        }

        notificationManager.notify(followUpId, builder.build())
    }

    private fun cancelNotification(context: Context, followUpId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(followUpId)
    }
}
