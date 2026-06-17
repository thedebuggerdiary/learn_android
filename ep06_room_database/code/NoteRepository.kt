package com.debuggerdiary.ep06

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {

    val allNotes: Flow<List<Note>> = dao.getAllNotes()

    suspend fun addNote(title: String) {
        dao.insert(Note(title = title))
    }

    suspend fun updateNote(note: Note) {
        dao.update(note)
    }

    suspend fun deleteNote(note: Note) {
        dao.delete(note)
    }

    suspend fun deleteNoteById(id: Long) {
        dao.deleteById(id)
    }
}
