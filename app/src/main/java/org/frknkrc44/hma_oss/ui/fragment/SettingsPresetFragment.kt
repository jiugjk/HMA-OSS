package org.frknkrc44.hma_oss.ui.fragment

import androidx.navigation.fragment.navArgs
import org.frknkrc44.hma_oss.ui.adapter.SettingsPresetListAdapter

class SettingsPresetFragment : BaseSettingsPTFragment() {
    private val args by lazy { navArgs<SettingsPresetFragmentArgs>() }

    override val adapter by lazy { SettingsPresetListAdapter(args.value.name) }

    override val title by lazy { args.value.title }

    override val menu = null
}
