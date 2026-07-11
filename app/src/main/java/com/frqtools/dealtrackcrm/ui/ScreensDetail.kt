package com.frqtools.dealtrackcrm.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.frqtools.dealtrackcrm.data.*
import com.frqtools.dealtrackcrm.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// -------------------------------------------------------------
// 4. CLIENT PROFILE SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(
    navController: NavController,
    viewModel: MainViewModel,
    clientId: Int
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val deals by viewModel.deals.collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    val followUps by viewModel.followUps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val client = remember(clients, clientId) { clients.find { it.id == clientId } }

    if (client == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Client not found", style = AppTypography.headlineMedium)
        }
        return
    }

    val clientDeals = remember(deals, clientId) { deals.filter { it.clientId == clientId } }
    val clientInteractions = remember(interactions, clientId) { interactions.filter { it.clientId == clientId } }
    val clientFollowUps = remember(followUps, clientId) { followUps.filter { it.clientId == clientId } }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Info", "Deals", "Interactions", "Follow-Ups")

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${client.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This action is permanent and cannot be undone.",
                        fontWeight = FontWeight.SemiBold,
                        color = LostRed
                    )
                    Text("The following associated records for ${client.name} will be permanently deleted:")
                    
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• ${clientDeals.size} Deal(s) (${clientDeals.count { it.status == "Open" }} open)")
                        Text("• ${clientInteractions.size} Interaction Log(s)")
                        Text("• ${clientFollowUps.size} Follow-up Reminder(s) (scheduled alarms will be cancelled)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClient(context, client) {
                            Toast.makeText(context, "Client deleted", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                ) { Text("Delete", color = OnLostRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("add_edit_client?clientId=${client.id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Client", tint = PrimaryBlue)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Client", tint = LostRed)
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
        ) {
            // Profile Header
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClientAvatar(name = client.name, size = 64.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(client.name, style = AppTypography.headlineMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                    if (client.companyName.isNotEmpty()) {
                        Text(client.companyName, style = AppTypography.bodyMedium, color = OnSurfaceVariantText)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        ProfileActionButton(icon = Icons.Default.Call, label = "Call", color = PrimaryBlue) {
                            val dialedPhone = cleanPhoneForDialing(client.phone)
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialedPhone"))
                            context.startActivity(intent)
                        }
                        ProfileActionButton(icon = Icons.AutoMirrored.Filled.Chat, label = "WhatsApp", color = WonGreen) {
                            val cleanPhone = cleanPhoneForWhatsApp(client.phone, settings.currency)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone"))
                            context.startActivity(intent)
                        }
                        ProfileActionButton(icon = Icons.Default.AddComment, label = "+ Log", color = PrimaryLight) {
                            navController.navigate("add_edit_interaction?clientId=${client.id}")
                        }
                        ProfileActionButton(icon = Icons.Default.AddAlert, label = "+ Reminder", color = WarningAmber) {
                            navController.navigate("add_edit_follow_up?clientId=${client.id}")
                        }
                        ProfileActionButton(icon = Icons.Default.AddCard, label = "+ Deal", color = ProposalPurple) {
                            navController.navigate("add_edit_deal?clientId=${client.id}")
                        }
                    }
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceBg,
                contentColor = PrimaryBlue,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreenBg)
            ) { page ->
                when (page) {
                    0 -> InfoTabContent(client)
                    1 -> DealsTabContent(clientDeals, settings.currency, viewModel, navController, client.id)
                    2 -> InteractionsTabContent(clientInteractions, viewModel, navController, client.id)
                    3 -> FollowUpsTabContent(clientFollowUps, viewModel, context, navController, client.id)
                }
            }
        }
    }
}

@Composable
fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = AppTypography.labelSmall, color = OnSurfaceText, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun InfoTabContent(client: Client) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoRowCard("Primary Phone", client.phone, Icons.Default.Phone)
        if (client.companyName.isNotEmpty()) {
            InfoRowCard("Company / Business", client.companyName, Icons.Default.Business)
        }
        if (client.city.isNotEmpty()) {
            InfoRowCard("City Location", client.city, Icons.Default.LocationOn)
        }
        InfoRowCard("Client Type Tag", client.clientType, Icons.Default.Label)
        if (client.source.isNotEmpty()) {
            InfoRowCard("Client Acquisition Source", client.source, Icons.Default.Launch)
        }
        InfoRowCard("Date Added", formatDate(client.dateAdded), Icons.Default.CalendarToday)
        if (client.notes.isNotEmpty()) {
            InfoRowCard("Private Log / Notes", client.notes, Icons.Default.Notes)
        }
    }
}

@Composable
fun InfoRowCard(label: String, value: String, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = AppTypography.bodySmall, color = OnSurfaceVariantText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, style = AppTypography.bodyLarge, color = OnSurfaceText)
            }
        }
    }
}

