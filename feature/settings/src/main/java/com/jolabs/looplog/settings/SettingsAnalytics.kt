package com.jolabs.looplog.settings

import com.jolabs.looplog.analytics.model.AnalyticsEvent

/** Analytics definitions for interactions on the Settings screen. */
object SettingsAnalytics {

    enum class Item(val wireName: String) {
        NOTIFICATION_SETTINGS("notification_settings"),
        PUSH_NOTIFICATIONS("push_notifications"),
        DAILY_REMINDERS("daily_reminders"),
        APPEARANCE("appearance"),
        APP_LOCK("app_lock"),
        THEME_MODE("theme_mode"),
        ACCENT_COLOR("accent_color"),
        FONT_SIZE("font_size"),
        HABIT_SECTIONS("habit_sections"),
        APP_LANGUAGE("app_language"),
        BACKUP_RESTORE("backup_restore"),
        EXPORT_DATA("export_data"),
        IMPORT_DATA("import_data"),
        ADD_WIDGET("add_widget"),
        PRIVACY_TERMS("privacy_terms"),
        TERMS_OF_SERVICE("terms_of_service"),
        PRIVACY_POLICY("privacy_policy"),
        SEND_FEEDBACK("send_feedback"),
        WHATS_NEW("whats_new")
    }

    data class ItemClicked(val item: Item) : AnalyticsEvent {
        override val name = "settings_item_clicked"
        override val params = mapOf("item" to item.wireName)
    }
}
