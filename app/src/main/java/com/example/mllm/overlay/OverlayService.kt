package com.example.mllm.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.mllm.LANGUAGE_CODE_EN
import com.example.mllm.LANGUAGE_CODE_ZH
import com.example.mllm.MainActivity
import com.example.mllm.R
import com.example.mllm.baidu.BaiduAsrClient
import com.example.mllm.baidu.BaiduWakeupClient
import com.example.mllm.createLocalizedContext
import com.example.mllm.device.DeviceAccessibilityService

class OverlayService : Service() {
    private val notificationId = 1001
    private val channelId = "mllm_wakeup"
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: FrameLayout
    private lateinit var panelView: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var cancelButton: TextView
    private lateinit var messagesScroll: ScrollView
    private lateinit var messagesContainer: LinearLayout
    private lateinit var inputRow: FrameLayout
    private lateinit var inputField: EditText
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null
    private var panelLayoutParams: WindowManager.LayoutParams? = null
    private var asrClient: BaiduAsrClient? = null
    private var wakeupClient: BaiduWakeupClient? = null
    private var wakeupRunning = false
    private var bubbleSizeDp = 56

    private fun currentLanguageTag(): String {
        val configured = OverlayController.languageCode
        if (configured.isNotBlank()) {
            return configured
        }
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) "" else locales[0]?.toLanguageTag().orEmpty()
        return tag
    }

    private fun localizedContext() = createLocalizedContext(this, currentLanguageTag())

    fun updateLanguage(code: String) {
        OverlayController.languageCode = code
        refreshTexts()
    }

    // Requires API 26+ (TYPE_APPLICATION_OVERLAY window type).
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubbleView = buildBubble()
        panelView = buildPanel()
        bubbleLayoutParams = buildBubbleParams()
        panelLayoutParams = buildPanelParams()
        windowManager.addView(bubbleView, bubbleLayoutParams)
        windowManager.addView(panelView, panelLayoutParams)
        panelView.visibility = View.GONE
        asrClient = BaiduAsrClient(applicationContext)
        wakeupClient =
            BaiduWakeupClient(applicationContext).apply {
                onWakeup = { word ->
                    showPanel(
                        localizedContext().getString(R.string.overlay_wakeup_prefix, word),
                    )
                    startRecognition(showPanel = true, restorePanelOnFinish = true)
                }
                onError = { error ->
                    showPanel(error)
                }
            }
        OverlayController.bind(this)
        refreshTexts()
        if (OverlayController.wakeupEnabled) {
            setWakeupEnabled(true)
        }
    }

    override fun onDestroy() {
        OverlayController.unbind(this)
        asrClient?.release()
        wakeupClient?.release()
        asrClient = null
        wakeupClient = null
        stopForegroundCompat()
        try {
            windowManager.removeView(bubbleView)
            windowManager.removeView(panelView)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun showPanel(text: String) {
        statusText.text = text
    }

    fun updatePanel(text: String) {
        statusText.text = text
    }

    fun appendMessage(
        role: String,
        text: String,
    ) {
        val row =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = if (role == "user") Gravity.END else Gravity.START
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(6)
                    }
            }
        val bubbleColor = if (role == "user") 0xFF95EC69.toInt() else 0xFFF2F3F5.toInt()
        val textColor = 0xFF111111.toInt()
        val bubble =
            TextView(this).apply {
                this.text = text
                setTextColor(textColor)
                textSize = 14f
                setPadding(dp(12), dp(8), dp(12), dp(8))
                maxWidth = (resources.displayMetrics.widthPixels * 0.7f).toInt()
                background =
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(bubbleColor)
                    }
                setTextIsSelectable(true)
            }
        row.addView(bubble)
        messagesContainer.addView(row)
        messagesScroll.post { messagesScroll.fullScroll(View.FOCUS_DOWN) }
    }

    fun clearMessages() {
        messagesContainer.removeAllViews()
    }

    fun setCancelVisible(visible: Boolean) {
        cancelButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun refreshTexts() {
        val localized = localizedContext()
        titleText.text = localized.getString(R.string.overlay_title)
        subtitleText.text = localized.getString(R.string.overlay_subtitle)
        cancelButton.text = localized.getString(R.string.overlay_cancel)
        inputField.hint = localized.getString(R.string.overlay_input_hint)
        val readyEn =
            createLocalizedContext(this, LANGUAGE_CODE_EN)
                .getString(R.string.overlay_ready)
        val readyZh =
            createLocalizedContext(this, LANGUAGE_CODE_ZH)
                .getString(R.string.overlay_ready)
        val accessibilityEn =
            createLocalizedContext(this, LANGUAGE_CODE_EN)
                .getString(R.string.overlay_accessibility_required)
        val accessibilityZh =
            createLocalizedContext(this, LANGUAGE_CODE_ZH)
                .getString(R.string.overlay_accessibility_required)
        val currentStatus = statusText.text?.toString().orEmpty()
        if (currentStatus.isBlank() ||
            currentStatus == readyEn ||
            currentStatus == readyZh ||
            currentStatus == accessibilityEn ||
            currentStatus == accessibilityZh
        ) {
            val updated =
                if (
                    currentStatus == accessibilityEn ||
                    currentStatus == accessibilityZh
                ) {
                    localized.getString(R.string.overlay_accessibility_required)
                } else {
                    localized.getString(R.string.overlay_ready)
                }
            statusText.text = updated
        }
    }

    fun hidePanel() {
        panelView.translationY = 0f
        panelView.visibility = View.GONE
    }

    fun setBubbleVisible(visible: Boolean) {
        bubbleView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setBubbleSizeDp(sizeDp: Int) {
        val clamped = sizeDp.coerceIn(32, 96)
        bubbleSizeDp = clamped
        val params = bubbleLayoutParams ?: return
        params.width = dp(clamped)
        params.height = dp(clamped)
        try {
            windowManager.updateViewLayout(bubbleView, params)
        } catch (_: Exception) {
        }
    }

    fun setPanelVisible(visible: Boolean) {
        if (visible) {
            panelView.translationY = 0f
        }
        panelView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun isBubbleVisible(): Boolean = bubbleView.visibility == View.VISIBLE

    fun isPanelVisible(): Boolean = panelView.visibility == View.VISIBLE

    fun setWakeupEnabled(enabled: Boolean) {
        val config = OverlayController.baiduConfig
        if (config.appId.isBlank() || config.appKey.isBlank() || config.secretKey.isBlank()) {
            showPanel(localizedContext().getString(R.string.overlay_baidu_config_missing))
            return
        }
        if (enabled && !wakeupRunning) {
            ensureForeground()
            wakeupClient?.config = config
            wakeupClient?.start()
            wakeupRunning = true
        } else if (!enabled && wakeupRunning) {
            wakeupClient?.stop()
            wakeupRunning = false
            stopForegroundCompat()
        }
    }

    private fun buildBubble(): FrameLayout {
        val view =
            FrameLayout(this).apply {
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0xFFFFFFFF.toInt())
                    }
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
            }
        val icon =
            ImageView(this).apply {
                setImageResource(com.example.mllm.R.drawable.ic_overlay_bubble)
                scaleType = ImageView.ScaleType.CENTER_CROP
                scaleX = 1.08f
                scaleY = 1.08f
            }
        view.addView(
            icon,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        view.setOnClickListener {
            if (panelView.visibility == View.VISIBLE) {
                panelView.visibility = View.GONE
            } else {
                panelView.translationY = 0f
                panelView.visibility = View.VISIBLE
            }
        }
        view.setOnTouchListener(BubbleTouchListener())
        return view
    }

    private fun buildPanel(): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.background =
            GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(0xFFFFFFFF.toInt())
            }
        layout.setPadding(dp(16), dp(12), dp(16), dp(12))

        val handle =
            FrameLayout(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(24),
                    ).apply {
                        bottomMargin = dp(4)
                    }
            }
        val handleBar =
            View(this).apply {
                layoutParams =
                    FrameLayout.LayoutParams(dp(44), dp(4)).apply {
                        gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.CENTER_VERTICAL
                    }
                background =
                    GradientDrawable().apply {
                        cornerRadius = dp(2).toFloat()
                        setColor(0xFFDDDDDD.toInt())
                    }
            }
        handle.addView(handleBar)
        val closeThreshold = dp(80).toFloat()
        var dragStartY = 0f
        var dragStartTranslation = 0f
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartY = event.rawY
                    dragStartTranslation = layout.translationY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = event.rawY - dragStartY
                    layout.translationY = (dragStartTranslation + delta).coerceAtLeast(0f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (layout.translationY > closeThreshold) {
                        layout.translationY = 0f
                        layout.visibility = View.GONE
                    } else {
                        layout.animate()
                            .translationY(0f)
                            .setDuration(160)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }

        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dp(8)
                    }
            }

        val titleGroup =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

        titleText =
            TextView(this).apply {
                text = localizedContext().getString(R.string.overlay_title)
                setTextColor(0xFF111111.toInt())
                textSize = 16f
            }

        subtitleText =
            TextView(this).apply {
                text = localizedContext().getString(R.string.overlay_subtitle)
                setTextColor(0xFF777777.toInt())
                textSize = 12f
            }

        titleGroup.addView(titleText)
        titleGroup.addView(subtitleText)
        header.addView(titleGroup)

        cancelButton =
            TextView(this).apply {
                text = localizedContext().getString(R.string.overlay_cancel)
                setTextColor(0xFFD32F2F.toInt())
                textSize = 12f
                setPadding(dp(8), dp(6), dp(8), dp(6))
                visibility = View.GONE
                setOnClickListener {
                    OverlayController.onTaskCancelRequested?.invoke()
                }
            }
        header.addView(cancelButton)

        statusText =
            TextView(this).apply {
                setTextColor(0xFF666666.toInt())
                textSize = 12f
                text = localizedContext().getString(R.string.overlay_ready)
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                setSingleLine(false)
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                setTextIsSelectable(true)
            }

        messagesContainer =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }

        messagesScroll =
            ScrollView(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f,
                    )
                addView(messagesContainer)
            }

        inputField =
            EditText(this).apply {
                hint = localizedContext().getString(R.string.overlay_input_hint)
                setSingleLine(true)
                imeOptions = EditorInfo.IME_ACTION_SEND
                background =
                    GradientDrawable().apply {
                        cornerRadius = dp(18).toFloat()
                        setColor(0xFFF2F3F5.toInt())
                    }
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        val text = text?.toString()?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            val accessibilityEnabled = DeviceAccessibilityService.instance != null
                            OverlayController.onTaskRequested?.invoke(text)
                            if (accessibilityEnabled) {
                                OverlayController.requestHidePanelAfterSend()
                                OverlayController.markPanelHiddenForTask()
                                setText("")
                            }
                        }
                        true
                    } else {
                        false
                    }
                }
            }

        val micButton =
            ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                background = null
                setPadding(0, 0, 0, 0)
                setColorFilter(0xFF9E9E9E.toInt())
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startRecognition(showPanel = true, restorePanelOnFinish = true)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            asrClient?.stop()
                            true
                        }
                        else -> false
                    }
                }
            }

        inputRow =
            FrameLayout(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(8)
                    }
                addView(
                    inputField,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    micButton,
                    FrameLayout.LayoutParams(
                        dp(36),
                        dp(36),
                        Gravity.END or Gravity.CENTER_VERTICAL,
                    ).apply {
                        rightMargin = dp(10)
                    },
                )
            }

        inputField.setPadding(dp(12), dp(8), dp(34), dp(8))
        inputField.post {
            val baseHeight = inputField.height.coerceAtLeast(dp(36))
            val iconSize = (baseHeight * 0.55f).toInt().coerceIn(dp(18), dp(24))
            val touchSize = (iconSize + dp(12)).coerceAtLeast(dp(36))
            val rightPadding = touchSize + dp(10)
            inputField.setPadding(dp(12), dp(8), rightPadding, dp(8))
            val params = micButton.layoutParams as? FrameLayout.LayoutParams ?: return@post
            params.width = touchSize
            params.height = touchSize
            params.rightMargin = dp(10)
            micButton.layoutParams = params
            val padding = ((touchSize - iconSize) / 2).coerceAtLeast(0)
            micButton.setPadding(padding, padding, padding, padding)
        }

        layout.addView(handle)
        layout.addView(header)
        layout.addView(statusText)
        layout.addView(messagesScroll)
        layout.addView(inputRow)
        return layout
    }

    // Requires API 26+ (TYPE_APPLICATION_OVERLAY).
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildBubbleParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            dp(bubbleSizeDp),
            dp(bubbleSizeDp),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 300
        }
    }

    // Requires API 26+ (TYPE_APPLICATION_OVERLAY).
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPanelParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            panelHeight(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }
    }

    private fun panelHeight(): Int {
        return (resources.displayMetrics.heightPixels * 0.5f).toInt()
    }

    private fun startRecognition(
        showPanel: Boolean = false,
        restorePanelOnFinish: Boolean = true,
    ) {
        val config = OverlayController.baiduConfig
        if (config.appId.isBlank() || config.appKey.isBlank() || config.secretKey.isBlank()) {
            showPanel(localizedContext().getString(R.string.overlay_baidu_config_missing))
            return
        }
        val permission =
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO,
            )
        if (permission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            showPanel(localizedContext().getString(R.string.overlay_mic_permission_required))
            return
        }
        if (showPanel) {
            setPanelVisible(true)
        }
        showPanel(localizedContext().getString(R.string.overlay_listening))
        asrClient?.config = config
        asrClient?.start(
            onPartial = { text ->
                showPanel(text)
            },
            onFinal = { text ->
                showPanel(text)
                val task = text.trim()
                if (task.isNotEmpty()) {
                    asrClient?.stop()
                    if (restorePanelOnFinish) {
                        OverlayController.requestHidePanelAfterSend()
                        OverlayController.markPanelHiddenForTask()
                    }
                    OverlayController.onTaskRequested?.invoke(task)
                }
            },
            onError = { error ->
                showPanel(error)
            },
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun ensureForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val intent = Intent(this, MainActivity::class.java)
        val pending =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val localized = localizedContext()
        val notification =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(localized.getString(R.string.overlay_wakeup_listening_title))
                .setContentText(localized.getString(R.string.notification_wakeup_running))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pending)
                .build()
        startForeground(notificationId, notification)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForegroundModern()
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    // Requires API 24+ (stopForeground with flags).
    @RequiresApi(Build.VERSION_CODES.N)
    private fun stopForegroundModern() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // Requires API 26+ (NotificationChannel).
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) != null) return
        val channel =
            NotificationChannel(
                channelId,
                localizedContext().getString(R.string.overlay_wakeup_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
        channel.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(channel)
    }

    private inner class BubbleTouchListener : View.OnTouchListener {
        private var lastX = 0
        private var lastY = 0
        private var startX = 0
        private var startY = 0
        private var moved = false

        override fun onTouch(
            view: View,
            event: MotionEvent,
        ): Boolean {
            val params = bubbleLayoutParams ?: return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    moved = false
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    startX = params.x
                    startY = params.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    if (dx != 0 || dy != 0) {
                        moved = true
                        params.x = startX + dx
                        params.y = startY + dy
                        windowManager.updateViewLayout(bubbleView, params)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        view.performClick()
                    }
                    return true
                }
            }
            return false
        }
    }
}
