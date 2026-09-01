package com.aura.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val trigger: String,
    val triggerHour: Int,
    val triggerMinute: Int,
    val triggerDayOfWeek: Int,
    /** [com.aura.ai.domain.model.WorkflowStepEncoding]-encoded — see its own doc for why this
     *  isn't a `@TypeConverter`-backed structured column. */
    val stepsEncoded: String,
    val enabled: Boolean,
)
