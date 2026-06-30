package com.example.weatherintelligence.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherintelligence.data.repository.WeatherRepository
import com.example.weatherintelligence.domain.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        WeatherUiState(query = WeatherRepository.DEFAULT_CITY),
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeCity(WeatherRepository.DEFAULT_CITY)
        refresh(force = false)
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        observeCity(query)
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        val query = _uiState.value.query.trim().ifBlank { WeatherRepository.DEFAULT_CITY }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val snapshot = repository.refresh(query, force)
                _uiState.update {
                    it.copy(
                        query = query,
                        weather = snapshot,
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = error.userMessage(it.weather),
                    )
                }
            }
        }
    }

    private fun observeCity(query: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeWeather(query).collect { snapshot ->
                if (snapshot != null) {
                    _uiState.update {
                        it.copy(
                            weather = snapshot,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    private fun Exception.userMessage(cached: WeatherSnapshot?): String {
        val baseMessage = message ?: "Unable to refresh weather right now."
        return if (cached != null) {
            "Refresh failed. Showing cached data. $baseMessage"
        } else {
            baseMessage
        }
    }

    companion object {
        fun factory(repository: WeatherRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WeatherViewModel(repository) as T
                }
            }
    }
}

data class WeatherUiState(
    val query: String,
    val weather: WeatherSnapshot? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
