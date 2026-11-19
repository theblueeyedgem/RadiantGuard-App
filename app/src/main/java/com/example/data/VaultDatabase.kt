package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VaultItem::class, SubscriptionItem::class, NoteItem::class],
    version = 3,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun noteDao(): NoteDao

    companion object {
        /**
         * Real migration 2 -> 3. Adds pin/lock columns to vault items and notes,
         * a pin column to subscriptions, and the note background-style column.
         * All columns have defaults, so existing rows survive the upgrade.
         * (Previously the app used fallbackToDestructiveMigration, which would
         * have wiped every vault item, subscription and note on this upgrade.)
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_items ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN lockPasswordHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN lockPasswordHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN backgroundStyle TEXT NOT NULL DEFAULT 'solid'")
            }
        }
    }
}
