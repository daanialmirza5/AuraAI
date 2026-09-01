package com.aura.ai.domain.model

enum class AppPermission(
    val storageKey: String,
) {
    Microphone("mic"),
    Notifications("notifications"),
    Calendar("calendar"),
    Location("location"),
    Contacts("contacts"),
}
