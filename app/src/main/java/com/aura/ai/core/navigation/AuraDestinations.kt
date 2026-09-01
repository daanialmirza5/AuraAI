package com.aura.ai.core.navigation

/** Top-level flow: splash -> onboarding -> login -> permissions -> the tabbed app shell. */
object AuraRoute {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val PERMISSIONS = "permissions"
    const val APP_SHELL = "app_shell"
}

/** The 5 bottom-nav destinations hosted inside the app shell's own nested NavHost. */
object AppTabRoute {
    const val HOME = "tab_home"
    const val AURA = "tab_aura"
    const val WORKSPACE = "tab_workspace"
    const val AUTOMATE = "tab_automate"
    const val PROFILE = "tab_profile"
}
