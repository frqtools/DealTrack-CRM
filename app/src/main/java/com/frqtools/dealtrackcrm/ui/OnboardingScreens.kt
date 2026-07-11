package com.frqtools.dealtrackcrm.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.frqtools.dealtrackcrm.R
import com.frqtools.dealtrackcrm.data.AppSettings
import com.frqtools.dealtrackcrm.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2200) // Elegant splash delay
        
        val prefs = context.getSharedPreferences("dealtrack_prefs", Context.MODE_PRIVATE)
        val isOnboarded = prefs.getBoolean("is_onboarded", false)
        
        if (isOnboarded) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.ONBOARDING) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .shadow(16.dp, CircleShape)
                    .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dealtrack_logo),
                    contentDescription = "DealTrack CRM",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "DealTrack CRM",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your Offline Deal Companion",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            CircularProgressIndicator(
                color = Color(0xFF38BDF8),
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4

    var ownerName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PKR") }
    var snoozeMinutes by remember { mutableStateOf(30) }

    LaunchedEffect(settings) {
        if (ownerName.isEmpty() && settings.ownerName != "Owner") {
            ownerName = settings.ownerName
        }
        if (businessName.isEmpty() && settings.businessName != "My Business") {
            businessName = settings.businessName
        }
        currency = settings.currency
        snoozeMinutes = settings.snoozeMinutes
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index <= currentStep) PrimaryBlue else OutlineColor.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    0 -> OnboardingStepWelcome()
                    1 -> OnboardingStepClients()
                    2 -> OnboardingStepDeals()
                    3 -> OnboardingStepSetup(
                        ownerName = ownerName,
                        onOwnerNameChange = { ownerName = it },
                        businessName = businessName,
                        onBusinessNameChange = { businessName = it },
                        currency = currency,
                        onCurrencyChange = { currency = it },
                        snoozeMinutes = snoozeMinutes,
                        onSnoozeChange = { snoozeMinutes = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            if (ownerName.trim().isEmpty() || businessName.trim().isEmpty()) {
                                Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateSettings(
                                    settings.copy(
                                        ownerName = ownerName.trim(),
                                        businessName = businessName.trim(),
                                        currency = currency,
                                        snoozeMinutes = snoozeMinutes
                                    )
                                ) {
                                    val prefs = context.getSharedPreferences("dealtrack_prefs", Context.MODE_PRIVATE)
                                    prefs.edit().putBoolean("is_onboarded", true).apply()
                                    
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .weight(1f)
                        .padding(start = if (currentStep > 0) 8.dp else 0.dp)
                ) {
                    Text(
                        text = if (currentStep == totalSteps - 1) "Get Started" else "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingStepWelcome() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .shadow(8.dp, CircleShape)
                .border(2.dp, PrimaryBlue.copy(alpha = 0.3f), CircleShape)
                .clip(CircleShape)
                .background(PrimaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dealtrack_logo),
                contentDescription = "Welcome logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to DealTrack",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurfaceText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your elite offline CRM built to help you track clients, manage interactive deals, and follow up in real-time.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = OnSurfaceVariantText,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun OnboardingStepClients() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = "Clients illustration",
                tint = PrimaryBlue,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Track Your Clients",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurfaceText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Organize clients in a safe, lightning-fast offline database. Import from your local phone book directly with one click.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = OnSurfaceVariantText,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun OnboardingStepDeals() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(WonGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Deals illustration",
                tint = WonGreen,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Never Miss a Callback",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurfaceText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Record negotiation prices, log status milestones, and configure direct alarm alerts for critical call-backs.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = OnSurfaceVariantText,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun OnboardingStepSetup(
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    snoozeMinutes: Int,
    onSnoozeChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Personalize Your CRM",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurfaceText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Configure your professional settings to get started immediately.",
            fontSize = 15.sp,
            color = OnSurfaceVariantText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Full Name",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceText,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = ownerName,
            onValueChange = onOwnerNameChange,
            placeholder = { Text("e.g., John Doe") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = OutlineColor
            )
        )

        Text(
            text = "Your Business Name",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceText,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = businessName,
            onValueChange = onBusinessNameChange,
            placeholder = { Text("e.g., Apex Agency") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = OutlineColor
            )
        )

        Text(
            text = "Base Currency",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceText,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("PKR", "USD", "EUR", "AED").forEach { cur ->
                val isSelected = currency == cur
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryContainer else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (isSelected) PrimaryBlue else OutlineColor.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onCurrencyChange(cur) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cur,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PrimaryBlue else OnSurfaceVariantText,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Text(
            text = "Follow-Up Snooze Time",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceText,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(15, 30, 45, 60).forEach { mins ->
                val isSelected = snoozeMinutes == mins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryContainer else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (isSelected) PrimaryBlue else OutlineColor.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSnoozeChange(mins) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${mins}m",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PrimaryBlue else OnSurfaceVariantText,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
