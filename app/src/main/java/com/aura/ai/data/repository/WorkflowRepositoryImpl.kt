package com.aura.ai.data.repository

import com.aura.ai.data.local.dao.WorkflowDao
import com.aura.ai.data.local.entity.WorkflowEntity
import com.aura.ai.domain.model.Workflow
import com.aura.ai.domain.model.WorkflowStepEncoding
import com.aura.ai.domain.model.WorkflowTrigger
import com.aura.ai.domain.repository.WorkflowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun WorkflowEntity.toDomain() =
    Workflow(
        id = id,
        name = name,
        description = description,
        trigger = runCatching { WorkflowTrigger.valueOf(trigger) }.getOrDefault(WorkflowTrigger.Manual),
        triggerHour = triggerHour,
        triggerMinute = triggerMinute,
        triggerDayOfWeek = triggerDayOfWeek,
        steps = WorkflowStepEncoding.decode(stepsEncoded),
        enabled = enabled,
    )

private fun Workflow.toEntity() =
    WorkflowEntity(
        id = id,
        name = name,
        description = description,
        trigger = trigger.name,
        triggerHour = triggerHour,
        triggerMinute = triggerMinute,
        triggerDayOfWeek = triggerDayOfWeek,
        stepsEncoded = WorkflowStepEncoding.encode(steps),
        enabled = enabled,
    )

@Singleton
class WorkflowRepositoryImpl
    @Inject
    constructor(
        private val dao: WorkflowDao,
    ) : WorkflowRepository {
        override fun observeWorkflows(): Flow<List<Workflow>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun get(id: String): Workflow? = dao.get(id)?.toDomain()

        override suspend fun upsert(workflow: Workflow) = dao.upsert(workflow.toEntity())

        override suspend fun delete(workflow: Workflow) = dao.delete(workflow.toEntity())

        override suspend fun setEnabled(
            id: String,
            enabled: Boolean,
        ) = dao.setEnabled(id, enabled)
    }
