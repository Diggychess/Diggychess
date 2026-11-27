package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.chess.StockfishWasmEngine
import com.example.data.FirestoreGameSync
import com.example.data.KnightScopeRepository

class KnightScopeViewModelFactory(
    private val repo: KnightScopeRepository
) : ViewModelProvider.Factory {

    private val sharedStockfishWasm: StockfishWasmEngine by lazy {
        StockfishWasmEngine(repo.appContext)
    }

    private val sharedFirestoreSync: FirestoreGameSync by lazy {
        FirestoreGameSync(repo.appContext)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repo) as T
            modelClass.isAssignableFrom(PlayViewModel::class.java) ->
                PlayViewModel(
                    repo = repo,
                    stockfishWasm = sharedStockfishWasm,
                    firestoreSync = sharedFirestoreSync
                ) as T
            modelClass.isAssignableFrom(AnalyzeViewModel::class.java) ->
                AnalyzeViewModel(
                    repo = repo,
                    stockfishWasm = sharedStockfishWasm,
                    firestoreSync = sharedFirestoreSync
                ) as T
            modelClass.isAssignableFrom(PuzzlesViewModel::class.java) ->
                PuzzlesViewModel(repo) as T
            modelClass.isAssignableFrom(FriendsViewModel::class.java) ->
                FriendsViewModel(repo) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(repo) as T
            modelClass.isAssignableFrom(OpeningExplorerViewModel::class.java) ->
                OpeningExplorerViewModel(repo) as T
            modelClass.isAssignableFrom(OpeningTrainerViewModel::class.java) ->
                OpeningTrainerViewModel(repo) as T
            modelClass.isAssignableFrom(ChessLessonsViewModel::class.java) ->
                ChessLessonsViewModel(repo) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repo) as T
            modelClass.isAssignableFrom(AboutViewModel::class.java) ->
                AboutViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
