package com.frqtools.dealtrackcrm.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.frqtools.dealtrackcrm.data.*
import com.frqtools.dealtrackcrm.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Unified UI navigation mapping
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CLIENTS = "clients"
    const val ADD_EDIT_CLIENT = "add_edit_client?clientId={clientId}"
    const val CLIENT_PROFILE = "client_profile/{clientId}"
    const val DEALS = "deals"
    const val ADD_EDIT_DEAL = "add_edit_deal?dealId={dealId}&clientId={clientId}"
    const val ADD_EDIT_INTERACTION = "add_edit_interaction?interactionId={interactionId}&clientId={clientId}"
    const val FOLLOW_UPS = "follow_ups"
    const val ADD_EDIT_FOLLOW_UP = "add_edit_follow_up?followUpId={followUpId}&clientId={clientId}"
    const val SETTINGS = "settings"
    const val SEARCH = "search"

    fun getComposeRouteFromTargetLink(targetLink: String): String? {
        val cleanLink = targetLink.trim().replace("dealtrack://", "")
        if (cleanLink.startsWith("http://") || cleanLink.startsWith("https://")) {
            return null
        }
        return when {
            cleanLink.startsWith("clients") || cleanLink.startsWith("client_list") -> {
                CLIENTS
            }
            cleanLink.startsWith("client/") -> {
                val idStr = cleanLink.substringAfter("client/").trim()
                val id = idStr.toIntOrNull() ?: 0
                if (id > 0) {
                    CLIENT_PROFILE.replace("{clientId}", id.toString())
                } else {
                    CLIENTS
                }
            }
            cleanLink.startsWith("deals") || cleanLink.startsWith("deal_list") -> {
                DEALS
            }
            cleanLink.startsWith("deal/") -> {
                val idStr = cleanLink.substringAfter("deal/").trim()
                val id = idStr.toIntOrNull() ?: 0
                if (id > 0) {
                    ADD_EDIT_DEAL.replace("{dealId}", id.toString()).replace("{clientId}", "0")
                } else {
                    DEALS
                }
            }
            cleanLink.startsWith("followups") || cleanLink.startsWith("follow_ups") -> {
                FOLLOW_UPS
            }
            cleanLink.startsWith("settings") -> {
                SETTINGS
            }
            cleanLink.startsWith("search") -> {
                SEARCH
            }
            else -> null
        }
    }
}

