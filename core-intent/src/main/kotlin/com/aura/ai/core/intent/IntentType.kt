package com.aura.ai.core.intent

/** The taxonomy AURA classifies every utterance into before core-planner decides what, if
 *  anything, needs to happen. [Unknown] is a distinct outcome from [Conversation] — [Unknown]
 *  means the recognizer couldn't decide; [Conversation] means it decided the answer is "none of
 *  the above, just talk back." */
enum class IntentType {
    OpenApp,
    Reminder,
    Calendar,
    Research,
    Coding,
    Shopping,
    Navigation,
    Email,
    Automation,
    Conversation,
    Unknown,
}
