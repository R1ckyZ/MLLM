package com.example.mllm.device

import android.accessibilityservice.GestureDescription
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

object DeviceController {
    suspend fun getCurrentApp(context: Context): String {
        val packageName =
            DeviceAccessibilityService.instance
                ?.rootInActiveWindow
                ?.packageName
                ?.toString()
                ?: getTopPackageFromUsageStats(context)

        return if (packageName == null) {
            "System Home"
        } else {
            AppPackages.appNameForPackage(packageName) ?: "System Home"
        }
    }

    suspend fun tap(
        x: Int,
        y: Int,
        delayMs: Long? = null,
    ) {
        val service = requireService()
        service.dispatchGesture(buildTapGesture(x, y, 1L), null, null)
        delay(delayMs ?: DeviceTimingConfig.defaultTapDelayMs)
    }

    suspend fun doubleTap(
        x: Int,
        y: Int,
        delayMs: Long? = null,
    ) {
        val service = requireService()
        service.dispatchGesture(buildTapGesture(x, y, 1L), null, null)
        delay(DeviceTimingConfig.doubleTapIntervalMs)
        service.dispatchGesture(buildTapGesture(x, y, 1L), null, null)
        delay(delayMs ?: DeviceTimingConfig.defaultDoubleTapDelayMs)
    }

    suspend fun longPress(
        x: Int,
        y: Int,
        durationMs: Long = 3000L,
        delayMs: Long? = null,
    ) {
        val service = requireService()
        service.dispatchGesture(buildTapGesture(x, y, durationMs), null, null)
        delay(delayMs ?: DeviceTimingConfig.defaultLongPressDelayMs)
    }

    suspend fun swipe(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Long? = null,
        delayMs: Long? = null,
    ) {
        val service = requireService()
        val computedDuration = durationMs ?: computeSwipeDurationMs(startX, startY, endX, endY)
        service.dispatchGesture(
            buildSwipeGesture(startX, startY, endX, endY, computedDuration),
            null,
            null,
        )
        delay(delayMs ?: DeviceTimingConfig.defaultSwipeDelayMs)
    }

    suspend fun back(delayMs: Long? = null) {
        val service = requireService()
        service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        delay(delayMs ?: DeviceTimingConfig.defaultBackDelayMs)
    }

    suspend fun home(delayMs: Long? = null) {
        val service = requireService()
        service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
        delay(delayMs ?: DeviceTimingConfig.defaultHomeDelayMs)
    }

    suspend fun launchApp(
        context: Context,
        appName: String,
        delayMs: Long? = null,
    ): Boolean {
        val packageName = AppPackages.packageFor(appName) ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        delay(delayMs ?: DeviceTimingConfig.defaultLaunchDelayMs)
        return true
    }

    private fun requireService(): DeviceAccessibilityService {
        return checkNotNull(DeviceAccessibilityService.instance) {
            "DeviceAccessibilityService is not connected. Enable accessibility service first."
        }
    }

    private fun buildTapGesture(
        x: Int,
        y: Int,
        durationMs: Long,
    ): GestureDescription {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, max(1L, durationMs))
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    private fun buildSwipeGesture(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Long,
    ): GestureDescription {
        val path =
            Path().apply {
                moveTo(startX.toFloat(), startY.toFloat())
                lineTo(endX.toFloat(), endY.toFloat())
            }
        val stroke = GestureDescription.StrokeDescription(path, 0, max(1L, durationMs))
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    private fun computeSwipeDurationMs(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
    ): Long {
        val dx = (startX - endX).toLong()
        val dy = (startY - endY).toLong()
        val distSq = dx * dx + dy * dy
        val estimated = distSq / 1000L
        return max(1000L, min(estimated, 2000L))
    }

    private fun getTopPackageFromUsageStats(context: Context): String? {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10 * 60 * 1000
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        if (stats.isNullOrEmpty()) {
            return null
        }
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}