// -------------------------------------------------------------
// 1. HOME SCREEN (DASHBOARD)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val deals by viewModel.deals.collectAsStateWithLifecycle()
    val followUps by viewModel.followUps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayEnd = todayStart + 86400000

    // Compute stats
    val overdueFollowUps = followUps.filter { !it.isDone && it.scheduledDateTime < now }
    val followUpsToday = followUps.filter { !it.isDone && it.scheduledDateTime in todayStart..todayEnd }
    val openDeals = deals.filter { it.status == "Open" }
    
    // Monthly won deals
    val currentMonthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
    }.timeInMillis
    val wonDealsThisMonth = deals.filter { it.status == "Won" && (it.closedDate ?: 0) >= currentMonthStart }
    val wonValueThisMonth = wonDealsThisMonth.sumOf { it.finalPrice ?: it.offeredPrice }

    val newClientsThisWeek = clients.filter { it.dateAdded >= now - 86400000 * 7 }

    // Aggregate recent actions
    val recentActivities = remember(clients, deals, viewModel.interactions.value) {
        val activities = mutableListOf<ActivityItem>()
        // Client creations
        clients.forEach {
            activities.add(ActivityItem("client_added", "New client added", it.name, it.dateAdded))
        }
        // Deals won/lost
        deals.forEach {
            if (it.status == "Won") {
                activities.add(ActivityItem("deal_won", "Deal won 🎉", "${it.title} (${formatCurrency(it.finalPrice ?: it.offeredPrice)})", it.closedDate ?: it.dateCreated))
            } else if (it.status == "Lost") {
                activities.add(ActivityItem("deal_lost", "Deal lost", "${it.title} - ${it.lostReason ?: ""}", it.closedDate ?: it.dateCreated))
            } else {
                activities.add(ActivityItem("deal_created", "New deal", "${it.title} (${formatCurrency(it.offeredPrice)})", it.dateCreated))
            }
        }
        // Interactions logged
        viewModel.interactions.value.forEach {
            activities.add(ActivityItem("interaction", "Logged ${it.contactMethod}", it.discussion, it.dateTime))
        }
        activities.sortedByDescending { it.timestamp }.take(10)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("DealTrack CRM", fontWeight = FontWeight.ExtraBold, color = OnSurfaceText, style = AppTypography.titleLarge) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.08f))
                            .clickable { onMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = if (settings.ownerName.length >= 2) settings.ownerName.substring(0, 2).uppercase(Locale.getDefault()) else "DT"
                        Text(
                            text = initials,
                            color = OnPrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SurfaceBg)
            )
        },
        floatingActionButton = {
            var showQuickMenu by remember { mutableStateOf(false) }
            Box(contentAlignment = Alignment.BottomEnd) {
                ExtendedFloatingActionButton(
                    onClick = { showQuickMenu = !showQuickMenu },
                    containerColor = PrimaryBlue,
                    contentColor = OnPrimaryBlue,
                    shape = RoundedCornerShape(16.dp),
                    icon = {
                        Icon(
                            imageVector = if (showQuickMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Quick Actions",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    text = {
                        Text("Add Quick Action", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                )
                DropdownMenu(
                    expanded = showQuickMenu,
                    onDismissRequest = { showQuickMenu = false },
                    modifier = Modifier.width(190.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Client", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.PersonAdd, null, tint = PrimaryBlue) },
                        onClick = {
                            showQuickMenu = false
                            navController.navigate("add_edit_client")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Log Interaction", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = WonGreen) },
                        onClick = {
                            showQuickMenu = false
                            navController.navigate("add_edit_interaction")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Set Reminder", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Alarm, null, tint = WarningAmber) },
                        onClick = {
                            showQuickMenu = false
                            navController.navigate("add_edit_follow_up")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add Deal", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.AddCard, null, tint = ProposalPurple) },
                        onClick = {
                            showQuickMenu = false
                            navController.navigate("add_edit_deal")
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // Premium Gradient Greetings Banner Card
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
            val greetingEmoji = when {
                hour < 12 -> "🌅"
                hour < 17 -> "☀️"
                else -> "🌌"
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryBlue, PrimaryBlue.copy(alpha = 0.85f), PrimaryLight)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$greeting $greetingEmoji",
                                    style = AppTypography.headlineSmall.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = settings.ownerName,
                                    style = AppTypography.headlineMedium.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = Color.White
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "PRO PARTNER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = settings.businessName,
                                style = AppTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        }
                    }
                }
            }

            // Big Search Bar Quick Link
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                border = BorderStroke(1.dp, OutlineColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { navController.navigate(Routes.SEARCH) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search clients, deals, or logs...",
                        style = AppTypography.bodyMedium,
                        color = OnSurfaceVariantText.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = OnSurfaceVariantText.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Overdue Alarm Banner
            if (overdueFollowUps.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LostRedContainer),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { navController.navigate(Routes.FOLLOW_UPS) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LostRed)
                        ) {
                            Text("!", color = OnLostRed, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${overdueFollowUps.size} Follow-ups Overdue",
                                style = AppTypography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = LostRed
                            )
                            Text(
                                text = "Immediate attention needed",
                                style = AppTypography.bodySmall,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                        Text(
                            text = "→",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = LostRed
                        )
                    }
                }
            }

            // Stats grid (2x2)
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        value = "${followUpsToday.size}",
                        label = "Reminders Today",
                        icon = Icons.Outlined.Notifications,
                        iconColor = WarningAmber,
                        iconBgColor = WarningContainer,
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate(Routes.FOLLOW_UPS) }
                    Spacer(modifier = Modifier.width(12.dp))
                    StatCard(
                        value = "${openDeals.size}",
                        label = "Open Deals",
                        icon = Icons.Outlined.Handshake,
                        iconColor = PrimaryBlue,
                        iconBgColor = PrimaryContainer,
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate(Routes.DEALS) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        value = formatCurrency(wonValueThisMonth, settings.currency),
                        label = "Won this Month",
                        icon = Icons.Outlined.Star,
                        iconColor = WonGreen,
                        iconBgColor = WonGreenContainer,
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate(Routes.DEALS) }
                    Spacer(modifier = Modifier.width(12.dp))
                    StatCard(
                        value = "${newClientsThisWeek.size}",
                        label = "New Clients (Week)",
                        icon = Icons.Outlined.People,
                        iconColor = ProposalPurple,
                        iconBgColor = ProposalPurpleContainer,
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate(Routes.CLIENTS) }
                }
            }

            // Quick Actions Title
            Text(
                text = "Quick Actions",
                style = AppTypography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = OnSurfaceText,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionCard(
                    label = "Client",
                    icon = Icons.Default.PersonAdd,
                    iconBgColor = PrimaryContainer,
                    iconTextColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("add_edit_client")
                }
                ActionCard(
                    label = "Log",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconBgColor = ProposalPurpleContainer,
                    iconTextColor = ProposalPurple,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("add_edit_interaction")
                }
                ActionCard(
                    label = "Follow",
                    icon = Icons.Default.Alarm,
                    iconBgColor = WarningContainer,
                    iconTextColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("add_edit_follow_up")
                }
                ActionCard(
                    label = "Deal",
                    icon = Icons.Default.AddCard,
                    iconBgColor = WonGreenContainer,
                    iconTextColor = WonGreen,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("add_edit_deal")
                }
            }

            // Recent Activity Title
            Text(
                text = "Recent Activity Feed",
                style = AppTypography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = OnSurfaceText,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 14.dp)
            )

            if (recentActivities.isEmpty()) {
                Text(
                    text = "No activities logged yet.",
                    style = AppTypography.bodyMedium,
                    color = OnSurfaceVariantText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    border = BorderStroke(1.dp, OutlineColor.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        recentActivities.forEachIndexed { index, act ->
                            RecentActivityRow(act, isLast = index == recentActivities.lastIndex)
                        }
                    }
                }
            }
        }
    }
}

data class ActivityItem(
    val type: String,
    val title: String,
    val desc: String,
    val timestamp: Long
)

@Composable
fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        border = BorderStroke(1.dp, OutlineColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBgColor)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = iconColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                style = AppTypography.headlineMedium.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = OnSurfaceText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = AppTypography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = OnSurfaceVariantText
            )
        }
    }
}

