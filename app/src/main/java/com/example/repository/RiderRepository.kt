package com.example.repository

import com.example.SupabaseManager
import com.example.model.Rider
import io.github.jan.supabase.postgrest.from

class RiderRepository {

    suspend fun login(phone: String, password: String): Result<Rider> {
        val client = SupabaseManager.getClient()
            ?: return Result.failure(
                IllegalStateException("Supabase is not configured. Please set your Supabase URL and Key.")
            )

        return try {
            val cleanPhone = phone.trim()
            val cleanPassword = password.trim()

            val riders = client.from("riders").select {
                filter {
                    eq("phone", cleanPhone)
                    eq("password", cleanPassword)
                }
            }.decodeList<Rider>()

            val matched = riders.firstOrNull()
            if (matched != null) {
                Result.success(matched)
            } else {
                Result.failure(IllegalArgumentException("No rider found with this Phone and Password. Please check your credentials."))
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Unknown error while connecting to Supabase"
            Result.failure(Exception("Connection error: $msg", e))
        }
    }

    suspend fun updateLocation(riderId: String?, phone: String, lat: Double, lon: Double): Result<Unit> {
        val client = SupabaseManager.getClient()
            ?: return Result.failure(IllegalStateException("Supabase client is not connected"))

        return try {
            val trimmedId = riderId?.trim()
            val trimmedPhone = phone.trim()

            client.from("riders").update({
                set("current_latitude", lat)
                set("current_longitude", lon)
            }) {
                filter {
                    if (!trimmedId.isNullOrBlank()) {
                        eq("id", trimmedId)
                    } else {
                        eq("phone", trimmedPhone)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
