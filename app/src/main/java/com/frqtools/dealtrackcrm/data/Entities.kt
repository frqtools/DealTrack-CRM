package com.frqtools.dealtrackcrm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val companyName: String = "",
    val city: String = "",
    val clientType: String = "Individual",
    val source: String = "",
    val notes: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "deals")
data class Deal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val title: String,
    val offeredPrice: Double = 0.0,
    val finalPrice: Double? = null,
    val stage: String = "Prospect",
    val status: String = "Open", // "Open", "Won", "Lost", "On Hold"
    val expectedCloseDate: Long? = null,
    val closedDate: Long? = null,
    val lostReason: String? = null,
    val notes: String = "",
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "interactions")
data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val dealId: Int? = null,
    val dateTime: Long = System.currentTimeMillis(),
    val contactMethod: String = "Call", // "Call", "WhatsApp", "Meeting", "Email", "Other"
    val discussion: String,
    val priceOffered: Double? = null,
    val productDiscussed: String? = null,
    val clientResponse: String = "Interested", // "Interested", "Not Interested", "Thinking", "Follow Later", "Closed"
    val myNextStep: String = ""
)

@Entity(tableName = "follow_ups")
data class FollowUp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val dealId: Int? = null,
    val scheduledDateTime: Long,
    val note: String,
    val priority: String = "Normal", // "Normal", "Important", "Urgent"
    val isDone: Boolean = false,
    val completedDateTime: Long? = null,
    val alarmId: Int = 0
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "My Business",
    val ownerName: String = "Owner",
    val currency: String = "PKR",
    val snoozeMinutes: Int = 30,
    val notificationsEnabled: Boolean = true,
    val customStages: String = "Prospect,Negotiation,Proposal Sent,Won,Lost,On Hold",
    val customClientTypes: String = "Individual,Retailer,Wholesaler,Corporate,Other",
    val customStatuses: String = "Open,Won,Lost,On Hold",
    val hasSeeded: Boolean = false
)
