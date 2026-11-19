package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VaultItem::class, SubscriptionItem::class, NoteItem::class],
    version = 2,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun noteDao(): NoteDao
}
