package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Rider(
    val id: Long? = null,
    val phone: String = "",
    val password: String = "",
    val name: String? = null,
    val current_latitude: Double? = null,
    val current_longitude: Double? = null
)
