package com.aura.ai.ai.reasoning

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.aura.ai.core.reasoning.context.PermissionChecker
import com.aura.ai.core.reasoning.context.PermissionState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The real [PermissionChecker] — core-reasoning's own default
 * (`com.aura.ai.core.reasoning.context.UnknownPermissionChecker`) reports every permission as
 * [PermissionState.Unknown] because it's pure Kotlin/JVM with no `Context`; this is the
 * Android-backed implementation `:app` binds instead, the same "interface in a pure core module,
 * real implementation wired at the composition root" pattern `core-actions`' tools and
 * `core-providers`' `AIProvider`s already use. A live check is always definitive — [check] never
 * returns [PermissionState.Unknown] itself, only [PermissionState.Granted] or [PermissionState.Denied].
 */
class AndroidPermissionChecker
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PermissionChecker {
        override fun check(permission: String): PermissionState {
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            return if (granted) PermissionState.Granted else PermissionState.Denied
        }
    }
