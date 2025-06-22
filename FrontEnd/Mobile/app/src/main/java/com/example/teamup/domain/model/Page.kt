package com.example.teamup.domain.model


data class Page<T>(
    val data: List<T>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Int
)