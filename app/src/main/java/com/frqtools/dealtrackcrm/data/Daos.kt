package com.frqtools.dealtrackcrm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    fun getClientById(id: Int): Flow<Client?>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientByIdDirect(id: Int): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()
}

@Dao
interface DealDao {
    @Query("SELECT * FROM deals ORDER BY dateCreated DESC")
    fun getAllDeals(): Flow<List<Deal>>

    @Query("SELECT * FROM deals WHERE clientId = :clientId ORDER BY dateCreated DESC")
    fun getDealsForClient(clientId: Int): Flow<List<Deal>>

    @Query("SELECT * FROM deals WHERE id = :id LIMIT 1")
    fun getDealById(id: Int): Flow<Deal?>

    @Query("SELECT * FROM deals WHERE id = :id LIMIT 1")
    suspend fun getDealByIdDirect(id: Int): Deal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeal(deal: Deal): Long

    @Update
    suspend fun updateDeal(deal: Deal)

    @Delete
    suspend fun deleteDeal(deal: Deal)

    @Query("DELETE FROM deals")
    suspend fun deleteAllDeals()

    @Query("DELETE FROM deals WHERE clientId = :clientId")
    suspend fun deleteDealsByClientId(clientId: Int)
}

@Dao
interface InteractionDao {
    @Query("SELECT * FROM interactions ORDER BY dateTime DESC")
    fun getAllInteractions(): Flow<List<Interaction>>

    @Query("SELECT * FROM interactions WHERE clientId = :clientId ORDER BY dateTime DESC")
    fun getInteractionsForClient(clientId: Int): Flow<List<Interaction>>

    @Query("SELECT * FROM interactions WHERE id = :id LIMIT 1")
    fun getInteractionById(id: Int): Flow<Interaction?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: Interaction): Long

    @Update
    suspend fun updateInteraction(interaction: Interaction)

    @Delete
    suspend fun deleteInteraction(interaction: Interaction)

    @Query("DELETE FROM interactions")
    suspend fun deleteAllInteractions()

    @Query("DELETE FROM interactions WHERE clientId = :clientId")
    suspend fun deleteInteractionsByClientId(clientId: Int)
}

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_ups ORDER BY scheduledDateTime ASC")
    fun getAllFollowUps(): Flow<List<FollowUp>>

    @Query("SELECT * FROM follow_ups WHERE clientId = :clientId ORDER BY scheduledDateTime ASC")
    fun getFollowUpsForClient(clientId: Int): Flow<List<FollowUp>>

    @Query("SELECT * FROM follow_ups WHERE id = :id LIMIT 1")
    fun getFollowUpById(id: Int): Flow<FollowUp?>

    @Query("SELECT * FROM follow_ups WHERE id = :id LIMIT 1")
    suspend fun getFollowUpByIdDirect(id: Int): FollowUp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUp): Long

    @Update
    suspend fun updateFollowUp(followUp: FollowUp)

    @Delete
    suspend fun deleteFollowUp(followUp: FollowUp)

    @Query("DELETE FROM follow_ups")
    suspend fun deleteAllFollowUps()

    @Query("DELETE FROM follow_ups WHERE clientId = :clientId")
    suspend fun deleteFollowUpsByClientId(clientId: Int)

    @Query("SELECT * FROM follow_ups WHERE clientId = :clientId")
    suspend fun getFollowUpsForClientDirect(clientId: Int): List<FollowUp>
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettings)
}
