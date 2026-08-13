package com.opportunity.app.data.local

import androidx.room.TypeConverter
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.domain.model.PriorityBucket
import com.opportunity.app.domain.model.TimerMode

class Converters {
    @TypeConverter
    fun toOpportunityType(value: String) = OpportunityType.valueOf(value)

    @TypeConverter
    fun fromOpportunityType(value: OpportunityType) = value.name

    @TypeConverter
    fun toOpportunityStatus(value: String) = OpportunityStatus.valueOf(value)

    @TypeConverter
    fun fromOpportunityStatus(value: OpportunityStatus) = value.name

    @TypeConverter
    fun toPriorityBucket(value: String?) = value?.let { PriorityBucket.valueOf(it) }

    @TypeConverter
    fun fromPriorityBucket(value: PriorityBucket?) = value?.name

    @TypeConverter
    fun toTimerMode(value: String) = TimerMode.valueOf(value)

    @TypeConverter
    fun fromTimerMode(value: TimerMode) = value.name
}
