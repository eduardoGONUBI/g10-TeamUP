package com.example.teamup.data.remote.model

import com.google.gson.annotations.SerializedName

data class PaginatedResponse<T>(
    val data: List<T>,
    val meta: Meta
)

data class Meta(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page")    val lastPage:    Int,
    @SerializedName("per_page")     val perPage:     Int,
    val total: Int
)
