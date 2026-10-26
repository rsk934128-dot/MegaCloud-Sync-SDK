package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.AuthManager
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MegaRed
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import kotlinx.coroutines.launch

/**
 * Modern, self-contained Login UI Component featuring:
 * - Cyber-cloud brand identity with glowing badge & status indicators
 * - Google Identity & Firebase Auth interactive sign-in
 * - Email & Password authentication fallback
 * - Instant Demo Operator Mode for quick testing in emulator
 * - Real-time progress feedback, cancellation handling & error alerts
 */
@Composable
fun ModernLoginComponent(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthError: ((String) -> Unit)? = null,
    onDemoMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEmailLogin by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CyanPrimary.copy(alpha = 0.65f),
                        CyanPrimary.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("modern_login_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Hero Icon with Neon Halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                // Outer glow circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CyanPrimary.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Foreground emblem container
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    CyanPrimary.copy(alpha = 0.25f),
                                    Color(0xFF003049).copy(alpha = 0.6f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = CyanPrimary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Shield",
                        tint = CyanPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Tiny Cloud status indicator badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(1.5.dp, CyanPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Cloud Synced",
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // App Brand Name & Subtitle
            Text(
                text = "AppPulse Cloud",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(SecurityGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zero-Trust Cloud & Threat Vault",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanPrimary
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Security Trust Pillars Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TrustPillarItem(
                        icon = Icons.Default.Security,
                        iconTint = MegaRed,
                        title = "Cloud Threat Audit",
                        subtitle = "Real-time APK vulnerability scanning & sync"
                    )
                    TrustPillarItem(
                        icon = Icons.Default.Sync,
                        iconTint = SecurityGreen,
                        title = "Firestore Multi-Device Sync",
                        subtitle = "Instant backup across all authorized devices"
                    )
                    TrustPillarItem(
                        icon = Icons.Default.Lock,
                        iconTint = CyanPrimary,
                        title = "Zero-Knowledge Vault",
                        subtitle = "Client-side encrypted APK storage with MEGA sync"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Error Banner
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                errorMessage?.let { errorText ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MegaRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MegaRed.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("auth_error_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Error",
                                tint = MegaRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorText,
                                fontSize = 12.sp,
                                color = MegaRed,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { errorMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MegaRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Google Sign-In Button
            GoogleSignInButton(
                isLoading = isLoading,
                onClick = {
                    isLoading = true
                    errorMessage = null
                    AuthManager.onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { err ->
                            isLoading = false
                            errorMessage = err
                            onAuthError?.invoke(err)
                        },
                        scope = coroutineScope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_sign_in_button")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Demo Operator / Instant Test Mode Direct Entry Button
            OutlinedButton(
                onClick = onDemoMode,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("demo_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SecurityAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue as Demo Operator (Instant Access)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Email Login Accordion
            if (!showEmailLogin) {
                TextButton(
                    onClick = { showEmailLogin = true },
                    modifier = Modifier.fillMaxWidth().testTag("toggle_email_login_btn")
                ) {
                    Text(
                        text = "Or Sign in / Register with Email & Password",
                        color = CyanPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Email Authentication",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("email_input")
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password (min 6 chars)", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("password_input")
                    )

                    Button(
                        onClick = {
                            if (emailInput.isNotBlank() && passwordInput.length >= 6) {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val res = AuthManager.signInWithEmail(emailInput.trim(), passwordInput.trim())
                                    isLoading = false
                                    res.onSuccess {
                                        onAuthSuccess()
                                    }.onFailure { ex ->
                                        errorMessage = ex.localizedMessage ?: "Email authentication failed"
                                    }
                                }
                            } else {
                                errorMessage = "Please enter a valid email and 6+ character password."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("email_sign_in_submit_btn")
                    ) {
                        Text("Sign In / Register with Email", color = Color(0xFF00202E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Security Compliance & Privacy Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SecurityGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Encrypted session authenticated via Firebase & Google Identity",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Premium Google Sign-In button following Material 3 guidelines and Google Brand identity.
 * Features the official multi-colored Google "G" icon drawn directly via Canvas for sharp vector rendering.
 */
@Composable
fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Sign in with Google"
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFFFFF),
            contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color(0xFFE0E0E0),
            disabledContentColor = Color(0xFF757575)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        modifier = modifier
            .height(52.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(14.dp))
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF4285F4),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Authenticating...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F1F1F)
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F),
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

/**
 * Clean Canvas-rendered 4-color Google "G" emblem.
 * Uses official brand colors:
 * - Blue: #4285F4
 * - Green: #34A853
 * - Yellow: #FBBC05
 * - Red: #EA4335
 */
@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val outerRadius = w / 2f
        val innerRadius = outerRadius * 0.58f

        // Blue right segment & horizontal bar
        val bluePath = Path().apply {
            moveTo(center.x, center.y - innerRadius * 0.2f)
            lineTo(w, center.y - innerRadius * 0.2f)
            lineTo(w, center.y + innerRadius * 0.45f)
            lineTo(center.x + innerRadius * 0.45f, center.y + innerRadius * 0.45f)
            arcTo(
                rect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 45f,
                forceMoveTo = false
            )
            arcTo(
                rect = Rect(0f, 0f, w, h),
                startAngleDegrees = 45f,
                sweepAngleDegrees = -70f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(bluePath, Color(0xFF4285F4), style = Fill)

        // Green bottom right & center segment
        val greenPath = Path().apply {
            arcTo(
                rect = Rect(0f, 0f, w, h),
                startAngleDegrees = 45f,
                sweepAngleDegrees = 90f,
                forceMoveTo = true
            )
            arcTo(
                rect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius),
                startAngleDegrees = 135f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(greenPath, Color(0xFF34A853), style = Fill)

        // Yellow bottom left segment
        val yellowPath = Path().apply {
            arcTo(
                rect = Rect(0f, 0f, w, h),
                startAngleDegrees = 135f,
                sweepAngleDegrees = 90f,
                forceMoveTo = true
            )
            arcTo(
                rect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius),
                startAngleDegrees = 225f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(yellowPath, Color(0xFFFBBC05), style = Fill)

        // Red top segment
        val redPath = Path().apply {
            arcTo(
                rect = Rect(0f, 0f, w, h),
                startAngleDegrees = 225f,
                sweepAngleDegrees = 110f,
                forceMoveTo = true
            )
            arcTo(
                rect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius),
                startAngleDegrees = 335f,
                sweepAngleDegrees = -110f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(redPath, Color(0xFFEA4335), style = Fill)

        // Clean center cutout to ensure crisp circle inside
        drawCircle(
            color = Color.White,
            radius = innerRadius * 0.72f,
            center = center
        )
        // Center blue tab extending inward
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(center.x - 1f, center.y - innerRadius * 0.28f),
            size = androidx.compose.ui.geometry.Size(outerRadius * 0.88f, innerRadius * 0.56f)
        )
    }
}

@Composable
private fun TrustPillarItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f))
                .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}