@Composable
fun ActionCard(
    label: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTextColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        border = BorderStroke(1.dp, OutlineColor.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier
            .height(86.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = AppTypography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.25.sp
                ),
                color = OnSurfaceText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RecentActivityRow(act: ActivityItem, isLast: Boolean) {
    val (icon, color) = when (act.type) {
        "client_added" -> Pair(Icons.Default.Person, PrimaryBlue)
        "deal_won" -> Pair(Icons.Default.Celebration, WonGreen)
        "deal_lost" -> Pair(Icons.Default.Cancel, LostRed)
        "deal_created" -> Pair(Icons.Default.Handshake, PrimaryLight)
        else -> Pair(Icons.AutoMirrored.Filled.Chat, OnHoldGray)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Connected Timeline track and node indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
                    .border(1.dp, color.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.4f), OutlineColor.copy(alpha = 0.3f))
                            )
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = act.title,
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurfaceText
                )
                Text(
                    text = formatTimeAgo(act.timestamp),
                    style = AppTypography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                    color = OnSurfaceVariantText.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = act.desc,
                style = AppTypography.bodySmall.copy(lineHeight = 16.sp),
                color = OnSurfaceVariantText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatTimeAgo(time: Long): String {
    val diff = System.currentTimeMillis() - time
    if (diff < 0) return "Just now"
    val minutes = diff / 60000
    if (minutes < 1) return "Just now"
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    if (days < 7) return "${days}d ago"
    return SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(time))
}

// -------------------------------------------------------------
// 2. CLIENT LIST SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val deals by viewModel.deals.collectAsStateWithLifecycle()
    val followUps by viewModel.followUps.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedType by remember { mutableStateOf("All") }

    // Unique custom tags + default ones
    val clientTypes = remember(clients) {
        listOf("All") + clients.map { it.clientType }.distinct().filter { it.isNotEmpty() }
    }

    val filteredClients = remember(clients, searchQuery.text, selectedType) {
        clients.filter {
            val matchesSearch = it.name.contains(searchQuery.text, ignoreCase = true) ||
                    it.phone.contains(searchQuery.text, ignoreCase = true) ||
                    it.companyName.contains(searchQuery.text, ignoreCase = true) ||
                    it.city.contains(searchQuery.text, ignoreCase = true)
            val matchesType = selectedType == "All" || it.clientType == selectedType
            matchesSearch && matchesType
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clients", fontWeight = FontWeight.Bold, color = OnSurfaceText, style = AppTypography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_edit_client") },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_client_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            var isSearchFocused by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, phone, or company...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariantText) },
                trailingIcon = {
                    if (searchQuery.text.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = OnSurfaceVariantText)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = OutlineColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !isSearchFocused) {
                            searchQuery = searchQuery.copy(
                                selection = TextRange(0, searchQuery.text.length)
                            )
                        }
                        isSearchFocused = focusState.isFocused
                    }
            )

            // Horizontal Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                clientTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceBg,
                            labelColor = OnSurfaceVariantText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredClients.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.People,
                    title = "No clients found",
                    description = if (searchQuery.text.isNotEmpty()) "Try adjusting your search keywords." else "Start adding clients manually or import from phone contacts.",
                    buttonText = if (searchQuery.text.isNotEmpty()) null else "Add Client",
                    onButtonClick = { navController.navigate("add_edit_client") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val openDealsCount = deals.count { it.clientId == client.id && it.status == "Open" }
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                        }.timeInMillis
                        val hasFollowUpToday = followUps.any {
                            it.clientId == client.id && !it.isDone && it.scheduledDateTime >= today && it.scheduledDateTime < today + 86400000
                        }

                        ClientCard(
                            client = client,
                            openDealsCount = openDealsCount,
                            hasFollowUpToday = hasFollowUpToday
                        ) {
                            navController.navigate("client_profile/${client.id}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientCard(
    client: Client,
    openDealsCount: Int,
    hasFollowUpToday: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClientAvatar(name = client.name)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    style = AppTypography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                if (client.companyName.isNotEmpty()) {
                    Text(
                        text = client.companyName,
                        style = AppTypography.bodySmall,
                        color = OnSurfaceVariantText
                    )
                }
                Text(
                    text = client.phone,
                    style = AppTypography.bodySmall,
                    color = OnSurfaceVariantText
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (openDealsCount > 0) {
                    StatusChip("Open Deal ($openDealsCount)")
                }
                if (hasFollowUpToday) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = LostRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Call Today",
                            color = OnLostRed,
                            style = AppTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. ADD / EDIT CLIENT SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClientScreen(
    navController: NavController,
    viewModel: MainViewModel,
    clientId: Int? = null
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var clientType by remember { mutableStateOf("Individual") }
    var source by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isEdit by remember { mutableStateOf(false) }

    // Error states
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val clientTypes = remember(settings.customClientTypes) {
        settings.customClientTypes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    LaunchedEffect(clientId, clients) {
        if (clientId != null && clientId != 0) {
            val cl = clients.find { it.id == clientId }
            if (cl != null) {
                isEdit = true
                name = cl.name
                phone = cl.phone
                companyName = cl.companyName
                city = cl.city
                clientType = cl.clientType
                source = cl.source
                notes = cl.notes
            }
        }
    }

    // Pick contact launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var cursor = contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val contactId = cursor.getString(idIndex)
                    val displayName = cursor.getString(nameIndex)
                    name = displayName

                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val hasPhone = cursor.getString(hasPhoneIndex)

                    if (hasPhone == "1") {
                        val phonesCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contactId),
                            null
                        )
                        if (phonesCursor != null && phonesCursor.moveToFirst()) {
                            val numberIndex = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numberIndex != -1) {
                                val phoneNumber = phonesCursor.getString(numberIndex)
                                phone = phoneNumber
                            }
                            phonesCursor.close()
                        }
                    }
                    cursor.close()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import contact: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permission to read contacts is required to import contacts", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Client" else "Add Client") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        var hasErr = false
                        if (name.trim().isEmpty()) {
                            nameError = "Enter a client name"
                            hasErr = true
                        } else {
                            nameError = null
                        }
                        if (phone.trim().isEmpty()) {
                            phoneError = "Enter a phone number"
                            hasErr = true
                        } else {
                            phoneError = null
                        }

                        if (!hasErr) {
                            val updated = Client(
                                id = clientId ?: 0,
                                name = name.trim(),
                                phone = phone.replace(" ", "").trim(),
                                companyName = companyName.trim(),
                                city = city.trim(),
                                clientType = clientType,
                                source = source.trim(),
                                notes = notes.trim()
                            )
                            viewModel.saveClient(updated) {
                                Toast.makeText(context, if (isEdit) "Client updated" else "Client saved", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                            }
                        }
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Import button
            if (!isEdit) {
                Button(
                    onClick = {
                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.READ_CONTACTS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            contactPickerLauncher.launch(null)
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from phone contacts", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            OutlinedFormTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                placeholder = "e.g., Fazal Ur Rehman",
                isError = nameError != null,
                errorMessage = nameError,
                testTag = "client_name_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedFormTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone Number",
                placeholder = "e.g., +92 300 1234567",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                errorMessage = phoneError,
                testTag = "client_phone_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedFormTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = "Company / Business Name (Optional)",
                placeholder = "e.g., FRQ Tools Store"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedFormTextField(
                value = city,
                onValueChange = { city = it },
                label = "City (Optional)",
                placeholder = "e.g., Karachi"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Client Type chips
            Text("Client Type", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                clientTypes.forEach { type ->
                    FilterChip(
                        selected = clientType == type,
                        onClick = { clientType = type },
                        label = { Text(type) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceBg,
                            labelColor = OnSurfaceVariantText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedFormTextField(
                value = source,
                onValueChange = { source = it },
                label = "Source (Where did you meet?)",
                placeholder = "e.g., Referral, Walk-in, WhatsApp, LinkedIn"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedFormTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes / Description",
                placeholder = "Enter any specific client requirements, offers discussed...",
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.height(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    var hasErr = false
                    if (name.trim().isEmpty()) {
                        nameError = "Enter a client name"
                        hasErr = true
                    } else {
                        nameError = null
                    }
                    if (phone.trim().isEmpty()) {
                        phoneError = "Enter a phone number"
                        hasErr = true
                    } else {
                        phoneError = null
                    }

                    if (!hasErr) {
                        val updated = Client(
                            id = clientId ?: 0,
                            name = name.trim(),
                            phone = phone.replace(" ", "").trim(),
                            companyName = companyName.trim(),
                            city = city.trim(),
                            clientType = clientType,
                            source = source.trim(),
                            notes = notes.trim()
                        )
                        viewModel.saveClient(updated) {
                            Toast.makeText(context, if (isEdit) "Client updated" else "Client saved", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (isEdit) "Update Client Info" else "Save Client", fontWeight = FontWeight.Bold, color = OnPrimaryBlue)
            }
        }
    }
}
