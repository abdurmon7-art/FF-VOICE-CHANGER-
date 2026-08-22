package com.example.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val launchIntent: Intent?
)

object InstalledAppsManager {

    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            // Skip own package
            if (packageName == context.packageName) continue

            try {
                val appName = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                list.add(AppInfo(appName, packageName, icon, launchIntent))
            } catch (e: Exception) {
                // ignore
            }
        }

        list.sortedBy { it.appName.lowercase() }
    }
}
