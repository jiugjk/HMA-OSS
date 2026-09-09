package icu.nullptr.hidemyapplist.common.settings_presets

import android.provider.Settings
import icu.nullptr.hidemyapplist.common.Constants

class DeveloperOptionsPreset : BasePreset(NAME) {
    companion object {
        const val NAME = "dev_options"
    }

    @Suppress("DEPRECATION")
    override val settingsKVPairs = listOf(
        ReplacementItem(
            Settings.Global.ADB_ENABLED,
            "0",
        ),
        ReplacementItem(
            "adb_wifi_enabled",
            "0",
        ),
        ReplacementItem(
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            "0",
        ),
        ReplacementItem(
            "hidden_api_policy",
            null,
        ),
        ReplacementItem(
            Settings.Secure.ALLOW_MOCK_LOCATION,
            "0",
            Constants.SETTINGS_SECURE,
        ),
    )
}
