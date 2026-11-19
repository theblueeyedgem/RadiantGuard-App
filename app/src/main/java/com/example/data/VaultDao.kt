package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY isPinned DESC, createdAt DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getAllItemsOnce(): List<VaultItem>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getItemById(id: Int): VaultItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItem)

    @Update
    suspend fun updateItem(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)
}
