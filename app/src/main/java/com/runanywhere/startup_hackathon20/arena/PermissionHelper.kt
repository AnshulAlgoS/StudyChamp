package com.runanywhere.startup_hackathon20.arena

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Check if usage stats permission is granted
 */
fun Context.hasUsageStatsPermission(): Boolean {
    return try {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }
}

/**
 * Open usage stats settings
 */
fun Context.openUsageStatsSettings() {
    try {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    } catch (e: Exception) {
        // Fallback to app settings
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

/**
 * Permission Request Dialog for Focus Mode
 */
@Composable
fun FocusModePermissionDialog(
    onDismiss: () -> Unit,
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasUsageStatsPermission()) }

    // Check permission periodically when dialog is shown
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val newPermission = context.hasUsageStatsPermission()
            if (newPermission != hasPermission) {
                hasPermission = newPermission
                if (hasPermission) {
                    onGranted()
                    onDismiss()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF6B46C1),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Enable Focus Mode",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "To enable Focus Mode and track app usage during study sessions, please grant Usage Access permission.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "What Focus Mode Does:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "• Monitors when you switch apps",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Records violations automatically",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Brings you back to study mode",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Calculates your focus score",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider()

                Text(
                    text = "Steps to enable:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. Tap 'Open Settings' below",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "2. Find 'StudyChamp' in the list",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "3. Toggle the switch to ON",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "4. Return to this app",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (hasPermission) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Permission granted! You're all set!",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    context.openUsageStatsSettings()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B46C1)
                )
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings")
            }
        },
        dismissButton = {
            if (hasPermission) {
                TextButton(onClick = onDismiss) {
                    Text("Continue")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Maybe Later")
                }
            }
        }
    )
}

/**
 * In-screen permission prompt (for initial setup)
 */
@Composable
fun FocusModePermissionCard(
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val hasPermission = context.hasUsageStatsPermission()

    if (!hasPermission) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Unlock Focus Mode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Text(
                    text = "Enable app monitoring to compete in Focus Arena and track your study habits!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242)
                )

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B46C1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Focus Mode")
                }
            }
        }
    }
}
