package com.aura.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aura.ai.data.local.dao.AutomationDao
import com.aura.ai.data.local.dao.ChatMessageDao
import com.aura.ai.data.local.dao.DeviceDao
import com.aura.ai.data.local.dao.MemoryDao
import com.aura.ai.data.local.dao.TodoDao
import com.aura.ai.data.local.dao.WorkflowDao
import com.aura.ai.data.local.entity.AutomationEntity
import com.aura.ai.data.local.entity.ChatMessageEntity
import com.aura.ai.data.local.entity.DeviceEntity
import com.aura.ai.data.local.entity.MemoryEntity
import com.aura.ai.data.local.entity.TodoEntity
import com.aura.ai.data.local.entity.WorkflowEntity

@Database(
    entities = [
        DeviceEntity::class,
        AutomationEntity::class,
        ChatMessageEntity::class,
        MemoryEntity::class,
        TodoEntity::class,
        WorkflowEntity::class,
    ],
    version = 3,
    // Schema export (writing app/schemas/*.json for migration testing) is off, not on principle
    // but because this project has no migrations yet — a single version, seeded fresh on every
    // install (DatabaseSeeder). Room 2.8.4's own schema-bundle (de)serialization uses
    // kotlinx-serialization internally, and this build observed real, hard-to-pin-down version
    // drift between the debug and release KSP tasks' processor classpaths that broke re-reading a
    // schema file written by the other variant (AbstractMethodError deep in
    // androidx.room.migration.bundle.*$$serializer). Re-enable once real migrations make export
    // worth the KSP-classpath fragility — see docs/ANDROID_AUTOMATION.md §5 for the full story.
    exportSchema = false,
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    abstract fun automationDao(): AutomationDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun memoryDao(): MemoryDao

    abstract fun todoDao(): TodoDao

    abstract fun workflowDao(): WorkflowDao

    companion object {
        const val DATABASE_NAME = "aura_database"
    }
}
