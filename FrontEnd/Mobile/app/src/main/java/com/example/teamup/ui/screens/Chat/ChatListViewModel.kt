// File: app/src/main/java/com/example/teamup/ui/screens/Chat/ChatListScreenViewModel.kt
package com.example.teamup.ui.screens.Chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ChatItem
import com.example.teamup.data.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListState(
    // full lists (newest‐first)
    val fullActive:  List<ChatItem> = emptyList(),
    val fullArchive: List<ChatItem> = emptyList(),

    // visible slices (current page)
    val visibleActive:  List<ChatItem> = emptyList(),
    val visibleArchive: List<ChatItem> = emptyList(),

    // page indices and pageSize
    val activePage:  Int = 1,
    val archivePage: Int = 1,
    val pageSize:    Int = 10,

    // flags for “Load more”
    val hasMoreActive:  Boolean = false,
    val hasMoreArchive: Boolean = false,

    val loading: Boolean = false,
    val error:   String? = null
)

class ChatListScreenViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state

    /** 1) Load all chats, reverse so newest appear first, then paginate. */
    fun load(token: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val allChats: List<ChatItem> = repo.myChats(token)

            // Reverse each list so newest come first
            val activeChats   = allChats.filter { it.status == "in progress" }.reversed()
            val archivedChats = (allChats.filter { it.status != "in progress" }).reversed()

            val pageSize = _state.value.pageSize

            // Take the first page from each reversed list
            val firstActiveSlice   = activeChats.take(pageSize)
            val firstArchiveSlice  = archivedChats.take(pageSize)

            _state.update {
                it.copy(
                    fullActive       = activeChats,
                    fullArchive      = archivedChats,
                    visibleActive    = firstActiveSlice,
                    visibleArchive   = firstArchiveSlice,
                    activePage       = 1,
                    archivePage      = 1,
                    hasMoreActive    = activeChats.size > pageSize,
                    hasMoreArchive   = archivedChats.size > pageSize,
                    loading          = false,
                    error            = null
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    loading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /** 2) “Load more” for active (newest‐first) */
    fun loadMoreActive() {
        val ui = _state.value
        if (!ui.hasMoreActive) return

        val nextPage   = ui.activePage + 1
        val toIndex    = (nextPage * ui.pageSize).coerceAtMost(ui.fullActive.size)
        val newSlice   = ui.fullActive.take(toIndex)

        _state.update {
            it.copy(
                visibleActive  = newSlice,
                activePage     = nextPage,
                hasMoreActive  = ui.fullActive.size > toIndex
            )
        }
    }

    /** 3) “Load more” for archive (newest‐first) */
    fun loadMoreArchive() {
        val ui = _state.value
        if (!ui.hasMoreArchive) return

        val nextPage     = ui.archivePage + 1
        val toIndex      = (nextPage * ui.pageSize).coerceAtMost(ui.fullArchive.size)
        val newSlice     = ui.fullArchive.take(toIndex)

        _state.update {
            it.copy(
                visibleArchive  = newSlice,
                archivePage     = nextPage,
                hasMoreArchive  = ui.fullArchive.size > toIndex
            )
        }
    }
}
