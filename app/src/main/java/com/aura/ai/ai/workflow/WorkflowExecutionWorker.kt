package com.aura.ai.ai.workflow

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.domain.repository.WorkflowRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Fires when a scheduled [com.aura.ai.domain.model.Workflow]'s `WorkManager` `PeriodicWorkRequest`
 *  is due — loads the current workflow definition fresh (not a snapshot from when it was
 *  scheduled, so an edit between runs takes effect on the very next one) and runs it via
 *  [WorkflowExecutor]. A workflow deleted or disabled since scheduling is a silent no-op, not a
 *  crash: [com.aura.ai.ai.workflow.WorkflowManager] is responsible for cancelling the
 *  `WorkManager` request when that happens, but this worker doesn't trust that timing and checks
 *  for itself regardless. */
@HiltWorker
class WorkflowExecutionWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val workflowRepository: WorkflowRepository,
        private val workflowExecutor: WorkflowExecutor,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val workflowId = inputData.getString(KEY_WORKFLOW_ID) ?: return Result.failure()
            val workflow = workflowRepository.get(workflowId) ?: return Result.success()
            if (!workflow.enabled) return Result.success()

            return when (workflowExecutor.execute(workflow)) {
                is AuraResult.Success -> Result.success()
                is AuraResult.Failure -> Result.failure()
            }
        }

        companion object {
            const val KEY_WORKFLOW_ID = "workflow_id"
        }
    }
