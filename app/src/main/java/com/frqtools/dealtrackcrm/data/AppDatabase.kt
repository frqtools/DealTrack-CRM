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
    version = 4,
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
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "track_deals_database"
                )
                .addMigrations(
                    BackupMigration(context.applicationContext, 1, 2),
                    BackupMigration(context.applicationContext, 2, 3),
                    BackupMigration(context.applicationContext, 3, 4)
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeAndResetInstance() {
            synchronized(this) {
                INSTANCE?.let {
                    if (it.isOpen) {
                        it.close()
                    }
                }
                INSTANCE = null
            }
        }
    }
}
