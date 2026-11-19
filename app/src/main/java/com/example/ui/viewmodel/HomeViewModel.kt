package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KnightScopeRepository
import com.example.data.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class HomeUiState(
    val userProfile: UserProfile = UserProfile(),
    val puzzleRating: Int = 1200,
    val openingStreak: Int = 0,
    val completedLessonsCount: Int = 0,
    val totalLessonsCount: Int = 6,
    val recentGamesCount: Int = 0,
    val lastAnalysedTitle: String? = null
)

class HomeViewModel(repo: KnightScopeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.userProfileFlow.collect { prof ->
                _uiState.update { it.copy(userProfile = prof) }
            }
        }
        viewModelScope.launch {
            repo.puzzleRatingFlow.collect { pr ->
                _uiState.update { it.copy(puzzleRating = pr.rating.roundToInt()) }
            }
        }
        viewModelScope.launch {
            repo.openingStreakFlow.collect { st ->
                _uiState.update { it.copy(openingStreak = st) }
            }
        }
        viewModelScope.launch {
            repo.completedLessonsFlow.collect { done ->
                _uiState.update { it.copy(completedLessonsCount = done.size) }
            }
        }
        viewModelScope.launch {
            repo.historyFlow.collect { hist ->
                _uiState.update {
                    it.copy(
                        recentGamesCount = hist.size,
                        lastAnalysedTitle = hist.firstOrNull()?.title
                    )
                }
            }
        }
    }
}