@Composable
fun DealsTabContent(
    deals: List<Deal>,
    currency: String,
    viewModel: MainViewModel,
    navController: NavController,
    clientId: Int
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var dealToDelete by remember { mutableStateOf<Deal?>(null) }

    if (dealToDelete != null) {
        AlertDialog(
            onDismissRequest = { dealToDelete = null },
            title = { Text("Delete Deal?") },
            text = { Text("Are you sure you want to permanently delete \"${dealToDelete?.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        dealToDelete?.let { deal ->
                            viewModel.deleteDeal(context, deal) {}
                        }
                        dealToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                ) {
                    Text("Delete", color = OnLostRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { dealToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deals.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.Handshake,
            title = "No deals for this client",
            description = "Deals help you track offered prices and status stages.",
            buttonText = "+ New Deal",
            onButtonClick = { navController.navigate("add_edit_deal?clientId=$clientId") }
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(deals) { deal ->
                DealCardRow(
                    deal = deal,
                    currency = currency,
                    onDelete = { dealToDelete = deal }
                ) {
                    navController.navigate("add_edit_deal?dealId=${deal.id}")
                }
            }
        }
    }
}

@Composable
fun InteractionsTabContent(
    interactions: List<Interaction>,
    viewModel: MainViewModel,
    navController: NavController,
    clientId: Int
) {
    var interactionToDelete by remember { mutableStateOf<Interaction?>(null) }

    if (interactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { interactionToDelete = null },
            title = { Text("Delete Interaction?") },
            text = { Text("Are you sure you want to permanently delete this interaction log?") },
            confirmButton = {
                Button(
                    onClick = {
                        interactionToDelete?.let { interaction ->
                            viewModel.deleteInteraction(interaction) {}
                        }
                        interactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                ) {
                    Text("Delete", color = OnLostRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { interactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (interactions.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.Chat,
            title = "No interactions logged",
            description = "Log calls, meetings, or chats to build a history with this client.",
            buttonText = "+ Log Interaction",
            onButtonClick = { navController.navigate("add_edit_interaction?clientId=$clientId") }
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(interactions) { interaction ->
                InteractionCardRow(
                    interaction = interaction,
                    onDelete = { interactionToDelete = interaction }
                ) {
                    navController.navigate("add_edit_interaction?interactionId=${interaction.id}")
                }
            }
        }
    }
}

@Composable
fun FollowUpsTabContent(
    followUps: List<FollowUp>,
    viewModel: MainViewModel,
    context: Context,
    navController: NavController,
    clientId: Int
) {
    var followUpToDelete by remember { mutableStateOf<FollowUp?>(null) }

    if (followUpToDelete != null) {
        AlertDialog(
            onDismissRequest = { followUpToDelete = null },
            title = { Text("Delete Reminder?") },
            text = { Text("Are you sure you want to permanently delete this reminder? Any scheduled alarm will be cancelled.") },
            confirmButton = {
                Button(
                    onClick = {
                        followUpToDelete?.let { followUp ->
                            viewModel.deleteFollowUp(context, followUp) {}
                        }
                        followUpToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                ) {
                    Text("Delete", color = OnLostRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { followUpToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (followUps.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.NotificationsNone,
            title = "No reminders scheduled",
            description = "Set exact-time follow-up call alarms so you never forget.",
            buttonText = "+ Set Reminder",
            onButtonClick = { navController.navigate("add_edit_follow_up?clientId=$clientId") }
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(followUps) { item ->
                FollowUpCardRow(
                    followUp = item,
                    clientName = "",
                    showClientName = false,
                    onDone = { viewModel.markFollowUpDone(context, item) },
                    onSnooze = { viewModel.snoozeFollowUp(context, item) },
                    onDelete = { followUpToDelete = item },
                    onClick = { navController.navigate("add_edit_follow_up?followUpId=${item.id}") }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. DEAL LIST SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealListScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val deals by viewModel.deals.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var selectedStatusTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Open", "Won", "Lost", "On Hold")

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    LaunchedEffect(pagerState.currentPage) {
        selectedStatusTab = pagerState.currentPage
    }

    LaunchedEffect(selectedStatusTab) {
        if (pagerState.currentPage != selectedStatusTab) {
            pagerState.animateScrollToPage(selectedStatusTab)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals Tracker", fontWeight = FontWeight.Bold, color = OnSurfaceText, style = AppTypography.titleLarge) },
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
                onClick = { navController.navigate("add_edit_deal") },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Deal", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedStatusTab,
                containerColor = SurfaceBg,
                contentColor = PrimaryBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedStatusTab == index,
                        onClick = { selectedStatusTab = index },
                        text = { Text(title, style = AppTypography.labelSmall, fontWeight = if (selectedStatusTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageDeals = remember(deals, page) {
                    when (page) {
                        0 -> deals
                        1 -> deals.filter { it.status == "Open" }
                        2 -> deals.filter { it.status == "Won" }
                        3 -> deals.filter { it.status == "Lost" }
                        4 -> deals.filter { it.status == "On Hold" }
                        else -> deals
                    }
                }

                val pageTotalValue = remember(pageDeals, settings.currency) {
                    pageDeals.sumOf { it.finalPrice ?: it.offeredPrice }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Total Value banner
                    Surface(
                        color = PrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total filtered value:",
                                style = AppTypography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                            Text(
                                text = formatCurrency(pageTotalValue, settings.currency),
                                style = AppTypography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }

                    if (pageDeals.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.Handshake,
                            title = "No deals here",
                            description = "Deals help you record offers, negotiate stages and secure wins.",
                            buttonText = "New Deal",
                            onButtonClick = { navController.navigate("add_edit_deal") }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pageDeals) { deal ->
                                val clientName = clients.find { it.id == deal.clientId }?.name ?: "Unknown Client"
                                DealCardRow(
                                    deal = deal,
                                    currency = settings.currency,
                                    clientName = clientName,
                                    onClientClick = {
                                        navController.navigate("client_profile/${deal.clientId}")
                                    }
                                ) {
                                    navController.navigate("add_edit_deal?dealId=${deal.id}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DealCardRow(
    deal: Deal,
    currency: String,
    clientName: String = "",
    onDelete: (() -> Unit)? = null,
    onClientClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val accentColor = when (deal.status.lowercase(Locale.getDefault())) {
        "won" -> WonGreen
        "lost" -> LostRed
        "on hold" -> OnHoldGray
        else -> PrimaryBlue
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color-coded edge accent
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(84.dp)
                    .background(accentColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deal.title,
                        style = AppTypography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (clientName.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 2.dp, bottom = 2.dp)
                                .then(
                                    if (onClientClick != null) Modifier.clickable { onClientClick() }
                                    else Modifier
                                )
                        ) {
                            if (onClientClick != null) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = PrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = clientName,
                                style = AppTypography.bodySmall.copy(
                                    fontWeight = if (onClientClick != null) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (onClientClick != null) PrimaryBlue else OnSurfaceVariantText
                            )
                        }
                    }
                    Text(
                        text = "Stage: ${deal.stage}",
                        style = AppTypography.bodySmall,
                        color = OnSurfaceVariantText
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    val finalPrice = deal.finalPrice ?: deal.offeredPrice
                    Text(
                        text = formatCurrency(finalPrice, currency),
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(deal.status)
                }
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Deal",
                            tint = LostRed.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. ADD / EDIT DEAL SCREEN + CLOSE DEAL FLOW
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDealScreen(
    navController: NavController,
    viewModel: MainViewModel,
    dealId: Int? = null,
    clientId: Int? = null
) {
    val context = LocalContext.current
    val deals by viewModel.deals.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var selectedClientId by remember { mutableStateOf(clientId ?: 0) }
    var title by remember { mutableStateOf("") }
    var offeredPriceStr by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("Prospect") }
    var status by remember { mutableStateOf("Open") }
    var notes by remember { mutableStateOf("") }
    var expectedCloseDate by remember { mutableStateOf<Long?>(null) }

    var isEdit by remember { mutableStateOf(false) }

    // Dropdown / selector states
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var stageDropdownExpanded by remember { mutableStateOf(false) }

    // Close deal prompts
    var showWonDialog by remember { mutableStateOf(false) }
    var showLostDialog by remember { mutableStateOf(false) }

    // Captured close deal details
    var finalPriceStr by remember { mutableStateOf("") }
    var lostReason by remember { mutableStateOf("Price Too High") }
    var customLostReason by remember { mutableStateOf("") }

    val stages = remember(settings.customStages) {
        settings.customStages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    LaunchedEffect(dealId, deals) {
        if (dealId != null && dealId != 0) {
            val dl = deals.find { it.id == dealId }
            if (dl != null) {
                isEdit = true
                selectedClientId = dl.clientId
                title = dl.title
                offeredPriceStr = dl.offeredPrice.toString()
                stage = dl.stage
                status = dl.status
                notes = dl.notes
                expectedCloseDate = dl.expectedCloseDate
                finalPriceStr = (dl.finalPrice ?: dl.offeredPrice).toString()
                if (dl.lostReason != null) {
                    lostReason = dl.lostReason
                }
            }
        }
    }

    // Helper functions for date pickers
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val sel = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            expectedCloseDate = sel.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Won Close Deal Dialog
    if (showWonDialog) {
        AlertDialog(
            onDismissRequest = { showWonDialog = false },
            title = { Text("Mark Deal as WON! 🎉") },
            text = {
                Column {
                    Text("Confirm the final agreed sales value of this closed deal:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = finalPriceStr,
                        onValueChange = { finalPriceStr = it },
                        label = { Text("Final Agreed Price (${settings.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWonDialog = false
                        status = "Won"
                        stage = "Won"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WonGreen)
                ) { Text("Confirm Win", color = OnWonGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showWonDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Lost Close Deal Dialog
    if (showLostDialog) {
        val reasons = listOf("Price Too High", "No Response", "Went to Competitor", "No Budget", "Other")
        AlertDialog(
            onDismissRequest = { showLostDialog = false },
            title = { Text("Mark Deal as LOST") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Select the primary reason for this deal loss:")
                    Spacer(modifier = Modifier.height(12.dp))
                    reasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { lostReason = r }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = lostReason == r, onClick = { lostReason = r })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(r)
                        }
                    }
                    if (lostReason == "Other") {
                        OutlinedTextField(
                            value = customLostReason,
                            onValueChange = { customLostReason = it },
                            label = { Text("Specify Custom Reason") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLostDialog = false
                        status = "Lost"
                        stage = "Lost"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                ) { Text("Mark Lost", color = OnLostRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLostDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Deal" else "New Deal") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEdit) {
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Deal", tint = LostRed)
                        }
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete Deal?") },
                                text = { Text("Are you sure you want to permanently delete this deal?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val dl = deals.find { it.id == dealId }
                                            if (dl != null) {
                                                viewModel.deleteDeal(context, dl) {
                                                    Toast.makeText(context, "Deal deleted", Toast.LENGTH_SHORT).show()
                                                    navController.navigateUp()
                                                }
                                            }
                                            showDeleteConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                                    ) { Text("Delete", color = OnLostRed) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                    TextButton(onClick = {
                        val clientSelected = clients.find { it.id == selectedClientId }
                        if (clientSelected == null) {
                            Toast.makeText(context, "Please select a client", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (title.trim().isEmpty()) {
                            Toast.makeText(context, "Enter a deal title", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val price = cleanPriceInput(offeredPriceStr).toDoubleOrNull() ?: 0.0
                        val finalPrice = cleanPriceInput(finalPriceStr).toDoubleOrNull() ?: price

                        val updated = Deal(
                            id = dealId ?: 0,
                            clientId = selectedClientId,
                            title = title.trim(),
                            offeredPrice = price,
                            finalPrice = if (status == "Won") finalPrice else null,
                            stage = stage,
                            status = status,
                            expectedCloseDate = expectedCloseDate,
                            closedDate = if (status == "Won" || status == "Lost") System.currentTimeMillis() else null,
                            lostReason = if (status == "Lost") (if (lostReason == "Other") customLostReason else lostReason) else null,
                            notes = notes.trim()
                        )
                        viewModel.saveDeal(updated) {
                            Toast.makeText(context, "Deal saved", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
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
            // Client Dropdown Selector
            ClientSelectionHeader(
                selectedClientId = selectedClientId,
                onClientSelected = { selectedClientId = it },
                clients = clients,
                viewModel = viewModel,
                navController = navController
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                val currentClient = clients.find { it.id == selectedClientId }
                OutlinedTextField(
                    value = currentClient?.name ?: "Select Client...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = OutlineColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { clientDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = clientDropdownExpanded,
                    onDismissRequest = { clientDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    var searchQuery by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        placeholder = { Text("Search clients...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = OutlineColor
                        )
                    )
                    val filteredClients = remember(searchQuery, clients) {
                        if (searchQuery.isEmpty()) {
                            clients
                        } else {
                            clients.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.phone.contains(searchQuery, ignoreCase = true) ||
                                it.companyName.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    if (filteredClients.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No clients found", style = AppTypography.bodyMedium, color = OnSurfaceVariantText) },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        filteredClients.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.phone})") },
                                onClick = {
                                    selectedClientId = c.id
                                    clientDropdownExpanded = false
                                    searchQuery = ""
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedFormTextField(
                value = title,
                onValueChange = { title = it },
                label = "Deal Title (Product/Service)",
                placeholder = "e.g., 50 cartons tissues, Website redesign"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedFormTextField(
                value = offeredPriceStr,
                onValueChange = { offeredPriceStr = it },
                label = "Offered Price (${settings.currency})",
                placeholder = "e.g., 25000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expected close date picker
            Text("Expected Close Date", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = expectedCloseDate?.let { formatDate(it) } ?: "Select target date...",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.DateRange, null) } },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = OutlineColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deal stage selector
            Text("Current Stage", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = stage,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = OutlineColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { stageDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = stageDropdownExpanded,
                    onDismissRequest = { stageDropdownExpanded = false }
                ) {
                    stages.forEach { st ->
                        DropdownMenuItem(
                            text = { Text(st) },
                            onClick = {
                                stage = st
                                stageDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Selector (Open, Won, Lost, On Hold)
            Text("Overall Status Flow", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = remember(settings.customStatuses) {
                    settings.customStatuses.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
                statuses.forEach { st ->
                    val color = when (st) {
                        "Won" -> WonGreen
                        "Lost" -> LostRed
                        "On Hold" -> OnHoldGray
                        else -> PrimaryBlue
                    }
                    val isSelected = status == st

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (st == "Won") {
                                finalPriceStr = offeredPriceStr.ifEmpty { "0" }
                                showWonDialog = true
                            } else if (st == "Lost") {
                                showLostDialog = true
                            } else {
                                status = st
                            }
                        },
                        label = { Text(st) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedFormTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Deal Notes / Closure Terms",
                placeholder = "Add details of specific negotiation, client replies, delivery requirements...",
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.height(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val clientSelected = clients.find { it.id == selectedClientId }
                    if (clientSelected == null) {
                        Toast.makeText(context, "Please select a client", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (title.trim().isEmpty()) {
                        Toast.makeText(context, "Enter a deal title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val price = cleanPriceInput(offeredPriceStr).toDoubleOrNull() ?: 0.0
                    val finalPrice = cleanPriceInput(finalPriceStr).toDoubleOrNull() ?: price

                    val updated = Deal(
                        id = dealId ?: 0,
                        clientId = selectedClientId,
                        title = title.trim(),
                        offeredPrice = price,
                        finalPrice = if (status == "Won") finalPrice else null,
                        stage = stage,
                        status = status,
                        expectedCloseDate = expectedCloseDate,
                        closedDate = if (status == "Won" || status == "Lost") System.currentTimeMillis() else null,
                        lostReason = if (status == "Lost") (if (lostReason == "Other") customLostReason else lostReason) else null,
                        notes = notes.trim()
                    )
                    viewModel.saveDeal(updated) {
                        Toast.makeText(context, "Deal saved", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (isEdit) "Update Deal" else "Create Deal", fontWeight = FontWeight.Bold, color = OnPrimaryBlue)
            }
        }
    }
}

// -------------------------------------------------------------
// 7. ADD / EDIT INTERACTION SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInteractionScreen(
    navController: NavController,
    viewModel: MainViewModel,
    interactionId: Int? = null,
    clientId: Int? = null
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val deals by viewModel.deals.collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()

    var selectedClientId by remember { mutableStateOf(clientId ?: 0) }
    var selectedDealId by remember { mutableStateOf<Int?>(null) }
    var contactMethod by remember { mutableStateOf("Call") }
    var discussion by remember { mutableStateOf("") }
    var priceOfferedStr by remember { mutableStateOf("") }
    var productDiscussed by remember { mutableStateOf("") }
    var clientResponse by remember { mutableStateOf("Interested") }
    var myNextStep by remember { mutableStateOf("") }

    var isEdit by remember { mutableStateOf(false) }

    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var dealDropdownExpanded by remember { mutableStateOf(false) }

    val methods = listOf("Call", "WhatsApp", "Meeting", "Email", "Other")
    val responses = listOf("Interested", "Not Interested", "Thinking", "Follow Later", "Closed")

    val clientDeals = remember(deals, selectedClientId) {
        deals.filter { it.clientId == selectedClientId }
    }

    LaunchedEffect(interactionId, interactions) {
        if (interactionId != null && interactionId != 0) {
            val intr = interactions.find { it.id == interactionId }
            if (intr != null) {
                isEdit = true
                selectedClientId = intr.clientId
                selectedDealId = intr.dealId
                contactMethod = intr.contactMethod
                discussion = intr.discussion
                priceOfferedStr = intr.priceOffered?.toString() ?: ""
                productDiscussed = intr.productDiscussed ?: ""
                clientResponse = intr.clientResponse
                myNextStep = intr.myNextStep
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Interaction" else "Log Interaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEdit) {
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Interaction", tint = LostRed)
                        }
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete Interaction?") },
                                text = { Text("Are you sure you want to permanently delete this interaction log?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val intr = interactions.find { it.id == interactionId }
                                            if (intr != null) {
                                                viewModel.deleteInteraction(intr) {
                                                    Toast.makeText(context, "Interaction deleted", Toast.LENGTH_SHORT).show()
                                                    navController.navigateUp()
                                                }
                                            }
                                            showDeleteConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                                    ) { Text("Delete", color = OnLostRed) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                    TextButton(onClick = {
                        val clientSelected = clients.find { it.id == selectedClientId }
                        if (clientSelected == null) {
                            Toast.makeText(context, "Select a client first", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (discussion.trim().isEmpty()) {
                            Toast.makeText(context, "Enter what was discussed", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        val interaction = Interaction(
                            id = interactionId ?: 0,
                            clientId = selectedClientId,
                            dealId = selectedDealId,
                            dateTime = System.currentTimeMillis(),
                            contactMethod = contactMethod,
                            discussion = discussion.trim(),
                            priceOffered = cleanPriceInput(priceOfferedStr).toDoubleOrNull(),
                            productDiscussed = productDiscussed.trim().ifEmpty { null },
                            clientResponse = clientResponse,
                            myNextStep = myNextStep.trim()
                        )
                        viewModel.saveInteraction(interaction) {
                            Toast.makeText(context, "Interaction logged", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
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
            // Client Dropdown Selector
            ClientSelectionHeader(
                selectedClientId = selectedClientId,
                onClientSelected = { selectedClientId = it },
                clients = clients,
                viewModel = viewModel,
                navController = navController
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                val currentClient = clients.find { it.id == selectedClientId }
                OutlinedTextField(
                    value = currentClient?.name ?: "Select Client...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = OutlineColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { clientDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = clientDropdownExpanded,
                    onDismissRequest = { clientDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    var searchQuery by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        placeholder = { Text("Search clients...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = OutlineColor
                        )
                    )
                    val filteredClients = remember(searchQuery, clients) {
                        if (searchQuery.isEmpty()) {
                            clients
                        } else {
                            clients.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.phone.contains(searchQuery, ignoreCase = true) ||
                                it.companyName.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    if (filteredClients.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No clients found", style = AppTypography.bodyMedium, color = OnSurfaceVariantText) },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        filteredClients.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.phone})") },
                                onClick = {
                                    selectedClientId = c.id
                                    selectedDealId = null // reset deal selection
                                    clientDropdownExpanded = false
                                    searchQuery = ""
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Deal Link Dropdown Selector
            if (clientDeals.isNotEmpty()) {
                Text("Link to Deal (Optional)", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentDeal = clientDeals.find { it.id == selectedDealId }
                    OutlinedTextField(
                        value = currentDeal?.title ?: "Select Deal...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = OutlineColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { dealDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = dealDropdownExpanded,
                        onDismissRequest = { dealDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedDealId = null
                                dealDropdownExpanded = false
                            }
                        )
                        clientDeals.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.title) },
                                onClick = {
                                    selectedDealId = d.id
                                    dealDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Contact Method
            Text("Contact Method", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                methods.forEach { m ->
                    FilterChip(
                        selected = contactMethod == m,
                        onClick = { contactMethod = m },
                        label = { Text(m) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedFormTextField(
                value = discussion,
                onValueChange = { discussion = it },
                label = "What was discussed (Most Important field)",
                placeholder = "e.g., Client requested wholesale fabric catalogs and negotiated bulk shipping options...",
                singleLine = false,
                maxLines = 5,
                modifier = Modifier.height(140.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedFormTextField(
                value = productDiscussed,
                onValueChange = { productDiscussed = it },
                label = "Product / Service Discussed (Optional)",
                placeholder = "e.g., Premium Lawn Prints"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedFormTextField(
                value = priceOfferedStr,
                onValueChange = { priceOfferedStr = it },
                label = "Price Quoted/Offered (Optional)",
                placeholder = "e.g., 45000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Client Response
            Text("Client Response State", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                responses.forEach { r ->
                    FilterChip(
                        selected = clientResponse == r,
                        onClick = { clientResponse = r },
                        label = { Text(r) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedFormTextField(
                value = myNextStep,
                onValueChange = { myNextStep = it },
                label = "My Next Step",
                placeholder = "e.g., Discuss bulk discount with factory manager before Friday"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val clientSelected = clients.find { it.id == selectedClientId }
                    if (clientSelected == null) {
                        Toast.makeText(context, "Select a client first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (discussion.trim().isEmpty()) {
                        Toast.makeText(context, "Enter what was discussed", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val interaction = Interaction(
                        id = interactionId ?: 0,
                        clientId = selectedClientId,
                        dealId = selectedDealId,
                        dateTime = System.currentTimeMillis(),
                        contactMethod = contactMethod,
                        discussion = discussion.trim(),
                        priceOffered = cleanPriceInput(priceOfferedStr).toDoubleOrNull(),
                        productDiscussed = productDiscussed.trim().ifEmpty { null },
                        clientResponse = clientResponse,
                        myNextStep = myNextStep.trim()
                    )
                    viewModel.saveInteraction(interaction) {
                        Toast.makeText(context, "Interaction logged", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (isEdit) "Update Log" else "Log Interaction", fontWeight = FontWeight.Bold, color = OnPrimaryBlue)
            }
        }
    }
}

@Composable
fun InteractionCardRow(
    interaction: Interaction,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val icon = when (interaction.contactMethod.lowercase(Locale.getDefault())) {
        "call" -> Icons.Default.Call
        "whatsapp" -> Icons.AutoMirrored.Filled.Chat
        "meeting" -> Icons.Default.Groups
        "email" -> Icons.Default.Email
        else -> Icons.Default.Description
    }

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
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contacted via ${interaction.contactMethod}",
                        style = AppTypography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                    Text(
                        text = formatDate(interaction.dateTime),
                        style = AppTypography.bodySmall,
                        color = OnSurfaceVariantText
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = interaction.discussion,
                    style = AppTypography.bodyMedium,
                    color = OnSurfaceText
                )
                if (interaction.priceOffered != null || !interaction.productDiscussed.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!interaction.productDiscussed.isNullOrEmpty()) {
                            StatusChip(interaction.productDiscussed)
                        }
                        if (interaction.priceOffered != null) {
                            StatusChip("Offered: ${formatCurrency(interaction.priceOffered, "PKR")}")
                        }
                    }
                }
                if (interaction.myNextStep.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NextPlan, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Next step: ${interaction.myNextStep}",
                            style = AppTypography.bodySmall,
                            color = WarningAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Interaction",
                        tint = LostRed.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. FOLLOW-UP REMINDERS SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpListScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val followUps by viewModel.followUps.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Today", "Upcoming")

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    val now = System.currentTimeMillis()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEnd = todayStart + 86400000

    val overdueList = remember(followUps) {
        followUps.filter { !it.isDone && it.scheduledDateTime < now }.sortedBy { it.scheduledDateTime }
    }

    val todayList = remember(followUps) {
        followUps.filter { !it.isDone && it.scheduledDateTime in todayStart..todayEnd }.sortedBy { it.scheduledDateTime }
    }

    val upcomingList = remember(followUps) {
        followUps.filter { !it.isDone && it.scheduledDateTime > todayEnd }.sortedBy { it.scheduledDateTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Follow-Ups Schedule", fontWeight = FontWeight.Bold, color = OnSurfaceText, style = AppTypography.titleLarge) },
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
                onClick = { navController.navigate("add_edit_follow_up") },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Follow-Up", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceBg,
                contentColor = PrimaryBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScreenBg)
                ) {
                    if (page == 0) {
                        // Today Tab: Overdue + Due Today
                        if (overdueList.isEmpty() && todayList.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Outlined.CheckCircle,
                                title = "All caught up!",
                                description = "You have no outstanding follow-ups scheduled for today.",
                                buttonText = "Set Reminder",
                                onButtonClick = { navController.navigate("add_edit_follow_up") }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (overdueList.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Overdue Reminders (${overdueList.size})",
                                            color = LostRed,
                                            style = AppTypography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    items(overdueList) { item ->
                                        val clientName = clients.find { it.id == item.clientId }?.name ?: "Client"
                                        FollowUpCardRow(
                                            followUp = item,
                                            clientName = clientName,
                                            showClientName = true,
                                            onClientClick = {
                                                navController.navigate("client_profile/${item.clientId}")
                                            },
                                            onDone = { viewModel.markFollowUpDone(context, item) },
                                            onSnooze = { viewModel.snoozeFollowUp(context, item) },
                                            onClick = { navController.navigate("add_edit_follow_up?followUpId=${item.id}") }
                                        )
                                    }
                                }

                                if (todayList.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Due Today (${todayList.size})",
                                            color = PrimaryBlue,
                                            style = AppTypography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    items(todayList) { item ->
                                        val clientName = clients.find { it.id == item.clientId }?.name ?: "Client"
                                        FollowUpCardRow(
                                            followUp = item,
                                            clientName = clientName,
                                            showClientName = true,
                                            onClientClick = {
                                                navController.navigate("client_profile/${item.clientId}")
                                            },
                                            onDone = { viewModel.markFollowUpDone(context, item) },
                                            onSnooze = { viewModel.snoozeFollowUp(context, item) },
                                            onClick = { navController.navigate("add_edit_follow_up?followUpId=${item.id}") }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Upcoming Tab
                        if (upcomingList.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Outlined.CalendarMonth,
                                title = "Nothing scheduled",
                                description = "Schedule reminders to follow up on open negotiations.",
                                buttonText = "Set Reminder",
                                onButtonClick = { navController.navigate("add_edit_follow_up") }
                            )
                        } else {
                            // Group by date
                            val grouped = upcomingList.groupBy { formatDate(it.scheduledDateTime) }
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                grouped.forEach { (dateStr, items) ->
                                    item {
                                        Text(
                                            text = dateStr,
                                            color = OnSurfaceText,
                                            style = AppTypography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    items(items) { item ->
                                        val clientName = clients.find { it.id == item.clientId }?.name ?: "Client"
                                        FollowUpCardRow(
                                            followUp = item,
                                            clientName = clientName,
                                            showClientName = true,
                                            onClientClick = {
                                                navController.navigate("client_profile/${item.clientId}")
                                            },
                                            onDone = { viewModel.markFollowUpDone(context, item) },
                                            onSnooze = { viewModel.snoozeFollowUp(context, item) },
                                            onClick = { navController.navigate("add_edit_follow_up?followUpId=${item.id}") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUpCardRow(
    followUp: FollowUp,
    clientName: String,
    showClientName: Boolean = true,
    onClientClick: (() -> Unit)? = null,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val borderAccent = when (followUp.priority.lowercase(Locale.getDefault())) {
        "urgent" -> LostRed
        "important" -> WarningAmber
        else -> OnHoldGray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left boundary priority color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(96.dp)
                    .background(borderAccent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (showClientName) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .then(
                                        if (onClientClick != null) Modifier.clickable { onClientClick() }
                                        else Modifier
                                    )
                            ) {
                                if (onClientClick != null) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = PrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = clientName,
                                    style = AppTypography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (onClientClick != null) PrimaryBlue else OnSurfaceText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = followUp.note,
                            style = AppTypography.bodyMedium,
                            color = OnSurfaceText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PriorityChip(followUp.priority)
                        if (onDelete != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Reminder",
                                    tint = LostRed.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDateTime(followUp.scheduledDateTime),
                        style = AppTypography.bodySmall,
                        color = OnSurfaceVariantText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSnooze,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Alarm, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Snooze", fontSize = 11.sp)
                        }
                        Button(
                            onClick = onDone,
                            colors = ButtonDefaults.buttonColors(containerColor = WonGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = OnWonGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", fontSize = 11.sp, color = OnWonGreen)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 9. ADD / EDIT FOLLOW-UP REMINDER SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFollowUpScreen(
    navController: NavController,
    viewModel: MainViewModel,
    followUpId: Int? = null,
    clientId: Int? = null
) {
    val context = LocalContext.current
    val followUps by viewModel.followUps.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()

    var selectedClientId by remember { mutableStateOf(clientId ?: 0) }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var scheduledDateTime by remember { mutableStateOf(System.currentTimeMillis() + 3600000) } // default 1 hour in future

    var isEdit by remember { mutableStateOf(false) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    val priorities = listOf("Normal", "Important", "Urgent")

    LaunchedEffect(followUpId, followUps) {
        if (followUpId != null && followUpId != 0) {
            val f = followUps.find { it.id == followUpId }
            if (f != null) {
                isEdit = true
                selectedClientId = f.clientId
                note = f.note
                priority = f.priority
                scheduledDateTime = f.scheduledDateTime
            }
        }
    }

    // Helper Dialog Pickers
    val cal = Calendar.getInstance().apply { timeInMillis = scheduledDateTime }
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            scheduledDateTime = cal.timeInMillis
        },
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        false
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            timePickerDialog.show() // Chain time picker automatically for fluid experience!
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Reminder" else "Schedule Reminder") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEdit) {
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete FollowUp", tint = LostRed)
                        }
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete Reminder?") },
                                text = { Text("Are you sure you want to permanently delete this reminder?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val f = followUps.find { it.id == followUpId }
                                            if (f != null) {
                                                viewModel.deleteFollowUp(context, f) {
                                                    Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show()
                                                    navController.navigateUp()
                                                }
                                            }
                                            showDeleteConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                                    ) { Text("Delete", color = OnLostRed) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                    TextButton(onClick = {
                        val clientSelected = clients.find { it.id == selectedClientId }
                        if (clientSelected == null) {
                            Toast.makeText(context, "Select a client first", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (note.trim().isEmpty()) {
                            Toast.makeText(context, "Enter a reminder note", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (scheduledDateTime < System.currentTimeMillis() && !isEdit) {
                            Toast.makeText(context, "Alarms must be scheduled in the future!", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        val f = FollowUp(
                            id = followUpId ?: 0,
                            clientId = selectedClientId,
                            scheduledDateTime = scheduledDateTime,
                            note = note.trim(),
                            priority = priority,
                            isDone = false,
                            alarmId = followUpId ?: Random().nextInt(1000000)
                        )
                        viewModel.saveFollowUp(context, f) {
                            Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
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
            // Client Dropdown Selector
            ClientSelectionHeader(
                selectedClientId = selectedClientId,
                onClientSelected = { selectedClientId = it },
                clients = clients,
                viewModel = viewModel,
                navController = navController
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                val currentClient = clients.find { it.id == selectedClientId }
                OutlinedTextField(
                    value = currentClient?.name ?: "Select Client...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = OutlineColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { clientDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = clientDropdownExpanded,
                    onDismissRequest = { clientDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    var searchQuery by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        placeholder = { Text("Search clients...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = OutlineColor
                        )
                    )
                    val filteredClients = remember(searchQuery, clients) {
                        if (searchQuery.isEmpty()) {
                            clients
                        } else {
                            clients.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.phone.contains(searchQuery, ignoreCase = true) ||
                                it.companyName.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    if (filteredClients.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No clients found", style = AppTypography.bodyMedium, color = OnSurfaceVariantText) },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        filteredClients.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.phone})") },
                                onClick = {
                                    selectedClientId = c.id
                                    clientDropdownExpanded = false
                                    searchQuery = ""
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date / Time Trigger Picker
            Text("Scheduled Reminder Time", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = formatDateTime(scheduledDateTime),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.DateRange, null) } },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = OutlineColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Level
            Text("Priority Alert Level", style = AppTypography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                priorities.forEach { p ->
                    val color = when (p) {
                        "Urgent" -> LostRed
                        "Important" -> WarningAmber
                        else -> OnHoldGray
                    }
                    val isSelected = priority == p

                    FilterChip(
                        selected = isSelected,
                        onClick = { priority = p },
                        label = { Text(p) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedFormTextField(
                value = note,
                onValueChange = { note = it },
                label = "Reminder Note (What to talk about)",
                placeholder = "e.g., Ask if he reviewed the pricing and call to confirm boutique fabric receipt...",
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.height(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val clientSelected = clients.find { it.id == selectedClientId }
                    if (clientSelected == null) {
                        Toast.makeText(context, "Select a client first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (note.trim().isEmpty()) {
                        Toast.makeText(context, "Enter a reminder note", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (scheduledDateTime < System.currentTimeMillis() && !isEdit) {
                        Toast.makeText(context, "Alarms must be scheduled in the future!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val f = FollowUp(
                        id = followUpId ?: 0,
                        clientId = selectedClientId,
                        scheduledDateTime = scheduledDateTime,
                        note = note.trim(),
                        priority = priority,
                        isDone = false,
                        alarmId = followUpId ?: Random().nextInt(1000000)
                    )
                    viewModel.saveFollowUp(context, f) {
                        Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (isEdit) "Update Reminder" else "Schedule Reminder", fontWeight = FontWeight.Bold, color = OnPrimaryBlue)
            }
        }
    }
}

// -------------------------------------------------------------
// 10. SETTINGS SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val deals by viewModel.deals.collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val success = backupDatabase(context, outputStream)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(context, "Database backup exported successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to export backup file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Backup Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val success = restoreDatabase(context, inputStream)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(context, "Database restored! Restarting app...", Toast.LENGTH_LONG).show()
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                // Terminate current process to guarantee a clean start with fresh singletons and view models
                                android.os.Process.killProcess(android.os.Process.myPid())
                            } else {
                                Toast.makeText(context, "Failed to restore database from backup file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Restore Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    var showClearConfirm by remember { mutableStateOf(false) }
    var ownerNameInput by remember { mutableStateOf("") }
    var businessNameInput by remember { mutableStateOf("") }
    var currencyInput by remember { mutableStateOf("") }
    var snoozeMinutesInput by remember { mutableStateOf("") }

    var isEditingProfile by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        ownerNameInput = settings.ownerName
        businessNameInput = settings.businessName
        currencyInput = settings.currency
        snoozeMinutesInput = settings.snoozeMinutes.toString()
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear/Reset App Database?") },
            text = { Text("This will permanently delete all your current clients, registered deals, follow-up alarms, and histories. Choose whether you want a completely empty database, or a reset with fresh Demo Data.") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clear Completely
                    Button(
                        onClick = {
                            viewModel.clearAllData(keepDemoSeeding = false) {
                                showClearConfirm = false
                                Toast.makeText(context, "All app data cleared successfully (Database is now empty)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LostRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Completely (Blank Database)", color = OnLostRed)
                    }

                    // Reset with Demo Data
                    Button(
                        onClick = {
                            viewModel.clearAllData(keepDemoSeeding = true) {
                                showClearConfirm = false
                                Toast.makeText(context, "Database reset with fresh demo data", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset with Demo Data", color = OnPrimaryBlue)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirm = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", textAlign = TextAlign.Center)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile", fontWeight = FontWeight.Bold, color = OnSurfaceText, style = AppTypography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PrimaryBlue)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Business Profile Header Card
            Text("Business Profile", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                elevation = CardDefaults.cardElevation(1.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isEditingProfile) {
                        Text("Business Name: ${settings.businessName}", fontWeight = FontWeight.SemiBold)
                        Text("Owner Name: ${settings.ownerName}")
                        Text("Preferred Currency: ${settings.currency}")
                        Text("Default Snooze: ${settings.snoozeMinutes} Minutes")
                        Button(
                            onClick = { isEditingProfile = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = OnPrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile Details", color = OnPrimaryBlue)
                        }
                    } else {
                        OutlinedTextField(
                            value = businessNameInput,
                            onValueChange = { businessNameInput = it },
                            label = { Text("Business Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ownerNameInput,
                            onValueChange = { ownerNameInput = it },
                            label = { Text("Owner Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = currencyInput,
                            onValueChange = { currencyInput = it },
                            label = { Text("Currency Symbol (e.g., PKR, USD, AED)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = snoozeMinutesInput,
                            onValueChange = { snoozeMinutesInput = it },
                            label = { Text("Snooze Reminders (Minutes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val minutes = snoozeMinutesInput.toIntOrNull() ?: 30
                                    val updated = settings.copy(
                                        businessName = businessNameInput.trim().ifEmpty { "My Business" },
                                        ownerName = ownerNameInput.trim().ifEmpty { "Owner" },
                                        currency = currencyInput.trim().ifEmpty { "PKR" },
                                        snoozeMinutes = minutes
                                    )
                                    viewModel.updateSettings(updated) {
                                        isEditingProfile = false
                                        Toast.makeText(context, "Business Profile Saved", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WonGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Save", color = OnWonGreen) }
                            OutlinedButton(
                                onClick = { isEditingProfile = false },
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Cancel") }
                        }
                    }
                }
            }

            // CRM Field Customization Card
            Text("CRM Field Customization", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                elevation = CardDefaults.cardElevation(1.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Deal Stages Customizer
                    val stagesList = remember(settings.customStages) {
                        settings.customStages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    CustomFieldSection(
                        title = "Deal Stages",
                        items = stagesList,
                        onAdd = { newItem ->
                            val updatedList = stagesList + newItem
                            viewModel.updateSettings(settings.copy(customStages = updatedList.joinToString(",")))
                        },
                        onEdit = { index, editedItem ->
                            val updatedList = stagesList.toMutableList()
                            updatedList[index] = editedItem
                            viewModel.updateSettings(settings.copy(customStages = updatedList.joinToString(",")))
                        },
                        onDelete = { index ->
                            val updatedList = stagesList.toMutableList()
                            updatedList.removeAt(index)
                            viewModel.updateSettings(settings.copy(customStages = updatedList.joinToString(",")))
                        }
                    )

                    HorizontalDivider(color = OutlineColor.copy(alpha = 0.5f))

                    // Overall Status Flow Customizer
                    val statusesList = remember(settings.customStatuses) {
                        settings.customStatuses.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    CustomFieldSection(
                        title = "Overall Status Flow",
                        items = statusesList,
                        onAdd = { newItem ->
                            val updatedList = statusesList + newItem
                            viewModel.updateSettings(settings.copy(customStatuses = updatedList.joinToString(",")))
                        },
                        onEdit = { index, editedItem ->
                            val updatedList = statusesList.toMutableList()
                            updatedList[index] = editedItem
                            viewModel.updateSettings(settings.copy(customStatuses = updatedList.joinToString(",")))
                        },
                        onDelete = { index ->
                            val updatedList = statusesList.toMutableList()
                            updatedList.removeAt(index)
                            viewModel.updateSettings(settings.copy(customStatuses = updatedList.joinToString(",")))
                        }
                    )

                    HorizontalDivider(color = OutlineColor.copy(alpha = 0.5f))

                    // Client Types Customizer
                    val typesList = remember(settings.customClientTypes) {
                        settings.customClientTypes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    CustomFieldSection(
                        title = "Client Types",
                        items = typesList,
                        onAdd = { newItem ->
                            val updatedList = typesList + newItem
                            viewModel.updateSettings(settings.copy(customClientTypes = updatedList.joinToString(",")))
                        },
                        onEdit = { index, editedItem ->
                            val updatedList = typesList.toMutableList()
                            updatedList[index] = editedItem
                            viewModel.updateSettings(settings.copy(customClientTypes = updatedList.joinToString(",")))
                        },
                        onDelete = { index ->
                            val updatedList = typesList.toMutableList()
                            updatedList.removeAt(index)
                            viewModel.updateSettings(settings.copy(customClientTypes = updatedList.joinToString(",")))
                        }
                    )
                }
            }

            // Customization Options
            Text("Data Management", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                elevation = CardDefaults.cardElevation(1.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Clear Database
                    Button(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LostRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, null, tint = LostRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All App Data", color = LostRed, fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(color = OutlineColor.copy(alpha = 0.5f))

                    Text("Full Database Backup & Restore", style = AppTypography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Text("Copy all app data (clients, deals, interactions, and settings) to a downloadable backup file, or restore from a previously saved backup file.", style = AppTypography.bodySmall, color = OnSurfaceVariantText)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                createDocumentLauncher.launch("track_deals_backup.db")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, null, tint = OnWonGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", color = OnWonGreen, style = AppTypography.labelMedium)
                        }

                        Button(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileOpen, null, tint = OnPrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup", color = OnPrimaryBlue, style = AppTypography.labelMedium)
                        }
                    }
                }
            }

            // About footer info
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TrackDeals Client Relationship Manager", style = AppTypography.bodySmall, color = OnSurfaceVariantText)
                Text("Version 1.0.0 (Offline Native Core)", style = AppTypography.bodySmall, color = OnSurfaceVariantText)
            }
        }
    }
}

// -------------------------------------------------------------
// 11. GLOBAL SEARCH SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val deals by viewModel.deals.collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }

    val matchedClients = remember(clients, query) {
        if (query.trim().isEmpty()) emptyList() else clients.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.phone.contains(query, ignoreCase = true) ||
                    it.companyName.contains(query, ignoreCase = true) ||
                    it.city.contains(query, ignoreCase = true)
        }
    }

    val matchedDeals = remember(deals, query) {
        if (query.trim().isEmpty()) emptyList() else deals.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.notes.contains(query, ignoreCase = true)
        }
    }

    val matchedInteractions = remember(interactions, query) {
        if (query.trim().isEmpty()) emptyList() else interactions.filter {
            it.discussion.contains(query, ignoreCase = true) ||
                    it.myNextStep.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search CRM Globally...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ScreenBg)
        ) {
            if (query.trim().isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Search,
                    title = "Global CRM Search",
                    description = "Search across client name, phone numbers, deals or logged interactions in one go."
                )
            } else if (matchedClients.isEmpty() && matchedDeals.isEmpty() && matchedInteractions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Info,
                    title = "No results found",
                    description = "Try typing some other client names, products, or keyword discussions."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (matchedClients.isNotEmpty()) {
                        item {
                            Text("Matched Clients", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        items(matchedClients) { c ->
                            ClientCard(c, 0, false) {
                                navController.navigate("client_profile/${c.id}")
                            }
                        }
                    }

                    if (matchedDeals.isNotEmpty()) {
                        item {
                            Text("Matched Deals", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        items(matchedDeals) { d ->
                            val clientName = clients.find { it.id == d.clientId }?.name ?: "Client"
                            DealCardRow(
                                deal = d,
                                currency = settings.currency,
                                clientName = clientName,
                                onClientClick = {
                                    navController.navigate("client_profile/${d.clientId}")
                                }
                            ) {
                                navController.navigate("add_edit_deal?dealId=${d.id}")
                            }
                        }
                    }

                    if (matchedInteractions.isNotEmpty()) {
                        item {
                            Text("Matched Discussions", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        items(matchedInteractions) { intr ->
                            InteractionCardRow(intr) {
                                navController.navigate("add_edit_interaction?interactionId=${intr.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomFieldSection(
    title: String,
    items: List<String>,
    onAdd: (String) -> Unit,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialogItemIndex by remember { mutableStateOf<Int?>(null) }
    var newValue by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = AppTypography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            TextButton(
                onClick = {
                    newValue = ""
                    showAddDialog = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add New", style = AppTypography.labelMedium)
            }
        }

        // List of chips (using a simple scrollable row to keep it clean and robust)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEachIndexed { index, item ->
                InputChip(
                    selected = false,
                    onClick = {
                        editValue = item
                        showEditDialogItemIndex = index
                    },
                    label = { Text(item) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (items.isEmpty()) {
                Text("None configured", style = AppTypography.bodySmall, color = OnSurfaceVariantText, modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New $title") },
                text = {
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newValue.trim().isNotEmpty()) {
                                onAdd(newValue.trim())
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WonGreen)
                    ) { Text("Add", color = OnWonGreen) }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showEditDialogItemIndex != null) {
            val index = showEditDialogItemIndex!!
            AlertDialog(
                onDismissRequest = { showEditDialogItemIndex = null },
                title = { Text("Edit or Delete Item") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Rename or delete \"${items[index]}\"", style = AppTypography.bodyMedium)
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = { editValue = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        // Delete Button
                        Button(
                            onClick = {
                                onDelete(index)
                                showEditDialogItemIndex = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LostRed)
                        ) { Text("Delete", color = OnLostRed) }

                        // Save Button
                        Button(
                            onClick = {
                                if (editValue.trim().isNotEmpty()) {
                                    onEdit(index, editValue.trim())
                                    showEditDialogItemIndex = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WonGreen)
                        ) { Text("Save", color = OnWonGreen) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialogItemIndex = null }) { Text("Cancel") }
                }
            )
        }
    }
}

fun backupDatabase(context: Context, outputStream: java.io.OutputStream): Boolean {
    return try {
        val db = AppDatabase.getDatabase(context)
        
        // Temporarily change journal mode to DELETE to merge the WAL into the main db file
        try {
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            db.openHelper.writableDatabase.query("PRAGMA journal_mode = DELETE").close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Close the database cleanly before copying to avoid in-use file lock issues and corruption
        AppDatabase.closeAndResetInstance()

        val dbFile = context.getDatabasePath("track_deals_database")
        val success = if (dbFile.exists()) {
            dbFile.inputStream().use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } else {
            false
        }

        // Restore journal mode back to WAL for performance by reopening database instance
        try {
            val dbNew = AppDatabase.getDatabase(context)
            dbNew.openHelper.writableDatabase.query("PRAGMA journal_mode = WAL").close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        success
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun isValidSqliteFile(file: File): Boolean {
    if (!file.exists() || file.length() < 16) return false
    val header = ByteArray(16)
    try {
        file.inputStream().use { input ->
            val read = input.read(header)
            if (read < 16) return false
        }
    } catch (e: Exception) {
        return false
    }
    
    val expected = byteArrayOf(
        0x53, 0x51, 0x4c, 0x69, 0x74, 0x65, 0x20, 0x66, 
        0x6f, 0x72, 0x6d, 0x61, 0x74, 0x20, 0x33, 0x00
    ) // "SQLite format 3\u0000"
    if (!header.contentEquals(expected)) return false

    // Try opening as an SQLite database to ensure it's not corrupted and contains our clients table
    return try {
        android.database.sqlite.SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        ).use { db ->
            var hasClientsTable = false
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='clients'", null).use { cursor ->
                hasClientsTable = cursor.moveToFirst()
            }
            hasClientsTable
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun restoreDatabase(context: Context, inputStream: java.io.InputStream): Boolean {
    val tempFile = File(context.cacheDir, "temp_restore.db")
    val dbFile = context.getDatabasePath("track_deals_database")
    val walFile = File(dbFile.path + "-wal")
    val shmFile = File(dbFile.path + "-shm")
    val journalFile = File(dbFile.path + "-journal")

    val backupDbFile = File(dbFile.path + ".bak")
    val backupWalFile = File(dbFile.path + "-wal.bak")
    val backupShmFile = File(dbFile.path + "-shm.bak")
    val backupJournalFile = File(dbFile.path + "-journal.bak")

    var backupCreated = false

    return try {
        // 1. Copy input stream to temp file
        tempFile.outputStream().use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }

        // 2. Validate temp file is a valid, uncorrupted SQLite DB
        if (!isValidSqliteFile(tempFile)) {
            if (tempFile.exists()) tempFile.delete()
            return false
        }

        // 3. Flush live database WAL before closing to make sure main file is up to date
        try {
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Close database connection cleanly to release all file locks and complete WAL checkpoint
        AppDatabase.closeAndResetInstance()

        // 5. Backup the current live database files
        if (dbFile.exists()) {
            dbFile.inputStream().use { input ->
                backupDbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            backupCreated = true
        }
        if (walFile.exists()) {
            walFile.inputStream().use { input ->
                backupWalFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        if (shmFile.exists()) {
            shmFile.inputStream().use { input ->
                backupShmFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        if (journalFile.exists()) {
            journalFile.inputStream().use { input ->
                backupJournalFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        // 6. Delete live database and support files
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()
        if (journalFile.exists()) journalFile.delete()
        if (dbFile.exists()) dbFile.delete()

        // 7. Copy temp file to live database path
        tempFile.inputStream().use { input ->
            dbFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 8. Cleanup temp file & backup files on successful restore
        tempFile.delete()
        if (backupDbFile.exists()) backupDbFile.delete()
        if (backupWalFile.exists()) backupWalFile.delete()
        if (backupShmFile.exists()) backupShmFile.delete()
        if (backupJournalFile.exists()) backupJournalFile.delete()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        if (tempFile.exists()) tempFile.delete()

        // 9. Rollback transaction: If anything went wrong, restore original live database files from backups
        if (backupCreated && backupDbFile.exists()) {
            try {
                AppDatabase.closeAndResetInstance()

                if (dbFile.exists()) dbFile.delete()
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()
                if (journalFile.exists()) journalFile.delete()

                backupDbFile.inputStream().use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (backupWalFile.exists()) {
                    backupWalFile.inputStream().use { input ->
                        walFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (backupShmFile.exists()) {
                    backupShmFile.inputStream().use { input ->
                        shmFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (backupJournalFile.exists()) {
                    backupJournalFile.inputStream().use { input ->
                        journalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (rollbackEx: Exception) {
                rollbackEx.printStackTrace()
            } finally {
                // Ensure backups are cleaned up after rollback
                if (backupDbFile.exists()) backupDbFile.delete()
                if (backupWalFile.exists()) backupWalFile.delete()
                if (backupShmFile.exists()) backupShmFile.delete()
                if (backupJournalFile.exists()) backupJournalFile.delete()
            }
        }
        false
    }
}

@Composable
fun ClientSelectionHeader(
    selectedClientId: Int,
    onClientSelected: (Int) -> Unit,
    clients: List<Client>,
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var showQuickAddClientDialog by remember { mutableStateOf(false) }
    var quickClientName by remember { mutableStateOf("") }
    var quickClientPhone by remember { mutableStateOf("") }
    var quickClientCompany by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            try {
                var name = ""
                var phone = ""
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                    if (idIndex != -1 && nameIndex != -1) {
                        name = cursor.getString(nameIndex)

                        val hasPhoneIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val hasPhone = if (hasPhoneIndex != -1) cursor.getString(hasPhoneIndex) else "0"

                        if (hasPhone == "1") {
                            val contactId = cursor.getString(idIndex)
                            val phonesCursor = contentResolver.query(
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(contactId),
                                null
                            )
                            if (phonesCursor != null && phonesCursor.moveToFirst()) {
                                val numberIndex = phonesCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numberIndex != -1) {
                                    phone = phonesCursor.getString(numberIndex)
                                }
                                phonesCursor.close()
                            }
                        }
                    }
                    cursor.close()
                }
                
                if (name.isNotEmpty()) {
                    val newClient = Client(
                        name = name,
                        phone = phone,
                        clientType = "Prospect",
                        city = "",
                        companyName = "",
                        source = "Phone Book",
                        notes = "Imported from Phone Book"
                    )
                    viewModel.saveClient(newClient) { newId ->
                        onClientSelected(newId.toInt())
                        Toast.makeText(context, "Client added: $name", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ContactPicker", "Error picking contact", e)
                Toast.makeText(context, "Error picking contact: ${e.message}", Toast.LENGTH_SHORT).show()
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

    if (showQuickAddClientDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddClientDialog = false },
            title = { Text("Quick Add Client") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quickClientName,
                        onValueChange = { quickClientName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = OutlineColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quickClientPhone,
                        onValueChange = { quickClientPhone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = OutlineColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quickClientCompany,
                        onValueChange = { quickClientCompany = it },
                        label = { Text("Company (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = OutlineColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickClientName.isBlank()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        } else {
                            val newClient = Client(
                                name = quickClientName.trim(),
                                phone = quickClientPhone.trim(),
                                companyName = quickClientCompany.trim(),
                                clientType = "Prospect"
                            )
                            viewModel.saveClient(newClient) { newId ->
                                onClientSelected(newId.toInt())
                                showQuickAddClientDialog = false
                                quickClientName = ""
                                quickClientPhone = ""
                                quickClientCompany = ""
                                Toast.makeText(context, "Client created & selected", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Add & Select", color = OnPrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddClientDialog = false }) {
                    Text("Cancel", color = PrimaryBlue)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Client Partner",
                style = AppTypography.bodySmall,
                color = PrimaryBlue,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedClientId != 0) {
                    IconButton(
                        onClick = {
                            navController.navigate("client_profile/$selectedClientId")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = "View Profile",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
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
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = "Add from Contacts",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        showQuickAddClientDialog = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Quick New Client",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
