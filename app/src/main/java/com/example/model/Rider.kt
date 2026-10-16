package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Rider(
    val id: Int,
    val phone: String,
    val password: String,
    val current_latitude: Double? = null,
    val current_longitude: Double? = null
)
