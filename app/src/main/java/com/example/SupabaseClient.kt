package com.example

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseManager {
    private const val PREFS_NAME = "supabase_config"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON = "supabase_key"

    private var activeClient: SupabaseClient? = null
    var configuredUrl: String = ""
        private set
    var configuredKey: String = ""
        private set

    val jsonInstance = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_URL, null)
        val savedKey = prefs.getString(KEY_ANON, null)

        val finalUrl = when {
            !savedUrl.isNullOrBlank() -> savedUrl.trim()
            BuildConfig.SUPABASE_URL.isNotBlank() -> BuildConfig.SUPABASE_URL.trim()
            else -> ""
        }
        val finalKey = when {
            !savedKey.isNullOrBlank() -> savedKey.trim()
            BuildConfig.SUPABASE_KEY.isNotBlank() -> BuildConfig.SUPABASE_KEY.trim()
            else -> ""
        }

        updateCredentials(context, finalUrl, finalKey)
    }

    fun getClient(): SupabaseClient? = activeClient

    fun isConfigured(): Boolean = activeClient != null && configuredUrl.isNotBlank() && configuredKey.isNotBlank()

    fun updateCredentials(context: Context, url: String, key: String): Boolean {
        val trimmedUrl = url.trim()
        val trimmedKey = key.trim()

        configuredUrl = trimmedUrl
        configuredKey = trimmedKey

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_URL, trimmedUrl)
            .putString(KEY_ANON, trimmedKey)
            .apply()

        if ((trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) && trimmedKey.isNotBlank()) {
            return try {
                activeClient = createSupabaseClient(
                    supabaseUrl = trimmedUrl,
                    supabaseKey = trimmedKey
                ) {
                    defaultSerializer = KotlinXSerializer(jsonInstance)
                    install(Postgrest)
                }
                true
            } catch (e: Exception) {
                activeClient = null
                false
            }
        }
        activeClient = null
        return false
    }
}
