package com.opportunity.app.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opportunity.app.domain.model.Place
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.placesDataStore: DataStore<Preferences> by preferencesDataStore(name = "opportunity_places")

@Singleton
class PlaceStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val placesKey = stringPreferencesKey("places_json")
    private val json = Json { ignoreUnknownKeys = true }

    val places: Flow<List<Place>> = context.placesDataStore.data.map { prefs ->
        prefs[placesKey]?.let { runCatching { json.decodeFromString<List<Place>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun addPlace(name: String, latitude: Double, longitude: Double, radiusMeters: Double = 150.0) {
        val current = places.first()
        val updated = current + Place(UUID.randomUUID().toString(), name, latitude, longitude, radiusMeters)
        context.placesDataStore.edit { it[placesKey] = json.encodeToString(updated) }
    }

    suspend fun removePlace(id: String) {
        val updated = places.first().filterNot { it.id == id }
        context.placesDataStore.edit { it[placesKey] = json.encodeToString(updated) }
    }
}
