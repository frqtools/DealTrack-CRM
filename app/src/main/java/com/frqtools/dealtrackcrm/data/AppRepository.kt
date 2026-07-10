package com.frqtools.dealtrackcrm.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import androidx.room.withTransaction

class AppRepository(private val db: AppDatabase) {
    private val clientDao = db.clientDao()
    private val dealDao = db.dealDao()
    private val interactionDao = db.interactionDao()
    private val followUpDao = db.followUpDao()
    private val appSettingsDao = db.appSettingsDao()

    // --- Clients ---
    val allClients: Flow<List<Client>> = clientDao.getAllClients()
    fun getClientById(id: Int): Flow<Client?> = clientDao.getClientById(id)
    suspend fun getClientByIdDirect(id: Int): Client? = clientDao.getClientByIdDirect(id)
    suspend fun insertClient(client: Client): Long = clientDao.insertClient(client)
    suspend fun updateClient(client: Client) = clientDao.updateClient(client)
    suspend fun deleteClient(client: Client) {
        deleteClientCascade(client)
    }

    suspend fun getFollowUpsForClientDirect(clientId: Int): List<FollowUp> = followUpDao.getFollowUpsForClientDirect(clientId)

    suspend fun deleteClientCascade(client: Client) {
        db.withTransaction {
            dealDao.deleteDealsByClientId(client.id)
            interactionDao.deleteInteractionsByClientId(client.id)
            followUpDao.deleteFollowUpsByClientId(client.id)
            clientDao.deleteClient(client)
        }
    }

    // --- Deals ---
    val allDeals: Flow<List<Deal>> = dealDao.getAllDeals()
    fun getDealsForClient(clientId: Int): Flow<List<Deal>> = dealDao.getDealsForClient(clientId)
    fun getDealById(id: Int): Flow<Deal?> = dealDao.getDealById(id)
    suspend fun getDealByIdDirect(id: Int): Deal? = dealDao.getDealByIdDirect(id)
    suspend fun insertDeal(deal: Deal): Long = dealDao.insertDeal(deal)
    suspend fun updateDeal(deal: Deal) = dealDao.updateDeal(deal)
    suspend fun deleteDeal(deal: Deal) = dealDao.deleteDeal(deal)

    suspend fun getFollowUpsForDealDirect(dealId: Int): List<FollowUp> = followUpDao.getFollowUpsForDealDirect(dealId)

    suspend fun deleteDealCascade(deal: Deal) {
        db.withTransaction {
            interactionDao.deleteInteractionsByDealId(deal.id)
            followUpDao.deleteFollowUpsByDealId(deal.id)
            dealDao.deleteDeal(deal)
        }
    }

    // --- Interactions ---
    val allInteractions: Flow<List<Interaction>> = interactionDao.getAllInteractions()
    fun getInteractionsForClient(clientId: Int): Flow<List<Interaction>> = interactionDao.getInteractionsForClient(clientId)
    suspend fun insertInteraction(interaction: Interaction): Long = interactionDao.insertInteraction(interaction)
    suspend fun updateInteraction(interaction: Interaction) = interactionDao.updateInteraction(interaction)
    suspend fun deleteInteraction(interaction: Interaction) = interactionDao.deleteInteraction(interaction)

    // --- Follow-ups ---
    val allFollowUps: Flow<List<FollowUp>> = followUpDao.getAllFollowUps()
    fun getFollowUpsForClient(clientId: Int): Flow<List<FollowUp>> = followUpDao.getFollowUpsForClient(clientId)
    fun getFollowUpById(id: Int): Flow<FollowUp?> = followUpDao.getFollowUpById(id)
    suspend fun getFollowUpByIdDirect(id: Int): FollowUp? = followUpDao.getFollowUpByIdDirect(id)
    suspend fun insertFollowUp(followUp: FollowUp): Long = followUpDao.insertFollowUp(followUp)
    suspend fun updateFollowUp(followUp: FollowUp) = followUpDao.updateFollowUp(followUp)
    suspend fun deleteFollowUp(followUp: FollowUp) = followUpDao.deleteFollowUp(followUp)

    // --- Settings ---
    val appSettings: Flow<AppSettings> = flow {
        // Ensure settings exists
        var settings = appSettingsDao.getSettingsDirect()
        if (settings == null) {
            settings = AppSettings()
            appSettingsDao.insertOrUpdateSettings(settings)
        }
        appSettingsDao.getSettings().collect {
            if (it != null) {
                emit(it)
            } else {
                emit(AppSettings())
            }
        }
    }

    suspend fun getSettingsDirect(): AppSettings {
        var settings = appSettingsDao.getSettingsDirect()
        if (settings == null) {
            settings = AppSettings()
            appSettingsDao.insertOrUpdateSettings(settings)
        }
        return settings
    }

    suspend fun updateSettings(settings: AppSettings) {
        appSettingsDao.insertOrUpdateSettings(settings)
    }

    // --- General Reset ---
    suspend fun clearAllData(keepDemoSeeding: Boolean = false) {
        clientDao.deleteAllClients()
        dealDao.deleteAllDeals()
        interactionDao.deleteAllInteractions()
        followUpDao.deleteAllFollowUps()
        // Re-insert default settings with hasSeeded set based on user's choice.
        // If keepDemoSeeding is false, we mark hasSeeded = true to prevent re-seeding demo data.
        appSettingsDao.insertOrUpdateSettings(AppSettings(hasSeeded = !keepDemoSeeding))
    }
}
