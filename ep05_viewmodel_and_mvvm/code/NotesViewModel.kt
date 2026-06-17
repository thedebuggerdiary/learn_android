package com.debuggerdiary.ep05

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class NotesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun addNote(title: String) {
        if (title.isBlank()) return
        val newNote = Note(
            id = System.currentTimeMillis(),
            title = title.trim()
        )
        _uiState.update { it.copy(notes = it.notes + newNote) }
    }

    fun deleteNote(id: Long) {
        _uiState.update { it.copy(notes = it.notes.filter { note -> note.id != id }) }
    }

    fun simulateLoading() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            kotlinx.coroutines.delay(1500)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    notes = listOf(
                        Note(1L, "Loaded from network"),
                        Note(2L, "Another remote note")
                    )
                )
            }
        }
    }
}
