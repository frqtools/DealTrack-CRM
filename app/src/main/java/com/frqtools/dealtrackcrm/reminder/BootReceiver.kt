package com.frqtools.dealtrackcrm.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.frqtools.dealtrackcrm.data.AppDatabase
import com.frqtools.dealtrackcrm.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val repository = AppRepository(context)
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            try {
                val followUps = repository.allFollowUps.firstOrNull() ?: emptyList()
                val now = System.currentTimeMillis()
                for (f in followUps) {
                    if (!f.isDone && f.scheduledDateTime > now) {
                        val client = repository.getClientByIdDirect(f.clientId)
                        val clientName = client?.name ?: "Client"
                        ReminderScheduler.schedule(context, f, clientName, client?.phone)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
