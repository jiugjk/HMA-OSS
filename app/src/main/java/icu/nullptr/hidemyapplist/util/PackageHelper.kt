package icu.nullptr.hidemyapplist.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import icu.nullptr.hidemyapplist.MyApp.Companion.hmaApp
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.asDrawable
import icu.nullptr.hidemyapplist.ui.util.asComponentName
import icu.nullptr.hidemyapplist.util.ConfigUtils.Companion.getLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R
import java.text.Collator

object PackageHelper {
    const val TAG = "PackageHelper"

    class PackageCache(
        val info: PackageInfo,
        val label: String,
        val icon: Drawable,
        val userIds: HashSet<Int>,
    )

    object Comparators {
        val byLabel = Comparator<String> { o1, o2 ->
            try {
                val locale = getLocale()
                val n1 = loadAppLabel(o1).lowercase(locale)
                val n2 = loadAppLabel(o2).lowercase(locale)
                Collator.getInstance(locale).compare(n1, n2)
            } catch (_: Throwable) {
                byPackageName.compare(o1, o2)
            }
        }
        val byPackageName = Comparator<String> { o1, o2 ->
            val locale = getLocale()
            val n1 = o1.lowercase(locale)
            val n2 = o2.lowercase(locale)
            Collator.getInstance(locale).compare(n1, n2)
        }
        val byInstallTime = Comparator<String> { o1, o2 ->
            try {
                val n1 = loadPackageInfo(o1).firstInstallTime
                val n2 = loadPackageInfo(o2).firstInstallTime
                n2.compareTo(n1)
            } catch (_: Throwable) {
                byPackageName.compare(o1, o2)
            }
        }
        val byUpdateTime = Comparator<String> { o1, o2 ->
            try {
                val n1 = loadPackageInfo(o1).lastUpdateTime
                val n2 = loadPackageInfo(o2).lastUpdateTime
                n2.compareTo(n1)
            } catch (_: Throwable) {
                byPackageName.compare(o1, o2)
            }
        }
    }

    private val packageCache = MutableStateFlow<Map<String, PackageCache>>(emptyMap())
    private val installedPackages = MutableStateFlow<Set<String>>(emptySet())
    val appList = MutableStateFlow<List<String>>(emptyList())

    val isRefreshing = MutableStateFlow(false)

    val refreshing get() = isRefreshing.value

    private fun userIdOf(userHandle: UserHandle): Int {
        return runCatching {
            UserHandle::class.java.getMethod("getIdentifier").invoke(userHandle) as Int
        }.getOrElse { userHandle.hashCode() }
    }

    init {
        invalidateCache()
    }

    fun invalidateCache(
        onFinished: ((Throwable?) -> Unit)? = null
    ) {
        hmaApp.globalScope.launch {
            isRefreshing.value = true
            try {
                val cache = withContext(Dispatchers.IO) {
                    val pm = hmaApp.packageManager
                    val um = hmaApp.getSystemService(Context.USER_SERVICE) as UserManager
                    val profiles = um.userProfiles
                    val installed = mutableSetOf<String>()
                    var enumerated = false

                    mutableMapOf<String, PackageCache>().also { cacheMap ->
                        for (userProfile: UserHandle in profiles) {
                            val userId = userIdOf(userProfile)
                            val packages = ServiceClient.getPackageNames(userId) ?: continue
                            enumerated = true
                            installed.addAll(packages)
                            for (packageName in packages) {
                                if (packageName in Constants.packagesShouldNotHide) continue
                                val packageInfo = try {
                                    ServiceClient.getPackageInfo(packageName, userId)!!
                                } catch (e: Throwable) {
                                    ServiceClient.log(Log.DEBUG, TAG,
                                        "Cannot get package details for $packageName\n${e.stackTraceToString()}")
                                    continue
                                }
                                packageInfo.applicationInfo?.let { appInfo ->
                                    val label = pm.getApplicationLabel(appInfo).toString()
                                    val icon = loadAppIconFromAppInfo(appInfo)
                                    if (cacheMap.containsKey(packageName)) {
                                        cacheMap[packageName]?.userIds?.add(userId)
                                    } else {
                                        cacheMap[packageName] = PackageCache(
                                            packageInfo,
                                            label,
                                            icon,
                                            HashSet<Int>().apply { add(userId) }
                                        )
                                    }
                                }
                            }
                        }
                        if (!enumerated) {
                            error("Unable to enumerate installed packages")
                        }
                        installedPackages.value = installed
                    }
                }
                packageCache.value = cache
                appList.value = cache.keys.toList()
            } catch (t: Throwable) {
                ServiceClient.log(Log.ERROR, TAG, t.stackTraceToString())
                throw t
            } finally {
                isRefreshing.value = false
            }
        }.apply {
            if (onFinished != null) {
                invokeOnCompletion(onFinished)
            }
        }
    }

    suspend fun sortList(firstComparator: Comparator<String>) {
        var comparator = when (PrefManager.appFilter_sortMethod) {
            PrefManager.SortMethod.BY_LABEL -> Comparators.byLabel
            PrefManager.SortMethod.BY_PACKAGE_NAME -> Comparators.byPackageName
            PrefManager.SortMethod.BY_INSTALL_TIME -> Comparators.byInstallTime
            PrefManager.SortMethod.BY_UPDATE_TIME -> Comparators.byUpdateTime
        }
        if (PrefManager.appFilter_reverseOrder) comparator = comparator.reversed()
        val list = appList.value.sortedWith(firstComparator.then(comparator))
        appList.value = list
    }

    fun exists(packageName: String) = installedPackages.value.contains(packageName)

    fun loadPackageInfo(packageName: String): PackageInfo =
        packageCache.value[packageName]!!.info

    fun loadAppLabel(packageName: String): String =
        packageCache.value[packageName]?.label ?: packageName

    fun loadAppIcon(packageName: String): Drawable =
        packageCache.value[packageName]?.icon ?: android.R.drawable.sym_def_app_icon.asDrawable(hmaApp)

    fun loadUserIds(packageName: String): Set<Int> =
        packageCache.value[packageName]?.userIds ?: setOf()

    fun isSystem(packageName: String): Boolean {
        val flags = packageCache.value[packageName]?.info?.applicationInfo?.flags ?: return false
        return flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    fun loadAppIconFromAppInfo(appInfo: ApplicationInfo): Drawable {
        return if (appInfo.packageName == BuildConfig.APPLICATION_ID) {
            val activityName = findEnabledAppComponent(hmaApp)
            if (activityName == null) {
                R.mipmap.ic_launcher.asDrawable(hmaApp)
            } else {
                hmaApp.packageManager.getActivityIcon(activityName)
            }
        } else {
            try {
                appInfo.loadIcon(hmaApp.packageManager)
            } catch (x: Throwable) {
                ServiceClient.log(Log.ERROR, TAG, x.stackTraceToString())

                return ResourcesCompat.getDrawable(
                    hmaApp.resources,
                    android.R.drawable.sym_def_app_icon,
                    hmaApp.theme,
                )!!
            }
        }
    }

    fun findEnabledAppComponent(context: Context): ComponentName? {
        with (context.packageManager) {
            val pkgInfo =  getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_ACTIVITIES)!!

            return pkgInfo.activities?.firstOrNull { it.targetActivity != null }?.asComponentName()
        }
    }
}
