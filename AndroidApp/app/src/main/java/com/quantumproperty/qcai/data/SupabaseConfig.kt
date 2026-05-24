package com.quantumproperty.qcai.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    const val URL = "https://whnowqhnkrnuamdnbohy.supabase.co"
    const val ANON_KEY = "sb_publishable_Wzsz-DKLp97Nr2oeU5tNPA_BDpfB8lj"
}

val supabase = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.ANON_KEY
) {
    install(Auth)
    install(Postgrest)
    install(Storage)
}
