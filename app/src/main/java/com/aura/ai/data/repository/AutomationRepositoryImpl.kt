package com.aura.ai.data.repository

import com.aura.ai.data.local.dao.AutomationDao
import com.aura.ai.data.local.entity.AutomationEntity
import com.aura.ai.domain.model.Automation
import com.aura.ai.domain.repository.AutomationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun AutomationEntity.toDomain() = Automation(id = id, name = name, description = description, isOn = isOn)

@Singleton
class AutomationRepositoryImpl
    @Inject
    constructor(
        private val dao: AutomationDao,
    ) : AutomationRepository {
        override fun observeAutomations(): Flow<List<Automation>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun toggleAutomation(id: String) = dao.toggle(id)
    }
