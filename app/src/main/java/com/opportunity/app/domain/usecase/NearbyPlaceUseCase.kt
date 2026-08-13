package com.opportunity.app.domain.usecase

import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.Place
import com.opportunity.app.location.Coordinates
import com.opportunity.app.location.LocationRepository
import com.opportunity.app.location.PlaceStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class NearbyPlaceMatch(val place: Place, val matchingOpportunities: List<OpportunityEntity>)

/**
 * Section 15 "Context-Aware Suggestions": "You're at college. You have two college-related
 * tasks." Matches the user's last known location against saved Places, then finds actionable
 * Opportunities whose category matches that place's name.
 */
@Singleton
class NearbyPlaceUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val placeStore: PlaceStore,
    private val opportunityRepository: OpportunityRepository
) {
    suspend fun currentPlaceMatch(): NearbyPlaceMatch? {
        val current = locationRepository.currentLocation() ?: return null
        val places = placeStore.places.first()
        val nearby = places.firstOrNull { place ->
            LocationRepository.distanceMeters(current, Coordinates(place.latitude, place.longitude)) <= place.radiusMeters
        } ?: return null

        val matching = opportunityRepository.observeAllActionable().first().filter { opp ->
            opp.status != OpportunityStatus.COMPLETED &&
                opp.category?.equals(nearby.name, ignoreCase = true) == true
        }
        return NearbyPlaceMatch(nearby, matching)
    }
}
