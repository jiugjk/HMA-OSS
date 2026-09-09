package org.frknkrc44.hma_oss.zygote.service

import icu.nullptr.hidemyapplist.common.FilterHolder
import icu.nullptr.hidemyapplist.common.PresetCache
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils
import java.util.concurrent.ConcurrentHashMap

class HMAServiceDataHolder {
    private val filterCountLock = Any()

    private val uidHideCache = ConcurrentHashMap<Int, UidHideCache>()

    var presetCache = PresetCache()
        internal set

    var filterHolder = FilterHolder()
        internal set

    private class UidHideCache(
        val caller: String,
        val queries: MutableSet<String> = ConcurrentHashMap.newKeySet(),
    )

    fun addIntoPresetCache(preset: String, packageName: String): Boolean {
        var returnedValue = false

        returnedValue = returnedValue or (presetCache.cache[preset]?.add(packageName) ?: false)
        if (RiskyPackageUtils.instance.appHasGMSConnection(packageName, true)) {
            returnedValue = returnedValue or presetCache.riskyPackageCache.add(packageName)
        }

        return returnedValue
    }

    fun removeFromPresetCache(preset: String, packageName: String): Boolean {
        var returnedValue = false

        returnedValue = returnedValue or (presetCache.cache[preset]?.remove(packageName) ?: false)
        returnedValue = returnedValue or presetCache.riskyPackageCache.remove(packageName)

        return returnedValue
    }

    fun findCallerByUid(uid: Int) = uidHideCache[uid]?.caller

    fun shouldHideFromUid(uid: Int, query: String?): Boolean? {
        if (query == null) return null
        return uidHideCache[uid]?.queries?.contains(query) == true
    }

    fun putShouldHideUidCache(uid: Int, caller: String, query: String) {
        uidHideCache.getOrPut(uid) { UidHideCache(caller) }.queries.add(query)
    }

    fun clearUidCache() = uidHideCache.clear()

    fun retainFilterCounts(validKeys: Set<String>) {
        synchronized(filterCountLock) {
            filterHolder.filterCounts.keys.retainAll(validKeys)
        }
    }

    fun clearFilterCounts() {
        synchronized(filterCountLock) {
            filterHolder.filterCounts.clear()
        }
    }

    fun snapshotFilterHolder(force: Boolean = true): String? {
        synchronized(filterCountLock) {
            if (!force && filterHolder.totalCount % 100 != 0) {
                return null
            }
            return filterHolder.toString()
        }
    }

    fun increaseFilterCount(
        uid: Int?,
        amount: Int = 1,
        filterType: FilterHolder.FilterType,
        writeFilterCount: () -> Unit,
    ) {
        if (uid == null || amount < 1) return

        val caller = findCallerByUid(uid) ?: return

        return increaseFilterCount(caller, amount, filterType, writeFilterCount)
    }

    fun increaseFilterCount(
        caller: String?,
        amount: Int = 1,
        filterType: FilterHolder.FilterType,
        writeFilterCount: () -> Unit,
    ) {
        if (caller == null || amount < 1) return

        synchronized(filterCountLock) {
            val filterCount = filterHolder.filterCounts.getOrPut(caller) { FilterHolder.FilterCount() }
            when (filterType) {
                FilterHolder.FilterType.PACKAGE_MANAGER -> filterCount.packageManagerCount += amount
                FilterHolder.FilterType.ACTIVITY_LAUNCH -> filterCount.activityLaunchCount += amount
                FilterHolder.FilterType.INSTALLER -> filterCount.installerCount += amount
                FilterHolder.FilterType.SETTINGS -> filterCount.settingsCount += amount
                FilterHolder.FilterType.OTHERS -> filterCount.othersCount += amount
            }
        }

        writeFilterCount()
    }
}
