package com.example.teamup.ui.screens.Chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ChatItem
import com.example.teamup.data.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatListState(
    val active:  List<ChatItem> = emptyList(),
    val archive: List<ChatItem> = emptyList(),
    val loading: Boolean        = false,
    val error:   String?        = null
)

class ChatListScreenViewModel(

    private val repo: ActivityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state

    fun load(token: String) = viewModelScope.launch {
        try {
            _state.value = _state.value.copy(loading = true, error = null)
            val all   = repo.myChats(token)
            val active   = all.filter { it.status == "in progress" }
            val archived = all - active
            _state.value = ChatListState(active, archived)
        } catch (e: Exception) {
            _state.value = ChatListState(error = e.message ?: "Unknown error")
        }
    }
}
