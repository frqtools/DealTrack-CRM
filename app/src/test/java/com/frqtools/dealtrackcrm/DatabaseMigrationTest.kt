package com.frqtools.dealtrackcrm

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.frqtools.dealtrackcrm.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        // 1. Create database in version 5 state
        var db = helper.createDatabase(TEST_DB, 5)

        // 2. Insert test data compatible with version 5 schema
        // Note: version 5 has clients, deals, interactions, and follow_ups.
        // It does not have notificationButtonType in follow_ups.
        db.execSQL("""
            INSERT INTO `clients` (id, name, phone, companyName, city, clientType, source, notes, dateAdded, isActive)
            VALUES (1, 'Jane Doe', '+923001112223', 'JD Enterprises', 'Karachi', 'Individual', 'Direct', 'Test Notes', 12345678, 1)
        """)
        
        db.execSQL("""
            INSERT INTO `deals` (id, clientId, title, offeredPrice, finalPrice, stage, status, expectedCloseDate, closedDate, lostReason, notes, dateCreated)
            VALUES (10, 1, 'Inbound Deal', 150000.0, null, 'Prospect', 'Open', null, null, null, 'Initial meeting done', 12345678)
        """)
        
        db.execSQL("""
            INSERT INTO `follow_ups` (id, clientId, dealId, scheduledDateTime, note, priority, isDone, completedDateTime, alarmId)
            VALUES (100, 1, 10, 12345678, 'Follow up tomorrow', 'Important', 0, null, 101)
        """)

        db.close()

        // 3. Open database and run migration 5 -> 6
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            AppDatabase.BackupMigration(InstrumentationRegistry.getInstrumentation().targetContext, 5, 6)
        )

        // 4. Validate that the client, deal, and follow-up data successfully persisted
        val clientCursor = db.query("SELECT * FROM clients WHERE id = 1")
        assert(clientCursor.moveToFirst())
        assertEquals("Jane Doe", clientCursor.getString(clientCursor.getColumnIndexOrThrow("name")))
        assertEquals("+923001112223", clientCursor.getString(clientCursor.getColumnIndexOrThrow("phone")))
        clientCursor.close()

        val dealCursor = db.query("SELECT * FROM deals WHERE id = 10")
        assert(dealCursor.moveToFirst())
        assertEquals("Inbound Deal", dealCursor.getString(dealCursor.getColumnIndexOrThrow("title")))
        dealCursor.close()

        // 5. Validate that notificationButtonType was added and got the default value 'Call'
        val followUpCursor = db.query("SELECT * FROM follow_ups WHERE id = 100")
        assert(followUpCursor.moveToFirst())
        assertEquals("Follow up tomorrow", followUpCursor.getString(followUpCursor.getColumnIndexOrThrow("note")))
        assertEquals("Call", followUpCursor.getString(followUpCursor.getColumnIndexOrThrow("notificationButtonType")))
        followUpCursor.close()
    }
}
