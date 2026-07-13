package com.frqtools.dealtrackcrm.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.frqtools.dealtrackcrm.data.*
import com.frqtools.dealtrackcrm.reminder.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val context: Context, private val repository: AppRepository) : ViewModel() {

    // --- State Streams ---
    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deals: StateFlow<List<Deal>> = repository.allDeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val interactions: StateFlow<List<Interaction>> = repository.allInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followUps: StateFlow<List<FollowUp>> = repository.allFollowUps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // --- Pending Notification Redirect ---
    private val _pendingNotificationRoute = MutableStateFlow<String?>(null)
    val pendingNotificationRoute: StateFlow<String?> = _pendingNotificationRoute.asStateFlow()

    private val _fcmToken = MutableStateFlow<String>("Loading FCM Token...")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    fun setPendingNotificationRoute(route: String?) {
        _pendingNotificationRoute.value = route
    }

    fun clearPendingNotificationRoute() {
        _pendingNotificationRoute.value = null
    }

    fun refreshFcmToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _fcmToken.value = task.result ?: "Token is empty"
                    Log.d("DealTrackFCM", "Retrieved FCM Token: ${task.result}")
                } else {
                    _fcmToken.value = "Failed to fetch FCM Token: ${task.exception?.localizedMessage}"
                    Log.e("DealTrackFCM", "FCM Token retrieval failed", task.exception)
                }
            }
        } catch (e: Exception) {
            _fcmToken.value = "Firebase not initialized: ${e.localizedMessage}"
            Log.e("DealTrackFCM", "Firebase messaging initialization exception", e)
        }
    }

    init {
        refreshFcmToken()
        viewModelScope.launch {
            val currentSettings = repository.getSettingsDirect()
            val sharedPrefs = context.getSharedPreferences("crm_app_preferences", Context.MODE_PRIVATE)
            val hasInitSeeded = sharedPrefs.getBoolean("has_initially_seeded", false)

            if (!hasInitSeeded) {
                val currentClients = repository.allClients.first()
                if (currentClients.isEmpty()) {
                    seedSampleData()
                }
                sharedPrefs.edit().putBoolean("has_initially_seeded", true).apply()
                repository.updateSettings(currentSettings.copy(hasSeeded = true))
            }
        }
    }

    // --- Actions ---
    
    // Clients
    fun saveClient(client: Client, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val sanitizedClient = client.copy(phone = client.phone.replace(" ", ""))
            val id = if (sanitizedClient.id == 0) {
                repository.insertClient(sanitizedClient)
            } else {
                repository.updateClient(sanitizedClient)
                sanitizedClient.id.toLong()
            }
            onComplete(id)
        }
    }

    fun deleteClient(context: Context, client: Client, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val followUps = repository.getFollowUpsForClientDirect(client.id)
            followUps.forEach { followUp ->
                ReminderScheduler.cancel(context, followUp.id)
            }
            repository.deleteClientCascade(client)
            onComplete()
        }
    }

    // Deals
    fun saveDeal(deal: Deal, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (deal.id == 0) {
                repository.insertDeal(deal)
            } else {
                repository.updateDeal(deal)
            }
            onComplete()
        }
    }

    fun deleteDeal(context: Context, deal: Deal, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val followUps = repository.getFollowUpsForDealDirect(deal.id)
                followUps.forEach { f ->
                    ReminderScheduler.cancel(context, f.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteDealCascade(deal)
            onComplete()
        }
    }

    // Interactions
    fun saveInteraction(interaction: Interaction, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (interaction.id == 0) {
                repository.insertInteraction(interaction)
            } else {
                repository.updateInteraction(interaction)
            }
            onComplete()
        }
    }

    fun deleteInteraction(interaction: Interaction, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteInteraction(interaction)
            onComplete()
        }
    }

    // Follow-ups
    fun saveFollowUp(context: Context, followUp: FollowUp, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val id = if (followUp.id == 0) {
                repository.insertFollowUp(followUp).toInt()
            } else {
                repository.updateFollowUp(followUp)
                followUp.id
            }

            val client = repository.getClientByIdDirect(followUp.clientId)
            val clientName = client?.name ?: "Client"
            val savedFollowUp = followUp.copy(id = id)

            if (!followUp.isDone) {
                ReminderScheduler.schedule(context, savedFollowUp, clientName, client?.phone)
            } else {
                ReminderScheduler.cancel(context, id)
            }
            onComplete()
        }
    }

    fun deleteFollowUp(context: Context, followUp: FollowUp, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            ReminderScheduler.cancel(context, followUp.id)
            repository.deleteFollowUp(followUp)
            onComplete()
        }
    }

    fun markFollowUpDone(context: Context, followUp: FollowUp, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val updated = followUp.copy(
                isDone = true,
                completedDateTime = System.currentTimeMillis()
            )
            repository.updateFollowUp(updated)
            ReminderScheduler.cancel(context, followUp.id)

            // Log an interaction automatically
            val client = repository.getClientByIdDirect(followUp.clientId)
            val clientName = client?.name ?: "Client"
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
            onComplete()
        }
    }

    fun snoozeFollowUp(context: Context, followUp: FollowUp, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val appSettings = repository.getSettingsDirect()
            val snoozeMillis = appSettings.snoozeMinutes * 60 * 1000L
            val newTime = System.currentTimeMillis() + snoozeMillis
            val updated = followUp.copy(scheduledDateTime = newTime)
            repository.updateFollowUp(updated)

            val client = repository.getClientByIdDirect(followUp.clientId)
            val clientName = client?.name ?: "Client"
            ReminderScheduler.schedule(context, updated, clientName, client?.phone)
            onComplete()
        }
    }

    // Settings
    fun updateSettings(updated: AppSettings, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateSettings(updated)
            onComplete()
        }
    }

    // Reset
    fun clearAllData(keepDemoSeeding: Boolean = false, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllData(keepDemoSeeding)
            if (keepDemoSeeding) {
                seedSampleData()
            }
            onComplete()
        }
    }

    // Seeding sample data
    private suspend fun seedSampleData() {
        val client1Id = repository.insertClient(
            Client(
                name = "Ahmed Ali",
                phone = "+923001234567",
                companyName = "Siddique Fabrics",
                city = "Faisalabad",
                clientType = "Wholesaler",
                source = "Referral",
                notes = "Large buyer of cotton threads. Looking for credit terms of 30 days."
            )
        ).toInt()

        val client2Id = repository.insertClient(
            Client(
                name = "Fatima Shah",
                phone = "+923217654321",
                companyName = "Elegant Boutique",
                city = "Lahore",
                clientType = "Retailer",
                source = "WhatsApp",
                notes = "Inquired about premium lawn prints. Very quality conscious."
            )
        ).toInt()

        val client3Id = repository.insertClient(
            Client(
                name = "Kamran Khan",
                phone = "+923339876543",
                companyName = "Khyber Traders",
                city = "Peshawar",
                clientType = "Individual",
                source = "Walk-in",
                notes = "Interested in personal electronic items. Cash payment only."
            )
        ).toInt()

        // Insert some Deals
        val deal1Id = repository.insertDeal(
            Deal(
                clientId = client1Id,
                title = "200 cartons cotton yarn",
                offeredPrice = 45000.0,
                stage = "Negotiation",
                status = "Open",
                expectedCloseDate = System.currentTimeMillis() + 86400000 * 5
            )
        ).toInt()

        val deal2Id = repository.insertDeal(
            Deal(
                clientId = client2Id,
                title = "Bulk Summer Lawn Collection",
                offeredPrice = 125000.0,
                stage = "Proposal Sent",
                status = "Open",
                expectedCloseDate = System.currentTimeMillis() + 86400000 * 3
            )
        ).toInt()

        val deal3Id = repository.insertDeal(
            Deal(
                clientId = client3Id,
                title = "S24 Ultra Purchase",
                offeredPrice = 320000.0,
                finalPrice = 315000.0,
                stage = "Won",
                status = "Won",
                closedDate = System.currentTimeMillis() - 86400000 * 2
            )
        ).toInt()

        // Insert some Interactions
        repository.insertInteraction(
            Interaction(
                clientId = client1Id,
                dealId = deal1Id,
                contactMethod = "Meeting",
                discussion = "Met at Faisalabad office. Quoted PKR 45,000 for yarn. Ahmed requested a 5% bulk discount.",
                priceOffered = 45000.0,
                productDiscussed = "Cotton Yarn Grade-A",
                clientResponse = "Thinking",
                myNextStep = "Discuss discount with factory manager."
            )
        )

        repository.insertInteraction(
            Interaction(
                clientId = client2Id,
                dealId = deal2Id,
                contactMethod = "WhatsApp",
                discussion = "Sent catalog on WhatsApp. Client liked the pastel colors but wants confirmation on fabric thickness.",
                priceOffered = 125000.0,
                productDiscussed = "Premium Lawn Prints",
                clientResponse = "Interested",
                myNextStep = "Send a physical fabric sample to her boutique."
            )
        )

        repository.insertInteraction(
            Interaction(
                clientId = client3Id,
                dealId = deal3Id,
                contactMethod = "Call",
                discussion = "Called Kamran to confirm delivery address. He requested quick delivery before Friday.",
                priceOffered = 320000.0,
                productDiscussed = "Galaxy S24 Ultra 512GB",
                clientResponse = "Closed",
                myNextStep = "Hand over to dispatch team."
            )
        )

        // Insert some Follow-Ups
        repository.insertFollowUp(
            FollowUp(
                clientId = client1Id,
                dealId = deal1Id,
                scheduledDateTime = System.currentTimeMillis() + 86400000, // tomorrow
                note = "Call Ahmed to offer updated discounted rate of PKR 43,500.",
                priority = "Important"
            )
        )

        repository.insertFollowUp(
            FollowUp(
                clientId = client2Id,
                dealId = deal2Id,
                scheduledDateTime = System.currentTimeMillis() + 86400000 * 2, // day after tomorrow
                note = "Follow up if fabric sample reached her boutique.",
                priority = "Normal"
            )
        )

        repository.insertFollowUp(
            FollowUp(
                clientId = client1Id,
                dealId = deal1Id,
                scheduledDateTime = System.currentTimeMillis() - 3600000, // 1 hour ago (overdue!)
                note = "Send credit terms proposal details via email.",
                priority = "Urgent",
                isDone = false
            )
        )
    }
}

class ViewModelFactory(private val context: Context, private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
