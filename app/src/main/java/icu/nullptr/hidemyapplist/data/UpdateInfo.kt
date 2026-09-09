package icu.nullptr.hidemyapplist.data

import org.json.JSONObject
import java.net.URL

class UpdateInfo(
    val versionName: String,
    val content: String,
    val downloadUrl: String,
)

fun fetchLatestUpdate(): UpdateInfo? = runCatching {
    val jsonText = URL(AppConstants.UPDATE_CHECK_URL).readText()
    val json = JSONObject(jsonText)
    UpdateInfo(
        versionName = json.getString("tag_name"),
        content = json.getString("body"),
        downloadUrl = json.getString("html_url"),
    )
}.getOrNull()
