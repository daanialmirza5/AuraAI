package com.aura.ai.domain.repository

import com.aura.ai.domain.model.Automation
import kotlinx.coroutines.flow.Flow

interface AutomationRepository {
    fun observeAutomations(): Flow<List<Automation>>

    suspend fun toggleAutomation(id: String)
}
