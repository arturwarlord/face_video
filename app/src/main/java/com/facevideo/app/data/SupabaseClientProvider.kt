package com.facevideo.app.data

import com.facevideo.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Client-side Supabase configuration.
 *
 * Only the project URL and publishable key belong in the Android client.
 * Never use a secret/service_role key here.
 */
object SupabaseClientProvider {
    val client by lazy {
        if (!BuildConfig.SUPABASE_CONFIGURED) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
            }
        }
    }
}
