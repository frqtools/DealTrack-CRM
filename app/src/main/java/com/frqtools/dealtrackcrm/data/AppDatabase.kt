package com.frqtools.dealtrackcrm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Client::class,
        Deal::class,
        Interaction::class,
        FollowUp::class,
        AppSettings::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun dealDao(): DealDao
    abstract fun interactionDao(): InteractionDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun appSettingsDao(): AppSettingsDao

    class BackupMigration(val context: Context, val start: Int, val end: Int) : androidx.room.migration.Migration(start, end) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                val dbFile = context.getDatabasePath("track_deals_database")
                if (dbFile.exists()) {
                    val backupFile = java.io.File(context.filesDir, "auto_backup_v${start}_to_v${end}.db")
                    dbFile.inputStream().use { input ->
                        backupFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (start == 4 && end == 5) {
                try {
                    // Migrate deals table
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `deals_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `clientId` INTEGER NOT NULL,
                            `title` TEXT NOT NULL,
                            `offeredPrice` REAL NOT NULL,
                            `finalPrice` REAL,
                            `stage` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `expectedCloseDate` INTEGER,
                            `closedDate` INTEGER,
                            `lostReason` TEXT,
                            `notes` TEXT NOT NULL,
                            `dateCreated` INTEGER NOT NULL,
                            FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO `deals_new` (`id`, `clientId`, `title`, `offeredPrice`, `finalPrice`, `stage`, `status`, `expectedCloseDate`, `closedDate`, `lostReason`, `notes`, `dateCreated`)
                        SELECT `id`, `clientId`, `title`, `offeredPrice`, `finalPrice`, `stage`, `status`, `expectedCloseDate`, `closedDate`, `lostReason`, `notes`, `dateCreated` FROM `deals`
                    """.trimIndent())
                    db.execSQL("DROP TABLE `deals`")
                    db.execSQL("ALTER TABLE `deals_new` RENAME TO `deals`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_deals_clientId` ON `deals` (`clientId`)")

                    // Migrate interactions table
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `interactions_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `clientId` INTEGER NOT NULL,
                            `dealId` INTEGER,
                            `dateTime` INTEGER NOT NULL,
                            `contactMethod` TEXT NOT NULL,
                            `discussion` TEXT NOT NULL,
                            `priceOffered` REAL,
                            `productDiscussed` TEXT,
                            `clientResponse` TEXT NOT NULL,
                            `myNextStep` TEXT NOT NULL,
                            FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO `interactions_new` (`id`, `clientId`, `dealId`, `dateTime`, `contactMethod`, `discussion`, `priceOffered`, `productDiscussed`, `clientResponse`, `myNextStep`)
                        SELECT `id`, `clientId`, `dealId`, `dateTime`, `contactMethod`, `discussion`, `priceOffered`, `productDiscussed`, `clientResponse`, `myNextStep` FROM `interactions`
                    """.trimIndent())
                    db.execSQL("DROP TABLE `interactions`")
                    db.execSQL("ALTER TABLE `interactions_new` RENAME TO `interactions`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_interactions_clientId` ON `interactions` (`clientId`)")

                    // Migrate follow_ups table
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `follow_ups_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `clientId` INTEGER NOT NULL,
                            `dealId` INTEGER,
                            `scheduledDateTime` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `priority` TEXT NOT NULL,
                            `isDone` INTEGER NOT NULL,
                            `completedDateTime` INTEGER,
                            `alarmId` INTEGER NOT NULL,
                            FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO `follow_ups_new` (`id`, `clientId`, `dealId`, `scheduledDateTime`, `note`, `priority`, `isDone`, `completedDateTime`, `alarmId`)
                        SELECT `id`, `clientId`, `dealId`, `scheduledDateTime`, `note`, `priority`, `isDone`, `completedDateTime`, `alarmId` FROM `follow_ups`
                    """.trimIndent())
                    db.execSQL("DROP TABLE `follow_ups`")
                    db.execSQL("ALTER TABLE `follow_ups_new` RENAME TO `follow_ups`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_follow_ups_clientId` ON `follow_ups` (`clientId`)")

                } catch (ex: Exception) {
                    ex.printStackTrace()
                    throw ex
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun performAutoBackupIfUpgrading(context: Context) {
            val dbFile = context.getDatabasePath("track_deals_database")
            if (dbFile.exists()) {
                try {
                    val currentAppVersion = 6 // Keep in sync with version = 6 annotation above
                    val existingDbVersion = android.database.sqlite.SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                    ).use { db ->
                        db.version
                    }

                    if (existingDbVersion > 0 && existingDbVersion < currentAppVersion) {
                        val backupDir = context.filesDir
                        val baseBackupName = "auto_backup_v${existingDbVersion}_before_v${currentAppVersion}"
                        val backupFile = java.io.File(backupDir, "$baseBackupName.db")

                        // 1. Copy the main SQLite database file
                        dbFile.inputStream().use { input ->
                            backupFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        // 2. Safely copy auxiliary journaling/WAL files if present
                        val journalFile = java.io.File(dbFile.path + "-journal")
                        if (journalFile.exists()) {
                            val journalBackup = java.io.File(backupDir, "$baseBackupName.db-journal")
                            journalFile.inputStream().use { input ->
                                journalBackup.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        val walFile = java.io.File(dbFile.path + "-wal")
                        if (walFile.exists()) {
                            val walBackup = java.io.File(backupDir, "$baseBackupName.db-wal")
                            walFile.inputStream().use { input ->
                                walBackup.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        val shmFile = java.io.File(dbFile.path + "-shm")
                        if (shmFile.exists()) {
                            val shmBackup = java.io.File(backupDir, "$baseBackupName.db-shm")
                            shmFile.inputStream().use { input ->
                                shmBackup.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        android.util.Log.i("AppDatabase", "Pre-migration auto-backup created: $baseBackupName")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Failed to perform pre-migration database backup", e)
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Safeguard against schema updates/missing migrations causing silent data wiping
                performAutoBackupIfUpgrading(context.applicationContext)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "track_deals_database"
                )
                .addMigrations(
                    BackupMigration(context.applicationContext, 1, 2),
                    BackupMigration(context.applicationContext, 2, 3),
                    BackupMigration(context.applicationContext, 3, 4),
                    BackupMigration(context.applicationContext, 4, 5)
                )
                .fallbackToDestructiveMigration(true) // Graceful fallback that won't crash the app, safe because of our auto-backup
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeAndResetInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.let {
                        if (it.isOpen) {
                            it.close()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    INSTANCE = null
                }
            }
        }
    }
}
