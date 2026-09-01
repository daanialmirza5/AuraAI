package com.aura.ai.ai.workflow

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.tools.ToolResult
import com.aura.ai.domain.model.Workflow
import com.aura.ai.domain.repository.WorkflowRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** The one thing `AutomateViewModel` depends on for workflows — keeps [WorkflowRepository]
 *  (persistence) and [WorkflowScheduler] (`WorkManager`) in sync with each other so nothing else
 *  ever has to remember to call both. Same "one facade, several composed collaborators" shape as
 *  `AuraRuntimeFacade`/`VoiceRuntime`. */
@Singleton
class WorkflowManager
    @Inject
    constructor(
        private val repository: WorkflowRepository,
        private val scheduler: WorkflowScheduler,
        private val executor: WorkflowExecutor,
    ) {
        fun observeWorkflows(): Flow<List<Workflow>> = repository.observeWorkflows()

        suspend fun addWorkflow(workflow: Workflow) {
            repository.upsert(workflow)
            scheduler.schedule(workflow)
        }

        suspend fun setEnabled(
            workflow: Workflow,
            enabled: Boolean,
        ) {
            repository.setEnabled(workflow.id, enabled)
            scheduler.schedule(workflow.copy(enabled = enabled))
        }

        suspend fun deleteWorkflow(workflow: Workflow) {
            scheduler.cancel(workflow.id)
            repository.delete(workflow)
        }

        suspend fun runNow(workflow: Workflow): AuraResult<List<ToolResult>> = executor.execute(workflow)
    }
