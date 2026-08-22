package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apps.AppCategory
import com.example.apps.AppRoutingCompatibility
import com.example.ui.VoiceChangerViewModel
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.VibrantCrimson
import com.example.ui.theme.VibrantDeepPurple
import com.example.ui.theme.VibrantLavender
import com.example.ui.theme.VibrantRosePink
import com.example.ui.theme.VibrantSuccessGreen
import com.example.ui.theme.VibrantWarningAmber

@Composable
fun AppsOverlayScreen(
    viewModel: VoiceChangerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.appsSearchQuery.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val selectedAppDetail by viewModel.selectedAppForDetail.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<AppCategory?>(null) }

    val hasOverlayPermission = remember(isOverlayActive) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    val displayedApps = remember(installedApps, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) installedApps
        else installedApps.filter { it.category == selectedCategoryFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Floating Overlay Controller Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floating_overlay_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isOverlayActive) VibrantLavender else DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = VibrantLavender,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = VibrantDeepPurple,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Floating Voice Controller",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isOverlayActive) "Active on screen" else "Overlay bubble over other apps",
                                    fontSize = 12.sp,
                                    color = if (isOverlayActive) VibrantSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isOverlayActive,
                            onCheckedChange = { viewModel.toggleOverlay(context) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VibrantDeepPurple,
                                checkedTrackColor = VibrantLavender,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("overlay_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "The floating bubble gives you instant 1-tap access to voice effects, live microphone preview, and voice recording controls while using games and other apps.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Permission status banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasOverlayPermission) VibrantSuccessGreen.copy(alpha = 0.14f)
                        else VibrantCrimson.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (hasOverlayPermission) VibrantSuccessGreen.copy(alpha = 0.35f)
                            else VibrantCrimson.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (hasOverlayPermission) VibrantSuccessGreen else VibrantCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasOverlayPermission) "Overlay permission active" else "Display overlay permission required",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasOverlayPermission) VibrantSuccessGreen else VibrantCrimson
                            )
                        }
                    }
                }
            }
        }

        // Android Audio Security & Compatibility Notice Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantDeepPurple.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, VibrantLavender.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VibrantLavender,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Android Audio Routing Compatibility",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = VibrantLavender
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Android's security model prohibits direct microphone stream hijacking of 3rd party apps. Tap any app below to view its supported official routing methods (Voice Note Share, Headset Passthrough, AUX Loopback, or Internal Audio Capture).",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                item {
                    val isSel = selectedCategoryFilter == null
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isSel) VibrantLavender else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSel) VibrantLavender else DarkCardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { selectedCategoryFilter = null }
                    ) {
                        Text(
                            text = "All Apps (${installedApps.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) VibrantDeepPurple else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                items(AppCategory.entries) { category ->
                    val isSel = selectedCategoryFilter == category
                    val count = installedApps.count { it.category == category }
                    if (count > 0) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSel) VibrantLavender else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSel) VibrantLavender else DarkCardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { selectedCategoryFilter = category }
                        ) {
                            Text(
                                text = "${category.title} ($count)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) VibrantDeepPurple else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Search Bar for Installed Apps
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchApps(it) },
                placeholder = { Text("Search installed apps...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchApps("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apps_search_bar")
            )
        }

        // Section Title: Apps List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Routing Directory (${displayedApps.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap for Routing Details",
                    fontSize = 11.sp,
                    color = VibrantLavender
                )
            }
        }

        if (displayedApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No apps matching '$searchQuery'" else "Loading installed apps...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedApps, key = { it.packageName }) { appInfo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { viewModel.setSelectedAppDetail(appInfo) }
                        .testTag("app_item_${appInfo.packageName}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App Icon
                            AppIconImage(drawable = appInfo.icon)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = appInfo.appName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = VibrantLavender.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = appInfo.category.title,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VibrantLavender,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Launch with Overlay Quick Action
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VibrantLavender,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.launchAppWithOverlay(context, appInfo) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = "Launch",
                                    tint = VibrantDeepPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Launch",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VibrantDeepPurple
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // App Routing Detail Dialog
    if (selectedAppDetail != null) {
        val app = selectedAppDetail!!
        AlertDialog(
            onDismissRequest = { viewModel.setSelectedAppDetail(null) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconImage(drawable = app.icon)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = app.category.title,
                            fontSize = 12.sp,
                            color = VibrantLavender
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Security limitation badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = VibrantWarningAmber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, VibrantWarningAmber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = VibrantWarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Direct Virtual Mic Injection: Prohibited by Android OS (Sandbox/SELinux).",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VibrantWarningAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "OFFICIAL SUPPORTED METHODS FOR THIS APP:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibrantLavender
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    app.supportedOfficialMethods.forEach { method ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = VibrantLavender
                            )
                            Text(
                                text = method,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = app.technicalExplanation,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        viewModel.setSelectedAppDetail(null)
                        viewModel.launchAppWithOverlay(context, app)
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VibrantLavender
                    )
                ) {
                    Text(
                        text = "Launch with Floating Bubble",
                        fontWeight = FontWeight.Bold,
                        color = VibrantDeepPurple
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setSelectedAppDetail(null) }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AppIconImage(drawable: Drawable?) {
    if (drawable == null) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    } else {
        val bitmap = remember(drawable) {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val b = Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(48),
                    drawable.intrinsicHeight.coerceAtLeast(48),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(b)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                b
            }
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    }
}
