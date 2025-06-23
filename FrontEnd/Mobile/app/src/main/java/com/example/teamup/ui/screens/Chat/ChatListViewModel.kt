package com.example.teamup.ui.screens.Chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.domain.model.Chat
import com.example.teamup.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// estado com paginaçao
data class ChatListState(
    val fullActive: List<Chat> = emptyList(),
    val fullArchive: List<Chat> = emptyList(),

    val visibleActive: List<Chat> = emptyList(),
    val visibleArchive: List<Chat> = emptyList(),

    val activeRemotePage: Int = 1,
    val archiveRemotePage: Int = 1,

    val hasMoreActive: Boolean = false,
    val hasMoreArchive: Boolean = false,

    val loading: Boolean = false,
    val error: String? = null
)

class ChatListScreenViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state

    private var savedToken: String = ""


    // load a primeira pagina de chats
    fun loadFirstPage(token: String) = viewModelScope.launch {
        savedToken = token
        _state.update { it.copy(loading = true, error = null) }
        try {
            val page1 = repo.myChats(token, page = 1)
            // divide entre ativo e arquivado
            val active = page1.filter  { it.status == "in progress" }
            val archive = page1.filter { it.status != "in progress" }

            _state.update {
                it.copy(
                    fullActive        = active,
                    fullArchive       = archive,
                    visibleActive     = active,
                    visibleArchive    = archive,
                    activeRemotePage  = 1,
                    archiveRemotePage = 1,
                    hasMoreActive     = repo.hasMore,
                    hasMoreArchive    = repo.hasMore,
                    loading           = false,
                    error             = null
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    loading = false,
                    error   = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

 // load more ativo
    fun loadMoreActive() = viewModelScope.launch {
        val ui = _state.value
        if (!ui.hasMoreActive) return@launch

        val nextPage = ui.activeRemotePage + 1
        try {
            val pageItems = repo.myChats(savedToken, page = nextPage)
            val newActive = ui.fullActive + pageItems.filter { it.status == "in progress" }

            _state.update {
                it.copy(
                    fullActive       = newActive,
                    visibleActive    = newActive,
                    activeRemotePage = nextPage,
                    hasMoreActive    = repo.hasMore
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.localizedMessage ?: "Failed to load more") }
        }
    }

   // load more arquivado
    fun loadMoreArchive() = viewModelScope.launch {
        val ui = _state.value
        if (!ui.hasMoreArchive) return@launch

        val nextPage = ui.archiveRemotePage + 1
        try {
            val pageItems = repo.myChats(savedToken, page = nextPage)
            val newArchive = ui.fullArchive + pageItems.filter { it.status != "in progress" }

            _state.update {
                it.copy(
                    fullArchive       = newArchive,
                    visibleArchive    = newArchive,
                    archiveRemotePage = nextPage,
                    hasMoreArchive    = repo.hasMore
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.localizedMessage ?: "Failed to load more") }
        }
    }
}
