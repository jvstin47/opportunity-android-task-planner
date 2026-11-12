package com.opportunity.app.ui.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.ai.AiResult
import com.opportunity.app.ai.TaskDecomposer
import com.opportunity.app.data.local.OpportunityDateHelpers.tomorrowMorningEpochMillis
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.local.withRecurrence
import com.opportunity.app.data.local.withReminder
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.domain.model.PriorityBucket
import com.opportunity.app.domain.model.RecurrenceFrequency
import com.opportunity.app.domain.model.RecurrenceRule
import com.opportunity.app.domain.model.ReminderConfig
import com.opportunity.app.domain.usecase.PostponementAction
import com.opportunity.app.domain.usecase.PostponementAdvisor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectDetailUiState(
    val opportunity: OpportunityEntity? = null,
    val children: List<OpportunityEntity> = emptyList(),
    val isDecomposing: Boolean = false,
    val decomposeError: String? = null,
    val postponementAdvice: PostponementAdvisor.Advice? = null
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: OpportunityRepository,
    private val taskDecomposer: TaskDecomposer,
    private val postponementAdvisor: PostponementAdvisor
) : ViewModel() {

    private val opportunityId: String = checkNotNull(savedStateHandle["opportunityId"])
    private val _extra = MutableStateFlow(ProjectDetailUiState())

    val uiState: StateFlow<ProjectDetailUiState> = combine(
        repository.observeById(opportunityId),
        repository.observeChildren(opportunityId),
        _extra
    ) { opportunity, children, extra ->
        extra.copy(opportunity = opportunity, children = children)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectDetailUiState())

    fun complete() = viewModelScope.launch { repository.complete(opportunityId) }

    fun reopen() = viewModelScope.launch { repository.reopen(opportunityId) }

    fun deleteItem() = viewModelScope.launch { repository.delete(opportunityId) }

    /**
     * Section 11 "Postponement Intelligence": the first couple postpones stay silent (see
     * PostponementAdvisor) -- only once the tier warrants it does a prompt surface, so tapping
     * Postpone doesn't turn into a nag every single time.
     */
    fun postpone() = viewModelScope.launch {
        repository.postpone(opportunityId)
        val updated = repository.getById(opportunityId) ?: return@launch
        val advice = postponementAdvisor.evaluate(updated)
        if (advice.action != PostponementAction.NONE) {
            _extra.value = _extra.value.copy(postponementAdvice = advice)
        }
    }

    fun dismissPostponementPrompt() {
        _extra.value = _extra.value.copy(postponementAdvice = null)
    }

    fun moveToTomorrow() = viewModelScope.launch {
        repository.getById(opportunityId)?.let {
            repository.save(it.copy(deadlineEpochMillis = tomorrowMorningEpochMillis()))
        }
        dismissPostponementPrompt()
    }

    fun makeRecurring() = viewModelScope.launch {
        repository.getById(opportunityId)?.let {
            repository.save(it.withRecurrence(RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY)))
        }
        dismissPostponementPrompt()
    }

    fun archiveItem() = viewModelScope.launch {
        repository.delete(opportunityId)
        dismissPostponementPrompt()
    }

    fun updateDetails(
        title: String,
        description: String,
        category: String?,
        type: OpportunityType,
        priorityBucket: PriorityBucket?,
        estimatedDurationMinutes: Int?,
        deadlineEpochMillis: Long?,
        reminderFrequencyMinutes: Int?,
        recurrenceFrequency: RecurrenceFrequency
    ) = viewModelScope.launch {
        val current = repository.getById(opportunityId) ?: return@launch
        val trimmedTitle = title.trim().ifBlank { current.title }
        val cleanCategory = category?.trim()?.takeIf { it.isNotBlank() }
        val userPriority = when (priorityBucket) {
            PriorityBucket.DO_SOON -> 1
            PriorityBucket.SHOULD_DO -> 3
            PriorityBucket.GROW, PriorityBucket.EXPLORE, PriorityBucket.PERSONAL, PriorityBucket.SOMEDAY -> 5
            null -> current.userPriority
        }
        var updated = current.copy(
            title = trimmedTitle,
            description = description.trim(),
            category = cleanCategory,
            type = type,
            status = if (current.status == OpportunityStatus.INBOX) OpportunityStatus.ACTIVE else current.status,
            priorityBucket = priorityBucket,
            userPriority = userPriority,
            estimatedDurationMinutes = estimatedDurationMinutes?.coerceIn(1, 480),
            deadlineEpochMillis = deadlineEpochMillis
        )
        updated = if (reminderFrequencyMinutes != null && reminderFrequencyMinutes > 0) {
            updated.withReminder(
                ReminderConfig(
                    enabled = true,
                    frequencyMinutes = reminderFrequencyMinutes,
                    nextReminderEpochMillis = System.currentTimeMillis() + reminderFrequencyMinutes * 60_000L
                )
            )
        } else {
            updated.withReminder(ReminderConfig(enabled = false))
        }
        updated = updated.withRecurrence(RecurrenceRule(frequency = recurrenceFrequency))
        repository.save(updated)
    }

    fun addSubtask(title: String) = viewModelScope.launch {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return@launch
        val parent = repository.getById(opportunityId) ?: return@launch
        if (parent.type != OpportunityType.PROJECT) {
            repository.save(parent.copy(type = OpportunityType.PROJECT, status = OpportunityStatus.ACTIVE))
        }
        val nextOrder = uiState.value.children.size
        repository.save(
            OpportunityEntity(
                title = trimmed,
                type = OpportunityType.TASK,
                status = OpportunityStatus.ACTIVE,
                priorityBucket = parent.priorityBucket ?: PriorityBucket.SHOULD_DO,
                category = parent.category,
                parentProjectId = opportunityId,
                sortOrderInParent = nextOrder
            )
        )
    }

    fun toggleChildComplete(child: OpportunityEntity) = viewModelScope.launch {
        if (child.status == OpportunityStatus.COMPLETED) {
            repository.reopen(child.id)
        } else {
            repository.complete(child.id)
        }
    }

    /**
     * Section 2.5 "Triage actions from Inbox": manual path for moving an item out of Inbox and
     * onto Today without waiting on AI enrichment -- capture must work whether or not AI is
     * configured (section 21 offline-first).
     */
    fun addToToday(minutes: Int, priority: Int, dueToday: Boolean) = viewModelScope.launch {
        val opportunity = repository.getById(opportunityId) ?: return@launch
        val bucket = when (priority) {
            1 -> PriorityBucket.DO_SOON
            3 -> PriorityBucket.SHOULD_DO
            else -> PriorityBucket.SOMEDAY
        }
        val deadline = if (dueToday) {
            java.time.LocalDate.now().atTime(23, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else null
        repository.save(
            opportunity.copy(
                status = OpportunityStatus.ACTIVE,
                estimatedDurationMinutes = minutes,
                userPriority = priority,
                priorityBucket = bucket,
                deadlineEpochMillis = deadline
            )
        )
    }

    fun decomposeWithAi() {
        viewModelScope.launch {
            val opportunity = repository.getById(opportunityId) ?: return@launch
            _extra.value = _extra.value.copy(isDecomposing = true, decomposeError = null)
            when (val result = taskDecomposer.decompose(opportunity.title, opportunity.description)) {
                is AiResult.Success -> {
                    repository.save(opportunity.copy(type = OpportunityType.PROJECT, status = OpportunityStatus.ACTIVE))
                    val children = result.value.subtasks.map { subtask ->
                        OpportunityEntity(
                            title = subtask.title,
                            type = OpportunityType.TASK,
                            status = OpportunityStatus.ACTIVE,
                            estimatedDurationMinutes = subtask.estimatedDurationMinutes,
                            parentProjectId = opportunityId,
                            sortOrderInParent = subtask.order
                        )
                    }
                    repository.saveAll(children)
                    _extra.value = _extra.value.copy(isDecomposing = false)
                }
                is AiResult.Failure -> {
                    _extra.value = _extra.value.copy(isDecomposing = false, decomposeError = result.reason)
                }
            }
        }
    }
}
