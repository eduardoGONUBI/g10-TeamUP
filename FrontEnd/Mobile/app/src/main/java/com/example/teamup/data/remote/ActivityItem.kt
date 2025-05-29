package com.example.teamup.data.remote

data class ActivityItem(
    val id: String,
    val title: String,
    val location: String,
    val date: String,
    val participants: Int,
    val maxParticipants: Int,
    val organizer: String,
    val creatorId: Int,     // ← NEW
    val latitude: Double,
    val longitude: Double,
    val isCreator: Boolean  // ← NEW: compare creatorId to your userId in the ViewModel
)
