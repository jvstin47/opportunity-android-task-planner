package com.opportunity.app.domain.model

enum class OpportunityType {
    TASK,
    PROJECT,
    GOAL,
    HABIT,
    IDEA,
    SOMEDAY
}

enum class OpportunityStatus {
    INBOX,
    ACTIVE,
    IN_PROGRESS,
    COMPLETED,
    POSTPONED,
    ARCHIVED
}

/** Section 12 buckets. AI-suggested by default; user can override per Opportunity. */
enum class PriorityBucket {
    DO_SOON,
    SHOULD_DO,
    GROW,
    EXPLORE,
    PERSONAL,
    SOMEDAY
}

enum class TimerMode {
    COUNTDOWN,
    STOPWATCH,
    SESSION_BASED,
    TOTAL_PROGRESS
}
