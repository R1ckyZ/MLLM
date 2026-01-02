package com.example.mllm.device

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Display
import androidx.annotation.RequiresApi
import com.example.mllm.overlay.OverlayController
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

class ScreenshotController(private val context: Context) {
    suspend fun capture(timeoutMs: Long = 2000L): Screenshot {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val accessibilityShot = captureWithAccessibility(timeoutMs)
            if (accessibilityShot != null) {
                return accessibilityShot
            }
        }
        return createFallbackScreenshot(false)
    }

    // Requires API 30+ (AccessibilityService.takeScreenshot).
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun captureWithAccessibility(timeoutMs: Long): Screenshot? {
        val service = DeviceAccessibilityService.instance ?: return null
        return suspendCancellableCoroutine { continuation ->
            var completed = false
            val timeoutHandler = Handler(Looper.getMainLooper())
            OverlayController.hideOverlaysForCapture()
            val overlayHideDelayMs = 120L

            timeoutHandler.postDelayed({
                if (completed || !continuation.isActive) {
                    return@postDelayed
                }
                service.takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    context.mainExecutor,
                    object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshotResult: android.accessibilityservice.AccessibilityService.ScreenshotResult) {
                            if (completed || !continuation.isActive) {
                                return
                            }
                            completed = true
                            timeoutHandler.removeCallbacksAndMessages(null)
                            OverlayController.restoreOverlaysAfterCapture()
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            hardwareBuffer.close()
                            if (hardwareBitmap == null) {
                                continuation.resume(null)
                                return
                            }
                            val bitmap =
                                if (hardwareBitmap.config == Bitmap.Config.HARDWARE) {
                                    hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                } else {
                                    hardwareBitmap
                                }
                            if (bitmap !== hardwareBitmap) {
                                hardwareBitmap.recycle()
                            }
                            val base64 = bitmapToBase64(bitmap)
                            val width = bitmap.width
                            val height = bitmap.height
                            bitmap.recycle()
                            continuation.resume(Screenshot(base64, width, height, isSensitive = false))
                        }

                        override fun onFailure(errorCode: Int) {
                            if (completed || !continuation.isActive) {
                                return
                            }
                            completed = true
                            timeoutHandler.removeCallbacksAndMessages(null)
                            OverlayController.restoreOverlaysAfterCapture()
                            continuation.resume(null)
                        }
                    },
                )
            }, overlayHideDelayMs)

            timeoutHandler.postDelayed({
                if (!completed && continuation.isActive) {
                    completed = true
                    OverlayController.restoreOverlaysAfterCapture()
                    continuation.resume(null)
                }
            }, timeoutMs)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        val bytes = output.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun createFallbackScreenshot(isSensitive: Boolean): Screenshot {
        val width = 1080
        val height = 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLACK)
        val base64 = bitmapToBase64(bitmap)
        bitmap.recycle()
        return Screenshot(base64, width, height, isSensitive)
    }
}
