package com.opportunity.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Section 15 "Context-Aware Suggestions - Location": user-named places ("College", "Home",
 * "Gym") matched against Opportunity.category so the app can say "You're at college. You have
 * two college-related tasks." No geofencing -- a simple radius check against the last known
 * location is enough for this use case and avoids the battery/permission cost of background
 * location tracking.
 */
@Serializable
data class Place(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = 150.0
)
