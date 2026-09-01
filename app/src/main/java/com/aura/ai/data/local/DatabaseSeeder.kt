package com.aura.ai.data.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder
    @Inject
    constructor(
        private val database: AuraDatabase,
    ) {
        suspend fun seedIfNeeded() {
            val deviceDao = database.deviceDao()
            if (deviceDao.count() == 0) deviceDao.insertAll(AuraSeedData.devices)

            val automationDao = database.automationDao()
            if (automationDao.count() == 0) automationDao.insertAll(AuraSeedData.automations)

            val chatDao = database.chatMessageDao()
            if (chatDao.count() == 0) chatDao.insert(AuraSeedData.initialChatMessage(System.currentTimeMillis()))

            val memoryDao = database.memoryDao()
            if (memoryDao.count() == 0) memoryDao.insertAll(AuraSeedData.memories)

            val todoDao = database.todoDao()
            if (todoDao.count() == 0) todoDao.insertAll(AuraSeedData.todos)
        }
    }
