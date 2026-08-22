package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.VoiceChangerApp
import com.example.audio.AudioEffectParams
import com.example.audio.VoiceEffectType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class VoiceOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isExpanded = false

    private lateinit var app: VoiceChangerApp

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        app = VoiceChangerApp.instance
        startForegroundServiceNotification()
        setupFloatingView()
    }

    private fun startForegroundServiceNotification() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, VoiceChangerApp.CHANNEL_ID_OVERLAY)
            .setContentTitle("Voice Changer Floating Active")
            .setContentText("Tap to open Voice Changer or use the floating bubble")
            .setSmallIcon(R.drawable.app_voice_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun setupFloatingView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        // Programmatically build modern floating view with dark translucent card styling
        val context = this
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        // Bubble Button
        val bubbleButton = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
            setPadding(24, 20, 28, 20)
        }

        val iconView = ImageView(context).apply {
            setImageResource(R.drawable.app_voice_icon)
            val sizePx = (38 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
        }

        val titleView = TextView(context).apply {
            text = " FX"
            setTextColor(0xFFE2E8F0.toInt())
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        bubbleButton.addView(iconView)
        bubbleButton.addView(titleView)

        // Expanded Panel
        val expandedPanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE0F172A.toInt())
            setPadding(28, 24, 28, 24)
            visibility = View.GONE
        }

        val panelHeader = TextView(context).apply {
            text = "Voice Changer Quick Control"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        expandedPanel.addView(panelHeader)

        // Horizontal ScrollView with preset buttons
        val scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        val effectsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val effects = listOf(
            VoiceEffectType.NORMAL,
            VoiceEffectType.MALE,
            VoiceEffectType.FEMALE,
            VoiceEffectType.ROBOT,
            VoiceEffectType.MONSTER,
            VoiceEffectType.ALIEN,
            VoiceEffectType.ECHO,
            VoiceEffectType.RADIO,
            VoiceEffectType.CHIPMUNK
        )

        for (effect in effects) {
            val chip = TextView(context).apply {
                text = effect.title
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setBackgroundColor(0xFF334155.toInt())
                setPadding(24, 14, 24, 14)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 12, 0)
                }
                layoutParams = lp

                setOnClickListener {
                    app.liveVoiceProcessor.currentParams = effect.defaultParams
                    Toast.makeText(context, "Voice Effect: ${effect.title}", Toast.LENGTH_SHORT).show()
                }
            }
            effectsRow.addView(chip)
        }
        scrollView.addView(effectsRow)
        expandedPanel.addView(scrollView)

        // Action Buttons Row (Live Mic toggle, Record, Open App, Close)
        val actionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnLiveMic = TextView(context).apply {
            text = "🎤 Mic: OFF"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            setBackgroundColor(0xFF2563EB.toInt())
            setPadding(20, 14, 20, 14)
            setOnClickListener {
                if (app.liveVoiceProcessor.isLiveListening.value) {
                    app.liveVoiceProcessor.stopLiveProcessing()
                    text = "🎤 Mic: OFF"
                } else {
                    val started = app.liveVoiceProcessor.startLiveProcessing(enableSpeakerPlayback = true)
                    text = if (started) "🎤 Mic: ON" else "🎤 Mic: Err"
                }
            }
        }

        val btnRecord = TextView(context).apply {
            text = "🔴 Record"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            setBackgroundColor(0xFFDC2626.toInt())
            setPadding(20, 14, 20, 14)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(12, 0, 0, 0)
            }
            layoutParams = lp

            setOnClickListener {
                if (app.liveVoiceProcessor.isRecording.value) {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val dir = File(filesDir, "recordings").apply { mkdirs() }
                    val finalWav = File(dir, "Floating_Rec_$timeStamp.wav")
                    val saved = app.liveVoiceProcessor.stopRecording(finalWav)
                    text = "🔴 Record"
                    if (saved != null) {
                        Toast.makeText(context, "Recording saved: ${saved.name}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val dir = File(filesDir, "recordings").apply { mkdirs() }
                    app.liveVoiceProcessor.startRecording(dir)
                    text = "⏹ Stop"
                    Toast.makeText(context, "Recording started...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnOpenApp = TextView(context).apply {
            text = "📱 Open App"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
            setPadding(20, 14, 20, 14)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(12, 0, 0, 0)
            }
            layoutParams = lp
            setOnClickListener {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(openIntent)
            }
        }

        actionsRow.addView(btnLiveMic)
        actionsRow.addView(btnRecord)
        actionsRow.addView(btnOpenApp)
        expandedPanel.addView(actionsRow)

        rootLayout.addView(bubbleButton)
        rootLayout.addView(expandedPanel)

        // Drag and click handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        bubbleButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(rootLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        isExpanded = !isExpanded
                        expandedPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        windowManager?.updateViewLayout(rootLayout, params)
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = rootLayout
        try {
            windowManager?.addView(rootLayout, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.preferences.setOverlayActive(false)
        if (floatingView != null && windowManager != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, VoiceOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceOverlayService::class.java)
            context.stopService(intent)
        }
    }
}
