package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<NoteItem>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<NoteItem>> = noteDao.searchNotes(query)

    suspend fun getById(id: Int): NoteItem? = noteDao.getNoteById(id)

    suspend fun insert(item: NoteItem): Long = noteDao.insertNote(item)

    suspend fun update(item: NoteItem) = noteDao.updateNote(item)

    suspend fun delete(item: NoteItem) = noteDao.deleteNote(item)

    suspend fun deleteById(id: Int) = noteDao.deleteNoteById(id)
}
