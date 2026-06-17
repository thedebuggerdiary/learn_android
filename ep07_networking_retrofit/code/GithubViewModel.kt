package com.debuggerdiary.ep07

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface GithubUiState {
    data object Idle : GithubUiState
    data object Loading : GithubUiState
    data class Success(val repos: List<GithubRepo>) : GithubUiState
    data class Error(val message: String) : GithubUiState
}

class GithubViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GithubUiState>(GithubUiState.Idle)
    val uiState: StateFlow<GithubUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun searchRepos() {
        val username = _searchQuery.value.trim()
        if (username.isBlank()) return

        viewModelScope.launch {
            _uiState.value = GithubUiState.Loading
            try {
                val repos = RetrofitInstance.api.getRepos(username)
                _uiState.value = if (repos.isEmpty()) {
                    GithubUiState.Error("No public repositories found for '$username'")
                } else {
                    GithubUiState.Success(repos)
                }
            } catch (e: retrofit2.HttpException) {
                val message = when (e.code()) {
                    404  -> "User '$username' not found"
                    403  -> "API rate limit exceeded. Try again later."
                    else -> "Server error: ${e.code()}"
                }
                _uiState.value = GithubUiState.Error(message)
            } catch (e: IOException) {
                _uiState.value = GithubUiState.Error("Network error. Check your connection.")
            }
        }
    }
}
