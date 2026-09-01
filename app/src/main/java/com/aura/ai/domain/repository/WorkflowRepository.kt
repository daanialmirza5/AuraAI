package com.aura.ai.domain.repository

import com.aura.ai.domain.model.Workflow
import kotlinx.coroutines.flow.Flow

/** Pure persistence, no scheduling side effects — see `com.aura.ai.ai.workflow.WorkflowManager`
 *  for the layer that keeps `WorkManager` in sync with what's stored here, the same
 *  "repository is data-only, a dedicated class owns orchestration" split this codebase already
 *  uses elsewhere (`AuraRuntimeFacade`, `VoiceRuntime`). */
interface WorkflowRepository {
    fun observeWorkflows(): Flow<List<Workflow>>

    suspend fun get(id: String): Workflow?

    suspend fun upsert(workflow: Workflow)

    suspend fun delete(workflow: Workflow)

    suspend fun setEnabled(
        id: String,
        enabled: Boolean,
    )
}
