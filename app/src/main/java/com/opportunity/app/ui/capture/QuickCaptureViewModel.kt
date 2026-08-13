package com.opportunity.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.domain.usecase.CaptureOpportunityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Quick Capture bottom sheet: input + inline triage cards for INBOX items.
 *
 * Each triage card supports multiple independent quick actions (category pill, duration
 * stepper) that can fire in rapid succession on the same item. An earlier version of this kind
 * of multi-field triage (ProjectDetailViewModel) had three independent getById-copy-save calls
 * race each other and silently drop writes -- found live on device, not in review. Here every
 * mutation for a given id goes through a per-id Mutex so read-modify-write is always serialized,
 * not just "atomic within one call" like the earlier fix.
 */
@HiltViewModel
class QuickCaptureViewModel @Inject constructor(
    private val repository: OpportunityRepository,
    private val captureUseCase: CaptureOpportunityUseCase
) : ViewModel() {

    val inboxItems: StateFlow<List<OpportunityEntity>> = repository.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing

    private val mutexes = mutableMapOf<String, Mutex>()
    private fun mutexFor(id: String) = synchronized(mutexes) { mutexes.getOrPut(id) { Mutex() } }

    private fun mutate(id: String, transform: (OpportunityEntity) -> OpportunityEntity) = viewModelScope.launch {
        mutexFor(id).withLock {
            repository.getById(id)?.let { repository.save(transform(it)) }
        }
    }

    fun capture(rawInput: String, dueToday: Boolean = false, highPriority: Boolean = false) {
        viewModelScope.launch {
            val draft = captureUseCase.captureRaw(rawInput, dueToday, highPriority)
            _isParsing.value = true
            captureUseCase.enrichWithAi(draft.id, rawInput)
            _isParsing.value = false
        }
    }

    fun setTypeAndActivate(id: String, type: OpportunityType) = mutate(id) {
        it.copy(type = type, status = OpportunityStatus.ACTIVE)
    }

    fun adjustDuration(id: String, deltaMinutes: Int) = mutate(id) {
        val current = it.estimatedDurationMinutes ?: 30
        it.copy(estimatedDurationMinutes = (current + deltaMinutes).coerceIn(5, 240))
    }

    fun complete(id: String) = viewModelScope.launch { repository.complete(id) }
}
