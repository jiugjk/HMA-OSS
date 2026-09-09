package org.frknkrc44.hma_oss.ui.util

import android.os.Bundle
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import org.frknkrc44.hma_oss.ui.fragment.EditSettingFragmentArgs

fun ReplacementItem.toEditSettingFragmentArgs() = EditSettingFragmentArgs(
    database = database,
    name = name,
    value = value,
)

fun List<ReplacementItem>.targetSettingListToBundle() = Bundle().apply {
    forEach { item ->
        putStringArray("${item.database}\u0000${item.name}", arrayOf(item.value, item.database, item.name))
    }
}

fun Bundle.toTargetSettingList() = keySet().mapNotNull {
    val item = getStringArray(it) ?: return@mapNotNull null
    if (item.size >= 3) {
        ReplacementItem(item[2], item[0], item[1])
    } else {
        ReplacementItem(it, item.getOrNull(0), item.getOrNull(1) ?: return@mapNotNull null)
    }
}
