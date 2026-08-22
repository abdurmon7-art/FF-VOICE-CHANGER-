package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.VoiceChangerApp
import com.example.audio.VoiceEffectType
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.VibrantCrimson
import com.example.ui.theme.VibrantDeepPurple
import com.example.ui.theme.VibrantLavender
import com.example.ui.theme.VibrantRosePink
import com.example.ui.theme.VibrantSuccessGreen
import com.example.ui.theme.VibrantWarningAmber
import com.example.ui.theme.VoiceChangerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class OverlayServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class VoiceOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayServiceLifecycleOwner? = null

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded = _isExpanded.asStateFlow()

    private val _selectedEffect = MutableStateFlow(VoiceEffectType.ROBOT)
    val selectedEffect = _selectedEffect.asStateFlow()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        startForegroundNotification()
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopLiveEngine()
                stopForeground(true)
                stopSelf()
            }
            ACTION_TOGGLE_MIC -> {
                val processor = VoiceChangerApp.instance.liveVoiceProcessor
                if (processor.isLiveListening.value) {
                    processor.stopLiveProcessing()
                } else {
                    processor.startLiveProcessing(enableSpeakerPlayback = true)
                }
                updateNotification()
            }
            ACTION_SET_EFFECT -> {
                val effectName = intent.getStringExtra(EXTRA_EFFECT_NAME)
                if (effectName != null) {
                    try {
                        val effect = VoiceEffectType.valueOf(effectName)
                        _selectedEffect.value = effect
                        VoiceChangerApp.instance.liveVoiceProcessor.currentParams = effect.defaultParams
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            ACTION_EXPAND_PANEL -> {
                _isExpanded.value = true
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildNotification("Live Voice Changer Active", "Voice effects running in background")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification() {
        val processor = VoiceChangerApp.instance.liveVoiceProcessor
        val isListening = processor.isLiveListening.value
        val title = if (isListening) "Live Voice Active: ${_selectedEffect.value.title}" else "Voice Changer Ready"
        val text = if (isListening) "Transforming mic audio in real-time" else "Tap to activate live voice changer"
        val notification = buildNotification(title, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleMicIntent = Intent(this, VoiceOverlayService::class.java).apply {
            action = ACTION_TOGGLE_MIC
        }
        val toggleMicPendingIntent = PendingIntent.getService(
            this, 1, toggleMicIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceOverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val processor = VoiceChangerApp.instance.liveVoiceProcessor
        val isListening = processor.isLiveListening.value

        return NotificationCompat.Builder(this, VoiceChangerApp.CHANNEL_ID_OVERLAY)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                if (isListening) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_btn_speak_now,
                if (isListening) "Stop Mic" else "Start Mic",
                toggleMicPendingIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Overlay", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("RtlHardcoded")
    private fun setupOverlayView() {
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
        windowLayoutParams = params

        val owner = OverlayServiceLifecycleOwner()
        lifecycleOwner = owner
        owner.performRestore(null)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        overlayComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                VoiceChangerTheme(darkTheme = true) {
                    OverlayContent(
                        service = this@VoiceOverlayService,
                        onDrag = { dx, dy ->
                            val currentParams = this@VoiceOverlayService.windowLayoutParams
                            if (currentParams != null) {
                                currentParams.x += dx.toInt()
                                currentParams.y += dy.toInt()
                                windowManager?.updateViewLayout(this@apply, currentParams)
                            }
                        },
                        onClose = {
                            stopLiveEngine()
                            stopForeground(true)
                            stopSelf()
                        }
                    )
                }
            }
        }

        windowManager?.addView(overlayComposeView, windowLayoutParams)
    }

    private fun stopLiveEngine() {
        VoiceChangerApp.instance.liveVoiceProcessor.stopLiveProcessing()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLiveEngine()
        lifecycleOwner?.destroy()
        overlayComposeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun setExpanded(expanded: Boolean) {
        _isExpanded.value = expanded
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_SERVICE"
        const val ACTION_TOGGLE_MIC = "com.example.service.ACTION_TOGGLE_MIC"
        const val ACTION_SET_EFFECT = "com.example.service.ACTION_SET_EFFECT"
        const val ACTION_EXPAND_PANEL = "com.example.service.ACTION_EXPAND_PANEL"
        const val EXTRA_EFFECT_NAME = "extra_effect_name"

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

@Composable
fun OverlayContent(
    service: VoiceOverlayService,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClose: () -> Unit
) {
    val isExpanded by service.isExpanded.collectAsState()
    val processor = VoiceChangerApp.instance.liveVoiceProcessor
    val isListening by processor.isLiveListening.collectAsState()
    val isRecording by processor.isRecording.collectAsState()
    val isMuted by processor.isMuted.collectAsState()
    val recordingDurationSec by processor.recordingDurationSec.collectAsState()
    val visualizerBars by processor.visualizerBars.collectAsState()
    val selectedEffect by service.selectedEffect.collectAsState()

    var isDragging by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .testTag("overlay_root")
    ) {
        // Floating Bubble Button (always visible)
        Surface(
            shape = CircleShape,
            color = if (isListening) VibrantSuccessGreen else VibrantLavender,
            shadowElevation = 10.dp,
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f)),
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable {
                    service.setExpanded(!isExpanded)
                }
                .testTag("overlay_bubble_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.FiberManualRecord
                    else if (isListening) Icons.Default.GraphicEq
                    else Icons.Default.Mic,
                    contentDescription = "Voice Changer Overlay",
                    tint = if (isRecording) VibrantCrimson
                    else if (isListening) Color.White
                    else VibrantDeepPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Expanded Control Panel Card
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1430).copy(alpha = 0.95f)
                ),
                border = BorderStroke(1.dp, VibrantLavender.copy(alpha = 0.4f)),
                modifier = Modifier
                    .width(280.dp)
                    .padding(top = 8.dp)
                    .shadow(16.dp, RoundedCornerShape(22.dp))
                    .testTag("overlay_expanded_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header with Effect Name & Minimize
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = selectedEffect.themeColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = selectedEffect.icon,
                                        contentDescription = null,
                                        tint = selectedEffect.themeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = selectedEffect.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isListening) "Live FX Active" else "Ready",
                                    fontSize = 10.sp,
                                    color = if (isListening) VibrantSuccessGreen else Color.LightGray
                                )
                            }
                        }

                        // Open Full App Button
                        IconButton(
                            onClick = {
                                val intent = Intent(service, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                service.startActivity(intent)
                                service.setExpanded(false)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open Studio",
                                tint = VibrantLavender,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Compact Live Waveform Visualizer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visualizerBars.forEach { barHeight ->
                            val effectiveHeight = if (isListening) barHeight else 0.08f
                            val barHeightDp = (22 * effectiveHeight).dp.coerceAtLeast(3.dp)
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(barHeightDp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(VibrantRosePink, VibrantLavender)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice Effect Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(VoiceEffectType.entries) { effect ->
                            val isSel = selectedEffect == effect
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (isSel) VibrantLavender else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, if (isSel) VibrantLavender else Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .clickable {
                                        val intent = Intent(service, VoiceOverlayService::class.java).apply {
                                            action = VoiceOverlayService.ACTION_SET_EFFECT
                                            putExtra(VoiceOverlayService.EXTRA_EFFECT_NAME, effect.name)
                                        }
                                        service.startService(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = effect.icon,
                                        contentDescription = null,
                                        tint = if (isSel) VibrantDeepPurple else effect.themeColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = effect.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) VibrantDeepPurple else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Controls: Live Mic, Mute, Record, Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live Mic Toggle Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isListening) VibrantSuccessGreen.copy(alpha = 0.2f) else VibrantLavender.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (isListening) VibrantSuccessGreen else VibrantLavender),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isListening) {
                                        processor.stopLiveProcessing()
                                    } else {
                                        processor.startLiveProcessing(enableSpeakerPlayback = true)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = if (isListening) VibrantSuccessGreen else VibrantLavender,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isListening) "Mic Active" else "Start Mic",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isListening) VibrantSuccessGreen else VibrantLavender
                                )
                            }
                        }

                        // Mute Toggle
                        IconButton(
                            onClick = { processor.setMuted(!isMuted) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) VibrantCrimson.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = if (isMuted) VibrantCrimson else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close Overlay
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
