package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.NoteRepository
import com.example.data.SettingsStore
import com.example.data.SubscriptionRepository
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import com.example.reminder.SubscriptionAlarmScheduler

class RadiantGuardApplication : Application() {

    lateinit var database: VaultDatabase
        private set

    lateinit var vaultRepository: VaultRepository
        private set

    lateinit var subscriptionRepository: SubscriptionRepository
        private set

    lateinit var noteRepository: NoteRepository
        private set

    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = Room.databaseBuilder(
            applicationContext,
            VaultDatabase::class.java,
            "radiant_guard_db"
        ).addMigrations(VaultDatabase.MIGRATION_2_3).build()
        
        vaultRepository = VaultRepository(database.vaultDao())
        subscriptionRepository = SubscriptionRepository(database.subscriptionDao())
        noteRepository = NoteRepository(database.noteDao())
        
        settingsStore = SettingsStore(applicationContext)

        SubscriptionAlarmScheduler.createNotificationChannel(applicationContext)
    }
}
