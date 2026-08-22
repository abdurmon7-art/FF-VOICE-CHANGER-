package com.example.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppCategory(val title: String) {
    VOIP_COMMUNICATION("Voice & Messaging"),
    GAMING("Games & Multiplayer"),
    STREAMING_RECORDING("Recording & Streaming"),
    SOCIAL_MEDIA("Social & Video"),
    GENERAL("General App")
}

data class AppRoutingCompatibility(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val launchIntent: Intent?,
    val category: AppCategory,
    val directMicInjectionSupported: Boolean = false, // Always false on standard unrooted Android
    val supportedOfficialMethods: List<String>,
    val recommendationSummary: String,
    val technicalExplanation: String
)

object AppCompatibilityManager {

    suspend fun getAppsWithCompatibility(context: Context): List<AppRoutingCompatibility> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<AppRoutingCompatibility>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName == context.packageName) continue

            try {
                val appName = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                val category = categorizeApp(packageName, appName)

                val (methods, summary, tech) = getRoutingProfile(category, appName)

                list.add(
                    AppRoutingCompatibility(
                        appName = appName,
                        packageName = packageName,
                        icon = icon,
                        launchIntent = launchIntent,
                        category = category,
                        directMicInjectionSupported = false,
                        supportedOfficialMethods = methods,
                        recommendationSummary = summary,
                        technicalExplanation = tech
                    )
                )
            } catch (e: Exception) {
                // ignore
            }
        }

        list.sortedBy { it.appName.lowercase() }
    }

    private fun categorizeApp(packageName: String, appName: String): AppCategory {
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase()

        return when {
            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("discord") ||
            lowerPkg.contains("skype") || lowerPkg.contains("zoom") || lowerPkg.contains("meet") ||
            lowerPkg.contains("teams") || lowerPkg.contains("viber") || lowerPkg.contains("signal") ||
            lowerPkg.contains("messenger") || lowerName.contains("chat") || lowerName.contains("call") -> {
                AppCategory.VOIP_COMMUNICATION
            }
            lowerPkg.contains("pubg") || lowerPkg.contains("freefire") || lowerPkg.contains("cod") ||
            lowerPkg.contains("roblox") || lowerPkg.contains("minecraft") || lowerPkg.contains("game") ||
            lowerPkg.contains("fortnite") || lowerPkg.contains("genshin") || lowerPkg.contains("clash") -> {
                AppCategory.GAMING
            }
            lowerPkg.contains("screenrecorder") || lowerPkg.contains("recorder") || lowerPkg.contains("obs") ||
            lowerPkg.contains("stream") || lowerPkg.contains("twitch") || lowerPkg.contains("mobizen") ||
            lowerPkg.contains("azrecorder") -> {
                AppCategory.STREAMING_RECORDING
            }
            lowerPkg.contains("instagram") || lowerPkg.contains("tiktok") || lowerPkg.contains("snapchat") ||
            lowerPkg.contains("youtube") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
            lowerPkg.contains("reddit") -> {
                AppCategory.SOCIAL_MEDIA
            }
            else -> AppCategory.GENERAL
        }
    }

    private fun getRoutingProfile(category: AppCategory, appName: String): Triple<List<String>, String, String> {
        val androidLimitationNotice = "Standard Android (unrooted) strictly enforces sandbox isolation for AudioRecord. Apps cannot inject synthetic audio directly into another app's hardware mic stream."

        return when (category) {
            AppCategory.VOIP_COMMUNICATION -> Triple(
                listOf(
                    "1-Tap Voice Note Share: Record with effect and share directly to $appName",
                    "Headset Passthrough: Live monitor with low latency over wired/Bluetooth headset",
                    "TRRS / AUX Loopback: Connect splitter cable to route output into mic input jack",
                    "Floating Controller: Instant on-screen overlay to change voices or record clips"
                ),
                "Supported via 1-Tap Voice Message Share & Headset Passthrough Loopback.",
                "$androidLimitationNotice For messaging, saving and sharing a transformed voice note is the official Android mechanism. For live voice calls, use Headset Passthrough or an AUX splitter."
            )
            AppCategory.GAMING -> Triple(
                listOf(
                    "Live Headset Passthrough: Speak with effect played aloud to gaming mic",
                    "Bluetooth SCO / AUX Cable: Feed transformed voice via audio loopback cable",
                    "Floating Game Overlay: Quick floating bubble to switch voice FX during gameplay",
                    "Soundboard & Clip Trigger: Play pre-recorded funny voice lines"
                ),
                "Supported via Live Passthrough, Floating In-Game Bubble & AUX Loopback.",
                "$androidLimitationNotice To broadcast changed voice into game voice chat, use Live Headset Passthrough near your mic or connect a standard 3.5mm TRRS audio loopback cable."
            )
            AppCategory.STREAMING_RECORDING -> Triple(
                listOf(
                    "Internal Audio Playback Capture (Android 10+): $appName can capture Voice Changer output directly",
                    "Live Mic + FX Output: Screen recorder captures combined game & transformed voice",
                    "Floating Control Overlay: Toggle effects while recording screen"
                ),
                "Fully Supported via Android 10+ Internal Audio Capture & Live Playback.",
                "Android 10+ supports AudioPlaybackCapture. When Voice Changer streams audio, screen recorders capturing internal media audio will capture your transformed voice cleanly."
            )
            AppCategory.SOCIAL_MEDIA -> Triple(
                listOf(
                    "Voice Clip Export & Attach: Export transformed WAV/audio to upload in reels/posts",
                    "Live Camera Passthrough: Play transformed voice while recording video",
                    "Floating Controller: Quick access while filming"
                ),
                "Supported via Exported Audio Clips and Live Background Audio.",
                "$androidLimitationNotice Export audio clips with any of the 13+ voice effects to attach directly in video editors or stories."
            )
            AppCategory.GENERAL -> Triple(
                listOf(
                    "Floating Companion Bubble: Control voice effects anywhere on screen",
                    "Background Audio Processing: Real-time DSP engine runs in foreground service",
                    "Share Audio Clips: Send voice clips to any installed app"
                ),
                "Supported via Floating Companion & Audio Sharing.",
                androidLimitationNotice
            )
        }
    }
}
