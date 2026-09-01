package com.aura.ai.ai.di

import com.aura.ai.ai.AppAutomationTool
import com.aura.ai.ai.DeviceNavigationTool
import com.aura.ai.ai.ImageUnderstandingTool
import com.aura.ai.ai.ReadScreenTool
import com.aura.ai.core.actions.BarcodeScanTool
import com.aura.ai.core.actions.CalendarTool
import com.aura.ai.core.actions.ClipboardTool
import com.aura.ai.core.actions.DeferredActionTool
import com.aura.ai.core.actions.EmailTool
import com.aura.ai.core.actions.ExportPdfTool
import com.aura.ai.core.actions.FileSearchTool
import com.aura.ai.core.actions.MediaControlTool
import com.aura.ai.core.actions.NavigationTool
import com.aura.ai.core.actions.NotifyTool
import com.aura.ai.core.actions.OcrTool
import com.aura.ai.core.actions.OpenAppTool
import com.aura.ai.core.actions.OpenSettingsTool
import com.aura.ai.core.actions.ReceiptScanTool
import com.aura.ai.core.actions.ReminderTool
import com.aura.ai.core.actions.SaveFileTool
import com.aura.ai.core.actions.ShareTool
import com.aura.ai.core.actions.ShoppingSearchTool
import com.aura.ai.core.actions.WebSearchTool
import com.aura.ai.core.tools.Tool
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Every [Tool] AURA ships with — the 9 generic, Android-backed ones from core-actions plus the
 * one app-specific tool ([AppAutomationTool]) that needs the app's own repositories. All 10 land
 * in the same `Set<Tool>`, which [com.aura.ai.AuraApplication] registers into the shared
 * `ToolRegistry` at startup — see its `onCreate()`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiToolsModule {
    @Binds
    @IntoSet
    abstract fun bindOpenAppTool(impl: OpenAppTool): Tool

    @Binds
    @IntoSet
    abstract fun bindReminderTool(impl: ReminderTool): Tool

    @Binds
    @IntoSet
    abstract fun bindCalendarTool(impl: CalendarTool): Tool

    @Binds
    @IntoSet
    abstract fun bindNavigationTool(impl: NavigationTool): Tool

    @Binds
    @IntoSet
    abstract fun bindEmailTool(impl: EmailTool): Tool

    @Binds
    @IntoSet
    abstract fun bindWebSearchTool(impl: WebSearchTool): Tool

    @Binds
    @IntoSet
    abstract fun bindShoppingSearchTool(impl: ShoppingSearchTool): Tool

    @Binds
    @IntoSet
    abstract fun bindNotifyTool(impl: NotifyTool): Tool

    @Binds
    @IntoSet
    abstract fun bindExportPdfTool(impl: ExportPdfTool): Tool

    @Binds
    @IntoSet
    abstract fun bindSaveFileTool(impl: SaveFileTool): Tool

    @Binds
    @IntoSet
    abstract fun bindAppAutomationTool(impl: AppAutomationTool): Tool

    @Binds
    @IntoSet
    abstract fun bindOpenSettingsTool(impl: OpenSettingsTool): Tool

    @Binds
    @IntoSet
    abstract fun bindClipboardTool(impl: ClipboardTool): Tool

    @Binds
    @IntoSet
    abstract fun bindShareTool(impl: ShareTool): Tool

    @Binds
    @IntoSet
    abstract fun bindFileSearchTool(impl: FileSearchTool): Tool

    @Binds
    @IntoSet
    abstract fun bindMediaControlTool(impl: MediaControlTool): Tool

    @Binds
    @IntoSet
    abstract fun bindDeferredActionTool(impl: DeferredActionTool): Tool

    @Binds
    @IntoSet
    abstract fun bindReadScreenTool(impl: ReadScreenTool): Tool

    @Binds
    @IntoSet
    abstract fun bindDeviceNavigationTool(impl: DeviceNavigationTool): Tool

    @Binds
    @IntoSet
    abstract fun bindOcrTool(impl: OcrTool): Tool

    @Binds
    @IntoSet
    abstract fun bindBarcodeScanTool(impl: BarcodeScanTool): Tool

    @Binds
    @IntoSet
    abstract fun bindReceiptScanTool(impl: ReceiptScanTool): Tool

    @Binds
    @IntoSet
    abstract fun bindImageUnderstandingTool(impl: ImageUnderstandingTool): Tool
}
