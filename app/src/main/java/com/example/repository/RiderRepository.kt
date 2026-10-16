package com.example.repository

import com.example.SupabaseClient
import com.example.model.Rider
import io.github.jan.supabase.postgrest.from

class RiderRepository {
    suspend fun login(phone: String, password: String): Rider? {
        val client = SupabaseClient.client ?: return null
        val riders = client.from("riders").select {
            filter {
                eq("phone", phone)
                eq("password", password)
            }
        }.decodeList<Rider>()
        return riders.firstOrNull()
    }

    suspend fun updateLocation(riderId: Int, lat: Double, lon: Double) {
        val client = SupabaseClient.client ?: return
        client.from("riders").update({
            set("current_latitude", lat)
            set("current_longitude", lon)
        }) {
            filter {
                eq("id", riderId)
            }
        }
    }
}
