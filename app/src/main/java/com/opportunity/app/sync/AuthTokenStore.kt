package com.opportunity.app.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "opportunity_sync")

@Singleton
class AuthTokenStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val tokenKey = stringPreferencesKey("auth_token")
    private val lastSyncKey = longPreferencesKey("last_sync_epoch_millis")

    suspend fun token(): String? = context.syncDataStore.data.first()[tokenKey]

    suspend fun saveToken(token: String) {
        context.syncDataStore.edit { it[tokenKey] = token }
    }

    suspend fun lastSyncEpochMillis(): Long = context.syncDataStore.data.first()[lastSyncKey] ?: 0L

    suspend fun saveLastSync(epochMillis: Long) {
        context.syncDataStore.edit { it[lastSyncKey] = epochMillis }
    }
}
