package com.aura.ai.ai.workflow

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aura.ai.domain.model.Workflow
import com.aura.ai.domain.model.WorkflowTrigger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps `WorkManager` in sync with a [Workflow]'s trigger — one uniquely-named
 * `PeriodicWorkRequest` per workflow id, so re-scheduling (an edit) or cancelling (disable/delete)
 * is always addressing exactly one request, never accidentally duplicating or orphaning one.
 * `PeriodicWorkRequest`'s own documented imprecision (not exact-to-the-minute, `WorkManager`
 * batches to save battery) is accepted rather than fought with `AlarmManager` — `ReminderTool`
 * already established that inexactness is an acceptable trade for this class of "roughly around
 * this time" feature, and reusing the same `WorkManager`/`HiltWorkerFactory` plumbing
 * `docs/ANDROID_AUTOMATION.md` §3 already wired up avoids a second scheduling mechanism.
 */
@Singleton
class WorkflowScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun schedule(workflow: Workflow) {
            if (workflow.trigger == WorkflowTrigger.Manual || !workflow.enabled) {
                cancel(workflow.id)
                return
            }
            val interval =
                when (workflow.trigger) {
                    WorkflowTrigger.Daily -> Duration.ofDays(1)
                    WorkflowTrigger.Weekly -> Duration.ofDays(7)
                    WorkflowTrigger.Manual -> return
                }
            val initialDelay = initialDelayFor(workflow)
            val request =
                PeriodicWorkRequestBuilder<WorkflowExecutionWorker>(interval)
                    .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder().putString(WorkflowExecutionWorker.KEY_WORKFLOW_ID, workflow.id).build())
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(uniqueWorkName(workflow.id), ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(workflowId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(workflowId))
        }

        private fun uniqueWorkName(workflowId: String) = "workflow-$workflowId"

        private fun initialDelayFor(workflow: Workflow): Duration {
            val now = LocalDateTime.now()
            val targetTime = LocalTime.of(workflow.triggerHour, workflow.triggerMinute)
            var target = now.toLocalDate().atTime(targetTime)
            if (workflow.trigger == WorkflowTrigger.Weekly) {
                val targetDay = DayOfWeek.of(workflow.triggerDayOfWeek.coerceIn(1, 7))
                target = target.with(TemporalAdjusters.nextOrSame(targetDay))
            }
            if (!target.isAfter(now)) {
                target = if (workflow.trigger == WorkflowTrigger.Weekly) target.plusWeeks(1) else target.plusDays(1)
            }
            return Duration.between(now, target)
        }
    }
