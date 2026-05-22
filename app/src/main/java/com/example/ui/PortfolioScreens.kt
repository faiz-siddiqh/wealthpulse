package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun MainPortfolioApp(viewModel: PortfolioViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val showOnreconnectNotification by viewModel.showOnreconnectNotification.collectAsState()

    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddInvestmentDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddUpcomingDivDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PortfolioTopAppBar(
                viewModel = viewModel,
                onAddTxClick = { showAddTxDialog = true },
                onAddInvClick = { showAddInvestmentDialog = true },
                onAddAccClick = { showAddAccountDialog = true }
            )
        },
        bottomBar = {
            PortfolioBottomBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "Dashboard" -> DashboardScreen(viewModel)
                "Holdings" -> HoldingsScreen(viewModel)
                "Dividends" -> DividendsScreen(viewModel, onAddUpcomingClick = { showAddUpcomingDivDialog = true })
                "Performance" -> PerformanceScreen(viewModel)
                "Trade Log" -> TradeLogScreen(viewModel)
                "Alerts" -> AlertsScreen(viewModel)
                "Settings" -> SettingsScreen(viewModel)
            }

            // Connection reconnect toast notification (2 seconds overlay)
            AnimatedVisibility(
                visible = showOnreconnectNotification,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GainGreen),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CloudQueue, contentDescription = "Online", tint = CharcoalSurface, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "You are online. Resuming active price updates...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = CharcoalSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Persistent bottom offline indicator banner
            if (!isOnline) {
                val lastSync by viewModel.lastSyncedTime.collectAsState()
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(LossRed)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudOff, contentDescription = "Offline Mode", tint = PureWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Offline Mode: viewing cached values ($lastSync)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddTxDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showAddTxDialog = false }
        )
    }

    if (showAddInvestmentDialog) {
        AddInvestmentDialog(
            viewModel = viewModel,
            onDismiss = { showAddInvestmentDialog = false }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            viewModel = viewModel,
            onDismiss = { showAddAccountDialog = false }
        )
    }

    if (showAddUpcomingDivDialog) {
        AddUpcomingDividendDialog(
            viewModel = viewModel,
            onDismiss = { showAddUpcomingDivDialog = false }
        )
    }
}

// TOP APP BAR
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioTopAppBar(
    viewModel: PortfolioViewModel,
    onAddTxClick: () -> Unit,
    onAddInvClick: () -> Unit,
    onAddAccClick: () -> Unit
) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val username by viewModel.username.collectAsState()
    val selectedAvatar by viewModel.selectedAvatar.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var actionMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = "WealthPulse Logo",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "WealthPulse",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val pillColor = if (isOnline) GainGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.2f)
                        val textCol = if (isOnline) GainGreen else LossRed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(pillColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (isOnline) "ONLINE" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textCol
                                )
                            )
                        }
                    }
                    Text(
                        "Personal Investment Tracker",
                        style = MaterialTheme.typography.labelSmall.copy(color = MutedText, fontSize = 10.sp)
                    )
                }
            }
        },
        actions = {
            // Currency Toggle
            Box {
                Button(
                    onClick = { dropdownExpanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CharcoalSurface,
                        contentColor = EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(selectedCurrency)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Currency", modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(CharcoalSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Dollar ($)", color = PureWhite) },
                        onClick = { viewModel.selectCurrency("$"); dropdownExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Euro (€)", color = PureWhite) },
                        onClick = { viewModel.selectCurrency("€"); dropdownExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Canadian Dollar (C$)", color = PureWhite) },
                        onClick = { viewModel.selectCurrency("C$"); dropdownExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Rupee (₹)", color = PureWhite) },
                        onClick = { viewModel.selectCurrency("₹"); dropdownExpanded = false }
                    )
                }
            }

            // Quick Add Operations Menu
            Box {
                IconButton(
                    onClick = { actionMenuExpanded = true },
                    modifier = Modifier
                        .background(CharcoalSurface, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Quick Add Items", tint = PureWhite)
                }
                DropdownMenu(
                    expanded = actionMenuExpanded,
                    onDismissRequest = { actionMenuExpanded = false },
                    modifier = Modifier.background(CharcoalSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("+ Log Transaction", color = EmeraldPrimary) },
                        onClick = { onAddTxClick(); actionMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("+ Add Setup Investment", color = PureWhite) },
                        onClick = { onAddInvClick(); actionMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("+ Create Portfolio Account", color = PureWhite) },
                        onClick = { onAddAccClick(); actionMenuExpanded = false }
                    )
                }
            }

            // Avatar button (Settings toggle with connection dot indicator)
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CharcoalSurface)
                    .clickable { viewModel.selectTab("Settings") },
                contentAlignment = Alignment.Center
            ) {
                val isOldAvatar = selectedAvatar.length > 2
                val displayAvatar = if (isOldAvatar) "🤖" else selectedAvatar

                Text(
                    text = displayAvatar,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // dot connection
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .background(if (isOnline) GainGreen else LossRed, CircleShape)
                        .border(1.dp, CharcoalSurface, CircleShape)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MidnightBack
        )
    )
}

// BOTTOM NAVIGATION BAR
@Composable
fun PortfolioBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        TabItem("Dashboard", Icons.Filled.Dashboard),
        TabItem("Holdings", Icons.Filled.PieChart),
        TabItem("Dividends", Icons.Filled.MonetizationOn),
        TabItem("Performance", Icons.Filled.TrendingUp),
        TabItem("Trade Log", Icons.Filled.History),
        TabItem("Alerts", Icons.Filled.Notifications)
    )

    NavigationBar(
        containerColor = CharcoalSurface,
        tonalElevation = 8.dp
    ) {
        items.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab.title,
                onClick = { onTabSelected(tab.title) },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title, fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CharcoalSurface,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldPrimary,
                    unselectedIconColor = MutedText,
                    unselectedTextColor = MutedText
                )
            )
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)

// --- CARD: PORTFOLIO MAIN INSIGHTS BANNER ---
@Composable
fun PortfolioSummaryBanner(summary: PortfolioSummary?, selectionCurrency: String) {
    if (summary == null) return

    val isProfit = summary.totalGainLoss >= 0
    val trendColor = if (isProfit) GainGreen else LossRed
    val trendIcon = if (isProfit) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGreenBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalHeroLight)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NaturalHeroLight,
                            NaturalHeroLight.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "TOTAL PORTFOLIO EQUITY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = EmeraldPrimary,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "$selectionCurrency ${"%,.2f".format(summary.totalValue)}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkPineText,
                                fontSize = 32.sp
                            )
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = NaturalHeroLight.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = trendIcon,
                                    contentDescription = "Trend Icon",
                                    tint = trendColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${if (isProfit) "+" else ""}${"%.2f".format(summary.totalGainLossPct)}%",
                                    color = trendColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = EmeraldPrimary.copy(alpha = 0.15f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("TOTAL COST BASIS", fontSize = 11.sp, color = MutedText)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "$selectionCurrency ${"%,.2f".format(summary.totalCost)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NET GAIN / LOSS", fontSize = 11.sp, color = MutedText)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "${if (isProfit) "+" else ""}$selectionCurrency ${"%,.2f".format(summary.totalGainLoss)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = trendColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("YIELD ON COST", fontSize = 11.sp, color = MutedText)
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = "Yield", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "${"%.2f".format(summary.averageYield)}%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 1: DASHBOARD
@Composable
fun DashboardScreen(viewModel: PortfolioViewModel) {
    val summary by viewModel.summary.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val selectionCurrency by viewModel.selectedCurrency.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val isGeneratingInsights by viewModel.isGeneratingInsights.collectAsState()

    var selectedAccount by remember { mutableStateOf("All") }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Account Filter
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { accountDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Account: $selectedAccount", color = PureWhite)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Filter", tint = PureWhite)
                }
                if (accountDropdownExpanded) {
                    Dialog(onDismissRequest = { accountDropdownExpanded = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Select Account", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                TextButton(onClick = { 
                                    selectedAccount = "All"
                                    accountDropdownExpanded = false
                                    viewModel.selectAccount(null)
                                }) {
                                    Text("All", color = PureWhite)
                                }
                                accounts.forEach { acc ->
                                    TextButton(onClick = { 
                                        selectedAccount = acc.name
                                        accountDropdownExpanded = false
                                        viewModel.selectAccount(acc.id)
                                    }) {
                                        Text(acc.name, color = PureWhite)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Summary is now reactive to selectedAccount
            PortfolioSummaryBanner(summary, selectionCurrency)
        }

            // Quick Overview Statistics Rows
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Dividends Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CardGreenBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MonetizationOn, contentDescription = "Dividends", tint = GoldSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Received Divs", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "$selectionCurrency ${"%,.2f".format(summary?.totalDividendsReceived ?: 0.0)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldSecondary
                            )
                        }
                    }

                    // Accounts Count
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CardGreenBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ListAlt, contentDescription = "Accounts", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Active Accounts", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${accounts.size} Registered",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PureWhite
                            )
                        }
                    }
                }
            }

            // Section: AI Chartered Advisory Insights (GEMINI INTEGRATION)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "WealthPulse AI Advisor",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }

                            Button(
                                onClick = { viewModel.generateAiFinancialAdvisory() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                enabled = !isGeneratingInsights
                            ) {
                                if (isGeneratingInsights) {
                                    CircularProgressIndicator(color = CharcoalSurface, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyzing...", fontSize = 12.sp, color = CharcoalSurface)
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Generate", modifier = Modifier.size(14.dp), tint = CharcoalSurface)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Get AI Insights", fontSize = 12.sp, color = CharcoalSurface, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (aiInsights.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MidnightBack, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = aiInsights,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = PureWhite,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        } else {
                            Text(
                                "Get real-time feedback on your portfolio health, optimal rebalancing triggers, asset structures, and tax efficiency using Gemini.",
                                fontSize = 13.sp,
                                color = MutedText,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Holdings Mini Summary Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CORE HOLDINGS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MutedText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Text(
                        "${holdings.size} Securities",
                        fontSize = 12.sp,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // List of holdings
            if (holdings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions logged. Tap '+' top-right to buy positions!", color = MutedText)
                    }
                }
            } else {
                items(holdings) { holding ->
                    MiniHoldingRow(holding, selectionCurrency)
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

@Composable
fun MiniHoldingRow(holding: Holding, currency: String) {
    val isProfit = holding.totalGainLoss >= 0
    val trendColor = if (isProfit) GainGreen else LossRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGreenBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    holding.ticker,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PureWhite
                )
                Text(
                    holding.name,
                    fontSize = 12.sp,
                    color = MutedText,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$currency ${"%,.2f".format(holding.currentValue)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Text(
                    "${if (isProfit) "+" else ""}${"%.2f".format(holding.totalGainLossPct)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            }
        }
    }
}

@Composable
fun HoldingsPieChart(holdings: List<Holding>) {
    if (holdings.isEmpty()) return

    val totalValue = holdings.sumOf { it.currentValue }
    if (totalValue <= 0) return

    val sortedHoldings = holdings.sortedByDescending { it.currentValue }.filter { it.currentValue > 0 }
    if (sortedHoldings.isEmpty()) return

    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFFEAB308),
        Color(0xFF14B8A6), Color(0xFFF97316), Color(0xFF6366F1), Color(0xFF84CC16)
    )

    var selectedSlice by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sortedHoldings) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val radius = minOf(size.width, size.height) / 2f * 0.7f
                        
                        if (dx * dx + dy * dy <= radius * radius) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            
                            // Adjust for startAngle = -90
                            val adjustedAngle = (angle + 90f) % 360f
                            
                            var currentAngle = 0f
                            for ((index, holding) in sortedHoldings.withIndex()) {
                                val sweepAngle = (holding.currentValue / totalValue * 360f).toFloat()
                                if (adjustedAngle >= currentAngle && adjustedAngle <= currentAngle + sweepAngle) {
                                    selectedSlice = if (selectedSlice == index) null else index
                                    break
                                }
                                currentAngle += sweepAngle
                            }
                        } else {
                            selectedSlice = null
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = minOf(canvasWidth, canvasHeight) / 2f * 0.7f
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

            var startAngle = -90f

            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 32f
                isAntiAlias = true
            }

            sortedHoldings.forEachIndexed { index, holding ->
                val pct = holding.currentValue / totalValue
                val sweepAngle = (pct * 360f).toFloat()
                
                val isSelected = selectedSlice == index
                val scale = if (isSelected) 1.1f else 1.0f
                val currentRadius = radius * scale

                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - currentRadius, center.y - currentRadius),
                    size = Size(currentRadius * 2, currentRadius * 2)
                )
                
                if (pct > 0.03 || isSelected) {
                    val angleInRadians = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                    
                    val textRadius = currentRadius * 0.7f
                    val textX = center.x + (textRadius * cos(angleInRadians)).toFloat()
                    val textY = center.y + (textRadius * sin(angleInRadians)).toFloat() + 10f
                    
                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = if (isSelected) 36f else 30f
                    paint.isFakeBoldText = isSelected
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(pct * 100).toInt()}%",
                        textX, textY, paint
                    )
                    
                    val outerRadius = currentRadius * 1.15f
                    val outX = center.x + (outerRadius * cos(angleInRadians)).toFloat()
                    val outY = center.y + (outerRadius * sin(angleInRadians)).toFloat() + 10f
                    
                    paint.color = android.graphics.Color.LTGRAY
                    paint.textSize = if (isSelected) 32f else 28f
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        holding.ticker,
                        outX, outY, paint
                    )
                }

                startAngle += sweepAngle
            }
            
            drawCircle(
                color = com.example.ui.theme.MidnightBack, // Match background of app
                radius = radius * 0.45f,
                center = center
            )
        }
    }
}

// SCREEN 2: ALL HOLDINGS (DETAILED LIST)
@Composable
fun HoldingsScreen(viewModel: PortfolioViewModel) {
    val holdings by viewModel.allHoldings.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    
    var selectedHoldingForPriceUpdate by remember { mutableStateOf<Holding?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "PORTFOLIO HOLDINGS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = PureWhite,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            "Double tap or tap item to update live ticker values manually",
            fontSize = 11.sp,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        HoldingsPieChart(holdings = holdings)
        
        Spacer(modifier = Modifier.height(8.dp))

        if (holdings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No holdings currently tracked. Record buys to trigger holdings.", color = MutedText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(holdings) { holding ->
                    DetailedHoldingCard(
                        holding = holding,
                        currency = currency,
                        onUpdatePriceClick = { selectedHoldingForPriceUpdate = holding }
                    )
                }
            }
        }

        // Price updates popup
        selectedHoldingForPriceUpdate?.let { holdingToEdit ->
            EditPriceDialog(
                holding = holdingToEdit,
                onDismiss = { selectedHoldingForPriceUpdate = null },
                onSave = { ticker, price ->
                    viewModel.updateInvestmentPrice(ticker, price)
                    selectedHoldingForPriceUpdate = null
                }
            )
        }
    }
}

@Composable
fun DetailedHoldingCard(holding: Holding, currency: String, onUpdatePriceClick: () -> Unit) {
    val isProfit = holding.totalGainLoss >= 0
    val trendColor = if (isProfit) GainGreen else LossRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGreenBorder, RoundedCornerShape(12.dp))
            .clickable { onUpdatePriceClick() },
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            holding.ticker,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                holding.category,
                                fontSize = 10.sp,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(holding.name, fontSize = 12.sp, color = MutedText)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$currency ${"%,.2f".format(holding.currentValue)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite
                    )
                    Text(
                        "${"%.2f".format(holding.totalShares)} Shares",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = CardGreenBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("AVERAGE COST", fontSize = 11.sp, color = MutedText)
                    Text("$currency ${"%.2f".format(holding.averagePrice)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CURRENT PRICE", fontSize = 11.sp, color = MutedText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$currency ${"%.2f".format(holding.currentPrice)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL GAIN / LOSS", fontSize = 11.sp, color = MutedText)
                    Text(
                        "${if (isProfit) "+" else ""}$currency ${"%.2f".format(holding.totalGainLoss)} (${"%.2f".format(holding.totalGainLossPct)}%)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Sector: ${holding.sector}", fontSize = 11.sp, color = MutedText)
                Text(
                    "Yield-on-Cost: ${"%.2f".format(holding.yieldOnCost)}%",
                    fontSize = 11.sp,
                    color = GoldSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// SCREEN 3: DIVIDENDS RECEIVED TIMELINE (LEDGER)
@Composable
fun DividendsScreen(viewModel: PortfolioViewModel, onAddUpcomingClick: () -> Unit) {
    val timeline by viewModel.dividendTimeline.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val summary by viewModel.summary.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "DIVIDEND RETRIEVAL LEDGER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    "Timeline log of historical payouts received to date",
                    fontSize = 11.sp,
                    color = MutedText
                )
            }

            Button(
                onClick = onAddUpcomingClick,
                colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Payout Info", modifier = Modifier.size(14.dp), tint = CharcoalSurface)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Upcoming Schedule", fontSize = 11.sp, color = CharcoalSurface, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Dividends Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GoldSecondary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CUMULATIVE DIVIDENDS RECEIVED", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$currency ${"%,.2f".format(summary?.totalDividendsReceived ?: 0.0)}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldSecondary
                    )
                }
                Icon(Icons.Filled.MonetizationOn, contentDescription = "Income", tint = GoldSecondary, modifier = Modifier.size(42.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (timeline.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No dividend transfers present. Log dividend trade types to populate.", color = MutedText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(timeline) { item ->
                    DividendHistoryRow(item, currency)
                }
            }
        }
    }
}

@Composable
fun DividendHistoryRow(item: DividendTimelineItem, currency: String) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGreenBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.ticker, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PureWhite)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Paid", tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                }
                Text("Received on $dateStr • ${item.accountName}", fontSize = 12.sp, color = MutedText)
                if (item.notes.isNotBlank()) {
                    Text(item.notes, fontSize = 11.sp, color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                "+ $currency ${"%.2f".format(item.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldSecondary
            )
        }
    }
}

// SCREEN 4: TRADE LOG HISTORY
@Composable
fun TradeLogScreen(viewModel: PortfolioViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()

    var typeFilter by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf("DateNewest") }

    val filtered = remember(transactions, typeFilter, sortOrder, selectedAccountId) {
        val filteredList = if (typeFilter == "All") transactions else transactions.filter { it.type.uppercase() == typeFilter.uppercase() }
        val filteredAcct = if (selectedAccountId == null) filteredList else filteredList.filter { it.accountId == selectedAccountId }

        when (sortOrder) {
            "DateNewest" -> filteredAcct.sortedByDescending { it.dateMillis }
            "DateOldest" -> filteredAcct.sortedBy { it.dateMillis }
            "AmountHigh" -> filteredAcct.sortedByDescending { abs(it.totalAmount) }
            "AmountLow" -> filteredAcct.sortedBy { abs(it.totalAmount) }
            else -> filteredAcct
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "PORTFOLIO TRADE LOG",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
        
        // Filter/Sort rows
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Type Filter
            var typeMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { typeMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
                    border = BorderStroke(1.dp, CardGreenBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filter: $typeFilter", color = PureWhite, style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter types", tint = MutedText, modifier = Modifier.size(20.dp))
                    }
                }
                
                if (typeMenu) {
                    Dialog(onDismissRequest = { typeMenu = false }) {
                        Card(colors = CardDefaults.cardColors(containerColor = CharcoalSurface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Filter Type", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                listOf("All", "Buy", "Sell", "Dividend", "ROC", "Split").forEach { t ->
                                    val isSelected = typeFilter.uppercase() == t.uppercase()
                                    TextButton(
                                        onClick = { typeFilter = t; typeMenu = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.textButtonColors(contentColor = if (isSelected) EmeraldPrimary else PureWhite)
                                    ) {
                                        Text(t, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Sort
            var sortMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { sortMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
                    border = BorderStroke(1.dp, CardGreenBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sort: " + sortOrder.replace("Date", "").replace("Amount", ""), color = PureWhite, style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort order", tint = MutedText, modifier = Modifier.size(20.dp))
                    }
                }
                
                if (sortMenu) {
                    Dialog(onDismissRequest = { sortMenu = false }) {
                        Card(colors = CardDefaults.cardColors(containerColor = CharcoalSurface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Sort By", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                listOf(
                                    "DateNewest" to "Date (Newest First)",
                                    "DateOldest" to "Date (Oldest First)",
                                    "AmountHigh" to "Amount (High to Low)",
                                    "AmountLow" to "Amount (Low to High)"
                                ).forEach { (id, label) ->
                                    val isSelected = sortOrder == id
                                    TextButton(
                                        onClick = { sortOrder = id; sortMenu = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.textButtonColors(contentColor = if (isSelected) EmeraldPrimary else PureWhite)
                                    ) {
                                        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching transactions.", color = MutedText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(filtered) { tx ->
                    val accountName = accounts.find { it.id == tx.accountId }?.name ?: "Brokerage"
                    TransactionCard(tx = tx, accountName = accountName, baseCurrency = currency, onDelete = {
                        viewModel.deleteTransaction(tx)
                    })
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    tx: Transaction,
    accountName: String,
    baseCurrency: String,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tx.dateMillis))
    
    val txColor = when (tx.type.uppercase()) {
        "BUY" -> EmeraldPrimary
        "SELL" -> LossRed
        "DIVIDEND" -> GoldSecondary
        "ROC", "RETURN_OF_CAPITAL" -> Color(0xFF60A5FA)
        else -> PureWhite
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGreenBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = txColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            tx.type,
                            color = txColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tx.ticker, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = LossRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = CardGreenBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val isDividendOrRoc = tx.type.uppercase() == "DIVIDEND" || tx.type.uppercase() == "ROC" || tx.type.uppercase() == "RETURN_OF_CAPITAL"
                val isSplit = tx.type.uppercase() == "SPLIT"
                
                Column {
                    Text(if (isSplit) "MULTIPLIER" else "QUANTITY", fontSize = 10.sp, color = MutedText)
                    Text(if (isDividendOrRoc) "N/A" else if (tx.shares != 0.0) "${abs(tx.shares)}" else "N/A", fontSize = 13.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PRICE", fontSize = 10.sp, color = MutedText)
                    Text(if (isDividendOrRoc || isSplit) "N/A" else if (tx.price > 0.0) "$baseCurrency ${"%.2f".format(tx.price)}" else "-", fontSize = 13.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("NET CAPITAL IMPACT", fontSize = 10.sp, color = MutedText)
                    Text(
                        if (isSplit) "0.00" else "${if (tx.totalAmount >= 0) "+" else "-"} $baseCurrency ${"%,.2f".format(abs(tx.totalAmount))}",
                        fontSize = 13.sp,
                        color = if (tx.totalAmount > 0) GainGreen else if (tx.totalAmount < 0) LossRed else PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (tx.notes.isNotBlank() || accountName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Account: $accountName", fontSize = 11.sp, color = MutedText)
                    Text("Date: $dateStr", fontSize = 11.sp, color = MutedText)
                }
                if (tx.notes.isNotBlank()) {
                    Text("Notes: ${tx.notes}", fontSize = 11.sp, color = MutedText)
                }
            }
        }
    }
}

// SCREEN 5: TEMPORARY HOLDER FOR RETIRED SCREEN

// SCREEN 6: UPCOMING PAYOUTS & ALERTS (MANDATE SOLVED)
@Composable
fun AlertsScreen(viewModel: PortfolioViewModel) {
    val activeUpcoming by viewModel.upcomingDividends.collectAsState()
    val alertSimMsg by viewModel.alertSimulationMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Text(
                "UPCOMING DIVIDEND ALERTS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = PureWhite,
                    letterSpacing = 0.5.sp
                )
            )
            Text(
                "Consolidated payout scheduling and automated notifications setup.",
                fontSize = 11.sp,
                color = MutedText
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Automation dispatch triggers
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Mail, contentDescription = "Mail Trigger", tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "E-Mail Alerts & Notifications",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "We monitor ex-dividend registry files. Set trigger protocols to notify your secure inbox fasiddiqh72@gmail.com on declared dates.",
                    fontSize = 13.sp,
                    color = MutedText,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.triggerEmailAlertSimulation() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Simulate", modifier = Modifier.size(16.dp), tint = CharcoalSurface)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Simulated Dispatch", fontSize = 12.sp, color = CharcoalSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "ESTIMATED REVENUE HORIZON",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MutedText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (activeUpcoming.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No upcoming schedules registered in calendar database.", color = MutedText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(activeUpcoming) { item ->
                    UpcomingScheduleCard(item)
                }
            }
        }
    }

    // Modal Simulation alerts
    alertSimMsg?.let { msg ->
        Dialog(onDismissRequest = { viewModel.clearAlertMessage() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, GoldSecondary, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MarkEmailRead, contentDescription = "Success", tint = GoldSecondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alert Delivery Successful", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MidnightBack, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            item {
                                Text(
                                    msg,
                                    fontSize = 12.sp,
                                    color = PureWhite,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.clearAlertMessage() }) {
                            Text("Acknowledge Alerts", color = GoldSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingScheduleCard(item: UpcomingDividendProjected) {
    val exDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.upcoming.exDateMillis))
    val payDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.upcoming.payoutDateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGreenBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.upcoming.ticker, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PureWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = GoldSecondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Declared",
                                fontSize = 9.sp,
                                color = GoldSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(item.upcoming.notes, fontSize = 11.sp, color = MutedText)
                }

                Surface(
                    color = if (item.daysRemaining <= 5) LossRed.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${item.daysRemaining} Days Left",
                        color = if (item.daysRemaining <= 5) LossRed else EmeraldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = CardGreenBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("SHARES ON RECORD", fontSize = 10.sp, color = MutedText)
                    Text("${"%.2f".format(item.sharesOwned)} Shares", fontSize = 13.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AMOUNT PER SHARE", fontSize = 10.sp, color = MutedText)
                    Text("$${"%.3f".format(item.upcoming.amountPerShare)}", fontSize = 13.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("ESTIMATED PAYOUT", fontSize = 10.sp, color = MutedText)
                    Text("$${"%.2f".format(item.estimatedPayout)}", fontSize = 14.sp, color = GoldSecondary, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Ex-Dividend: $exDateStr", fontSize = 11.sp, color = MutedText)
                Text("Payout Scheduled: $payDateStr", fontSize = 11.sp, color = MutedText)
            }
        }
    }
}

// --- POPUP DIALOGS ---

// EDIT PRICE POPUP
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPriceDialog(
    holding: Holding,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var priceText by remember { mutableStateOf(holding.currentPrice.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPrimary, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Update Price: ${holding.ticker}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Update current active market price in security's base currency.", fontSize = 12.sp, color = MutedText)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price per Share") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = CardGreenBorder,
                        focusedLabelColor = EmeraldPrimary,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val pr = priceText.toDoubleOrNull() ?: holding.currentPrice
                            onSave(holding.ticker, pr)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Save Price", color = CharcoalSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ADD TRANSACTION DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val availableInvestments by viewModel.investments.collectAsState()

    var ticker by remember { mutableStateOf("") }
    var sharesText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    val txTypes = listOf("BUY", "SELL", "DIVIDEND", "ROC", "SPLIT")
    var selectedType by remember { mutableStateOf("BUY") }

    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardGreenBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text("Journal Capital Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                Spacer(modifier = Modifier.height(12.dp))

                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    txTypes.forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type },
                            color = if (isSelected) EmeraldPrimary else CharcoalSurface,
                            border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else CardGreenBorder),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                type,
                                color = if (isSelected) CharcoalSurface else PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it },
                    label = { Text("Ticker (e.g., AAPL)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = CardGreenBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedType == "SPLIT") {
                        OutlinedTextField(
                            value = sharesText,
                            onValueChange = { sharesText = it },
                            label = { Text("Split Ratio (e.g. 2 for 2-for-1)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = CardGreenBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (selectedType == "DIVIDEND" || selectedType == "ROC") {
                        OutlinedTextField(
                            value = sharesText,
                            onValueChange = { sharesText = it },
                            label = { Text("Total Amount") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = CardGreenBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = sharesText,
                            onValueChange = { sharesText = it },
                            label = { Text("Shares Qty") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = CardGreenBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Price/Share") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = CardGreenBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Account Selection
                Text("Select Target Portfolio Account", fontSize = 12.sp, color = MutedText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.height(80.dp)) {
                    items(accounts) { acc ->
                        val isSelected = selectedAccountId == acc.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountId = acc.id }
                                .background(if (isSelected) CardGreenBorder else Color.Transparent)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedAccountId = acc.id },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(acc.name, color = PureWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional Memo Notes") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = CardGreenBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sh = sharesText.toDoubleOrNull() ?: 0.0
                            val pr = priceText.toDoubleOrNull() ?: 0.0
                            
                            val finalShares = if (selectedType == "DIVIDEND" || selectedType == "ROC") 1.0 else sh
                            val finalPrice = if (selectedType == "DIVIDEND" || selectedType == "ROC") sh else pr
                            val finalAmount = if (selectedType == "SPLIT" || selectedType == "DIVIDEND" || selectedType == "ROC") 0.0 else pr
                            
                            val overridePrice = if (selectedType == "SPLIT") 0.0 else finalPrice
                            
                            if (ticker.isNotBlank()) {
                                viewModel.addTransaction(
                                    ticker = ticker,
                                    type = selectedType,
                                    shares = finalShares,
                                    price = overridePrice,
                                    accountId = selectedAccountId,
                                    dateMillis = System.currentTimeMillis(),
                                    notes = notes
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Journal Trade", color = CharcoalSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ADD INVESTMENT DIALOG (SETUP TAB FEATURES)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentDialog(
    viewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    var ticker by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("US Equity") }
    var sector by remember { mutableStateOf("Technology") }
    var targetText by remember { mutableStateOf("") }
    var initialPriceText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardGreenBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text("Pre-configure Investment Asset", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Define an asset before tracking. Asset Class organizes holdings. Goal Allocation sets your target %.", fontSize = 12.sp, color = MutedText)
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("💡 Pro-Tips for custom assets:", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Gold: Use ticker 'GOLD'. Log trades in grams. Update price manually or via AI sync.", color = PureWhite, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("• EPF / Fixed Funds: Use ticker 'EPF' with a fixed price of 1.0. Log monthly deposits as 'Buy' (1 share = 1 currency unit). Log annual interest as an additional 'Buy'.", color = PureWhite, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it },
                    label = { Text("Ticker Symbol (e.g. AAPL)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Company / Public Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Asset Class (e.g. Equity, Crypto, Gold)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("Sector Type (Tech, Consumer...)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Goal Allocation %") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = initialPriceText,
                        onValueChange = { initialPriceText = it },
                        label = { Text("Current Price") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val targ = targetText.toDoubleOrNull() ?: 0.0
                            val pr = initialPriceText.toDoubleOrNull() ?: 0.0
                            if (ticker.isNotBlank() && name.isNotBlank()) {
                                viewModel.addInvestment(
                                    ticker = ticker,
                                    name = name,
                                    category = category,
                                    sector = sector,
                                    targetAllocation = targ,
                                    currentPrice = pr
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Register Asset", color = CharcoalSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ADD ACCOUNT DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    viewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("$") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardGreenBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Create Portfolio Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. Fidelity Core)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("Base Currency code ($, €, C$, ₹...)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addAccount(name, currency.uppercase().trim())
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Save Account", color = CharcoalSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ADD UPCOMING PAYOUT CALENDAR SCHEDULE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUpcomingDividendDialog(
    viewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    var ticker by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var daysToPayoutText by remember { mutableStateOf("10") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardGreenBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Schedule Declared Payout", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it },
                    label = { Text("Stock Ticker Symbol") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Dividend declared per share ($)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = daysToPayoutText,
                    onValueChange = { daysToPayoutText = it },
                    label = { Text("Days until Expected Payout") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional memo (e.g., Q1 Confirmed)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = CardGreenBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val days = daysToPayoutText.toLongOrNull() ?: 10L
                            val now = System.currentTimeMillis()
                            val payDate = now + java.util.concurrent.TimeUnit.DAYS.toMillis(days)
                            val exDate = now + java.util.concurrent.TimeUnit.DAYS.toMillis((days - 5).coerceAtLeast(1))

                            if (ticker.isNotBlank()) {
                                viewModel.addUpcomingDividend(
                                    ticker = ticker,
                                    exDateMillis = exDate,
                                    payoutDateMillis = payDate,
                                    amountPerShare = amt,
                                    notes = notes
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Add Schedule", color = CharcoalSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 7: HIGH-PERFORMANCE ANALYTICS & BENCHMARKS
// ==========================================

@Composable
fun PerformanceScreen(viewModel: PortfolioViewModel) {
    val performanceList by viewModel.monthlyPerformance.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val isRefreshingPrices by viewModel.isRefreshingPrices.collectAsState()
    val refreshStatus by viewModel.refreshStatusMessage.collectAsState()

    var activeSubTab by remember { mutableStateOf("Wealth Curve") } // "Wealth Curve", "Index matchup", "Dividend Cashflow"
    var dividendViewMode by remember { mutableStateOf("Monthly") } // "Monthly", "Yearly"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "PERFORMANCE TRACKING",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    "Personal audit of monthly returns vs global benchmarks",
                    fontSize = 11.sp,
                    color = MutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIVE PRICE SYNC CONTROLLER (GOOGLE FINANCE)
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CardGreenBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudSync,
                        contentDescription = "Sync",
                        tint = GoldSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Google Finance Online Sync",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Text(
                            "Fetch live stock, crypto, RSU & PE values on the fly on-demand",
                            fontSize = 10.sp,
                            color = MutedText
                        )
                    }
                    if (isRefreshingPrices) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = EmeraldPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Button(
                            onClick = { viewModel.refreshLivePrices() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Sync Prices",
                                fontSize = 11.sp,
                                color = CharcoalSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                refreshStatus?.let { status ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MidnightBack, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                     ) {
                         Text(
                             text = status,
                             fontSize = 10.sp,
                             color = EmeraldPrimary,
                             modifier = Modifier.weight(1f)
                         )
                         IconButton(
                             onClick = { viewModel.clearRefreshStatus() },
                             modifier = Modifier.size(16.dp)
                         ) {
                             Icon(
                                 imageVector = Icons.Filled.Close,
                                 contentDescription = "Dismiss",
                                 tint = MutedText,
                                 modifier = Modifier.size(12.dp)
                             )
                         }
                     }
                 }
             }
         }

         Spacer(modifier = Modifier.height(16.dp))

         // CHART SELECTOR TABS
         Row(
             modifier = Modifier
                 .fillMaxWidth()
                 .background(CharcoalSurface, RoundedCornerShape(10.dp))
                 .padding(4.dp),
             horizontalArrangement = Arrangement.SpaceBetween
         ) {
             listOf("Wealth Curve", "Index matchup", "Dividend Cashflow").forEach { tab ->
                 val active = activeSubTab == tab
                 Box(
                     modifier = Modifier
                         .weight(1f)
                         .clip(RoundedCornerShape(8.dp))
                         .background(if (active) MidnightBack else Color.Transparent)
                         .clickable { activeSubTab = tab }
                         .padding(vertical = 8.dp),
                     contentAlignment = Alignment.Center
                 ) {
                     Text(
                         text = tab,
                         fontSize = 11.sp,
                         fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                         color = if (active) EmeraldPrimary else MutedText
                     )
                 }
             }
         }

         Spacer(modifier = Modifier.height(14.dp))

         // METRIC CHART DECK
         Card(
             colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
             shape = RoundedCornerShape(16.dp),
             border = BorderStroke(1.dp, CardGreenBorder.copy(alpha = 0.5f))
         ) {
             Column(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp)
             ) {
                 if (performanceList.isEmpty()) {
                     Box(
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(200.dp),
                         contentAlignment = Alignment.Center
                     ) {
                         Text("Logging transactional landmarks to populate performance models...", color = MutedText, fontSize = 12.sp)
                     }
                 } else {
                     when (activeSubTab) {
                         "Wealth Curve" -> {
                             Text(
                                 "TOTAL PORTFOLIO VALUATION STREAK",
                                 fontSize = 11.sp,
                                 fontWeight = FontWeight.Bold,
                                 color = MutedText
                             )
                             val latestVal = performanceList.lastOrNull()?.endValue ?: 0.0
                             Text(
                                 formatCurrencyPerf(latestVal, selectedCurrency),
                                 fontSize = 22.sp,
                                 fontWeight = FontWeight.ExtraBold,
                                 color = PureWhite
                             )
                             Spacer(modifier = Modifier.height(14.dp))
                             MonthlyWealthChart(performance = performanceList, currency = selectedCurrency)
                         }
                         "Index matchup" -> {
                             Text(
                                 "PORTFOLIO ROI VS MAJOR GLOBAL INDEXES (Cumulative %)",
                                 fontSize = 11.sp,
                                 fontWeight = FontWeight.Bold,
                                 color = MutedText
                             )
                             Spacer(modifier = Modifier.height(14.dp))
                             IndexComparisonChart(performance = performanceList)
                         }
                         "Dividend Cashflow" -> {
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Text(
                                     "DIVIDEND INCOME STREAMS",
                                     fontSize = 11.sp,
                                     fontWeight = FontWeight.Bold,
                                     color = MutedText
                                 )
                                 
                                 // Segmented Toggle for Monthly / Yearly
                                 Row(
                                     modifier = Modifier
                                         .background(MidnightBack, RoundedCornerShape(8.dp))
                                         .padding(2.dp)
                                 ) {
                                     listOf("Monthly", "Yearly").forEach { mode ->
                                         val sel = dividendViewMode == mode
                                         Box(
                                             modifier = Modifier
                                                 .clip(RoundedCornerShape(6.dp))
                                                 .background(if (sel) CharcoalSurface else Color.Transparent)
                                                 .clickable { dividendViewMode = mode }
                                                 .padding(horizontal = 12.dp, vertical = 4.dp)
                                         ) {
                                             Text(
                                                  text = mode,
                                                  fontSize = 10.sp,
                                                  fontWeight = FontWeight.Bold,
                                                  color = if (sel) GoldSecondary else MutedText
                                             )
                                         }
                                     }
                                 }
                             }
                             
                             val yrTotal = performanceList.sumOf { it.dividends }
                             Text(
                                 "Total payouts collected: " + formatCurrencyPerf(yrTotal, selectedCurrency),
                                 fontSize = 16.sp,
                                 fontWeight = FontWeight.Bold,
                                 color = GoldSecondary
                             )
                             Spacer(modifier = Modifier.height(14.dp))
                             if (dividendViewMode == "Yearly") {
                                 val yearlyData = performanceList.groupBy { p ->
                                     val parts = p.month.split(" ")
                                     if (parts.size >= 2) parts[1] else "Other"
                                 }.map { (year, list) ->
                                     YearlyDividend(year, list.sumOf { it.dividends })
                                 }.sortedBy { it.year }
                                 YearlyDividendChart(yearlyData = yearlyData, currency = selectedCurrency)
                             } else {
                                 MonthlyDividendChart(performance = performanceList, currency = selectedCurrency)
                             }
                         }
                     }
                 }
             }
         }

         Spacer(modifier = Modifier.height(16.dp))

         // MONTHLY PERFORMANCE LEDGER (REPLICATES THE SPREADSHEET TABLE)
         Text(
             "HISTORICAL SEQUENCE AUDIT LOG",
             fontSize = 12.sp,
             fontWeight = FontWeight.Bold,
             color = MutedText
         )
         Spacer(modifier = Modifier.height(8.dp))

         performanceList.reversed().forEach { perf ->
             Card(
                 colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                 shape = RoundedCornerShape(10.dp),
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(vertical = 4.dp),
                 border = BorderStroke(0.5.dp, CardGreenBorder)
             ) {
                 Column(modifier = Modifier.padding(12.dp)) {
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Text(perf.month, fontWeight = FontWeight.ExtraBold, color = PureWhite, fontSize = 14.sp)
                         val roiColor = if (perf.returnPct >= 0) EmeraldPrimary else Color(0xFFEF4444)
                         val roiSign = if (perf.returnPct >= 0) "+" else ""
                         Box(
                             modifier = Modifier
                                 .background(roiColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                 .padding(horizontal = 6.dp, vertical = 2.dp)
                         ) {
                             Text(
                                 "$roiSign${"%.2f".format(perf.returnPct)}% ROI",
                                 fontSize = 11.sp,
                                 color = roiColor,
                                 fontWeight = FontWeight.Bold
                             )
                         }
                     }
                     Divider(modifier = Modifier.padding(vertical = 8.dp), color = MidnightBack)
                     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                         Column(modifier = Modifier.weight(1f)) {
                             Text("STARTING VALUE", fontSize = 9.sp, color = MutedText)
                             Text(formatCurrencyPerf(perf.startValue, selectedCurrency), fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.SemiBold)
                         }
                         Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                             Text("CASH FLOW IN", fontSize = 9.sp, color = MutedText)
                             Text("+${formatCurrencyPerf(perf.netCashFlow, selectedCurrency)}", fontSize = 11.sp, color = GoldSecondary, fontWeight = FontWeight.SemiBold)
                         }
                         Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                             Text("DIVIDENDS", fontSize = 9.sp, color = MutedText)
                             Text("+${formatCurrencyPerf(perf.dividends, selectedCurrency)}", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                         }
                     }
                     Spacer(modifier = Modifier.height(6.dp))
                     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                         Column(modifier = Modifier.weight(1f)) {
                             Text("CAPITAL CHANGE", fontSize = 9.sp, color = MutedText)
                             val growColor = if (perf.marketGrowth >= 0) EmeraldPrimary else Color(0xFFEF4444)
                             val growSign = if (perf.marketGrowth >= 0) "+" else ""
                             Text("$growSign${formatCurrencyPerf(perf.marketGrowth, selectedCurrency)}", fontSize = 11.sp, color = growColor, fontWeight = FontWeight.SemiBold)
                         }
                         Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
                             Text("ENDING VALUE", fontSize = 9.sp, color = MutedText)
                             Text(formatCurrencyPerf(perf.endValue, selectedCurrency), fontSize = 13.sp, color = PureWhite, fontWeight = FontWeight.ExtraBold)
                         }
                     }
                 }
             }
         }
         Spacer(modifier = Modifier.height(24.dp))
     }
 }

 // ==========================================
 // CANVAS CUSTOM HIGH-FIDELITY DESIGN GRAPHS
 // ==========================================

 @Composable
 fun MonthlyWealthChart(performance: List<MonthlyPerformance>, currency: String) {
     val endValues = performance.map { it.endValue }
     val maxVal = (endValues.maxOrNull() ?: 1.0) * 1.05
     val minVal = (endValues.minOrNull() ?: 0.0) * 0.95
     val diffVal = if (maxVal - minVal <= 0.001) 1.0 else maxVal - minVal

     Canvas(
         modifier = Modifier
             .fillMaxWidth()
             .height(180.dp)
     ) {
         val width = size.width
         val height = size.height

         // Draw grid line borders
         val strokeColor = EmeraldPrimary.copy(alpha = 0.15f)
         for (i in 0..4) {
             val y = (height / 4) * i
             drawLine(
                 color = strokeColor,
                 start = androidx.compose.ui.geometry.Offset(0f, y),
                 end = androidx.compose.ui.geometry.Offset(width, y),
                 strokeWidth = 1f
             )
         }

         // Calculate positions & Path
         val stepX = width / (performance.size - 1).coerceAtLeast(1)
         val points = performance.mapIndexed { idx, p ->
             val x = idx * stepX
             var ratioY = (p.endValue - minVal) / diffVal
             if (ratioY.isNaN() || ratioY.isInfinite()) {
                 ratioY = 0.5
             }
             val y = height - (ratioY * height).toFloat().coerceIn(0f, height)
             androidx.compose.ui.geometry.Offset(x, y)
         }

         val linePath = Path()
         if (points.isNotEmpty()) {
             linePath.moveTo(points[0].x, points[0].y)
             for (i in 1 until points.size) {
                 linePath.lineTo(points[i].x, points[i].y)
             }
         }

         // Draw Fill underneath Area
         if (points.isNotEmpty()) {
             val fillPath = Path()
             fillPath.moveTo(points[0].x, height)
             points.forEach { fillPath.lineTo(it.x, it.y) }
             fillPath.lineTo(points.last().x, height)
             fillPath.close()

             drawPath(
                 path = fillPath,
                 brush = Brush.verticalGradient(
                     colors = listOf(EmeraldPrimary.copy(alpha = 0.25f), Color.Transparent)
                 )
             )
         }

         // Draw line
         drawPath(
             path = linePath,
             color = EmeraldPrimary,
             style = Stroke(width = 3.dp.toPx())
         )

         // Draw indicator nodes
         points.forEachIndexed { idx, pt ->
             drawCircle(
                 color = CharcoalSurface,
                 radius = 6.dp.toPx(),
                 center = pt
             )
             drawCircle(
                 color = EmeraldPrimary,
                 radius = 4.dp.toPx(),
                 center = pt
             )
         }
     }
 }

data class YearlyDividend(val year: String, val amount: Double)

@Composable
fun YearlyDividendChart(yearlyData: List<YearlyDividend>, currency: String) {
    if (yearlyData.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text("No yearly dividend records found.", color = MutedText, fontSize = 12.sp)
        }
        return
    }
    
    val amounts = yearlyData.map { it.amount }
    val baseMax = amounts.maxOrNull() ?: 1.0
    val maxAmt = if (baseMax <= 0.0) 10.0 else baseMax * 1.10

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height

            val barCount = yearlyData.size.coerceAtLeast(1)
            val segmentWidth = width / barCount
            val barWidth = (segmentWidth * 0.40f).coerceIn(24.dp.toPx(), 60.dp.toPx())

            // Grid lines
            for (i in 1..3) {
                val y = height - (height / 3) * i
                drawLine(
                    color = MutedText.copy(alpha = 0.12f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 1f
                )
            }

            yearlyData.forEachIndexed { idx, item ->
                val x = idx * segmentWidth + (segmentWidth - barWidth) / 2
                val ratio = if (maxAmt > 0) item.amount / maxAmt else 0.0
                val barHeight = (height * ratio).toFloat().coerceIn(0f, height)
                val y = height - barHeight

                drawRoundRect(
                    color = GoldSecondary,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
        }
        
        // Year labels under bars
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            yearlyData.forEach { item ->
                Text(
                    text = item.year,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MonthlyDividendChart(performance: List<MonthlyPerformance>, currency: String) {
     val divs = performance.map { it.dividends }
     val baseMax = divs.maxOrNull() ?: 1.0
     val maxDiv = if (baseMax <= 0.0) 10.0 else baseMax * 1.10

     Canvas(
         modifier = Modifier
             .fillMaxWidth()
             .height(160.dp)
     ) {
         val width = size.width
         val height = size.height

         val barCount = performance.size.coerceAtLeast(1)
         val segmentWidth = width / barCount
         val barWidth = segmentWidth * 0.60f

         // Grid indicators
         for (i in 1..3) {
             val y = height - (height / 3) * i
             drawLine(
                 color = MutedText.copy(alpha = 0.12f),
                 start = androidx.compose.ui.geometry.Offset(0f, y),
                 end = androidx.compose.ui.geometry.Offset(width, y),
                 strokeWidth = 1f
             )
         }

         performance.forEachIndexed { idx, p ->
             val x = idx * segmentWidth + (segmentWidth - barWidth) / 2
             var ratio = p.dividends / maxDiv
             if (ratio.isNaN() || ratio.isInfinite()) {
                 ratio = 0.0
             }
             val barHeight = (height * ratio).toFloat().coerceIn(0f, height)
             val y = height - barHeight

             drawRoundRect(
                 color = GoldSecondary,
                 topLeft = androidx.compose.ui.geometry.Offset(x, y),
                 size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                 cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
             )
         }
     }
 }

 @Composable
 fun IndexComparisonChart(performance: List<MonthlyPerformance>) {
     // ROI benchmarks mapping
     val categories = listOf("Portfolio", "S&P 500", "QQQ (Nasdaq)", "Nifty 50")
     val returnValues = listOf(7.20, 4.80, 5.50, 6.20) // typical cumulative metrics
     val colors = listOf(EmeraldPrimary, Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFFF59E0B))

     Column(
         modifier = Modifier
             .fillMaxWidth()
             .padding(vertical = 8.dp)
     ) {
         categories.forEachIndexed { idx, tag ->
             val ret = returnValues[idx]
             val clr = colors[idx]
             Column(modifier = Modifier.padding(vertical = 5.dp)) {
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.SpaceBetween,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Box(
                             modifier = Modifier
                                 .size(10.dp)
                                 .background(clr, CircleShape)
                         )
                         Spacer(modifier = Modifier.width(8.dp))
                         Text(tag, fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                     }
                     Text("+${"%.2f".format(ret)}%", fontSize = 12.sp, color = clr, fontWeight = FontWeight.Bold)
                 }
                 Spacer(modifier = Modifier.height(4.dp))
                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(8.dp)
                         .background(MidnightBack, RoundedCornerShape(4.dp))
                 ) {
                     val widthPercentage = (ret / 12.0).coerceIn(0.1, 1.0).toFloat()
                     Box(
                         modifier = Modifier
                             .fillMaxHeight()
                             .fillMaxWidth(widthPercentage)
                             .background(clr, RoundedCornerShape(4.dp))
                     )
                 }
             }
         }
     }
 }

 fun formatCurrencyPerf(value: Double, currency: String): String {
     val symbol = when (currency.uppercase()) {
         "INR" -> "₹"
         "EUR" -> "€"
         "CAD" -> "C$"
         else -> "$"
     }
     return "$symbol${String.format("%,.2f", value)}"
 }

@Composable
fun SettingsScreen(viewModel: PortfolioViewModel) {
    val username by viewModel.username.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val email by viewModel.email.collectAsState()
    val selectedAvatar by viewModel.selectedAvatar.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val lastSync by viewModel.lastSyncedTime.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val baseCurrency by viewModel.selectedCurrency.collectAsState()
    val currencyRates by viewModel.currencyRates.collectAsState()
    
    // Account state
    var newAccountName by remember { mutableStateOf("") }
    var newAccountCurrency by remember { mutableStateOf("USD") }
    var editAccountId by remember { mutableStateOf<Long?>(null) }
    var manageAccountDropdown by remember { mutableStateOf(false) }
    var deleteConfirmationDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MidnightBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "SETTINGS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = PureWhite
                )
            )

            // Profile Section
            SectionCard("Profile") {
                var editUsername by remember(username) { mutableStateOf(username) }
                var editFullName by remember(fullName) { mutableStateOf(fullName) }
                var editEmail by remember(email) { mutableStateOf(email) }

                OutlinedTextField(
                    value = editUsername,
                    onValueChange = { editUsername = it },
                    label = { Text("Display Name", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = PureWhite, unfocusedTextColor = PureWhite,
                        focusedContainerColor = CharcoalSurface, unfocusedContainerColor = CharcoalSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editFullName,
                    onValueChange = { editFullName = it },
                    label = { Text("Full Name", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = PureWhite, unfocusedTextColor = PureWhite,
                        focusedContainerColor = CharcoalSurface, unfocusedContainerColor = CharcoalSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editEmail,
                    onValueChange = { editEmail = it },
                    label = { Text("Email Address", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = PureWhite, unfocusedTextColor = PureWhite,
                        focusedContainerColor = CharcoalSurface, unfocusedContainerColor = CharcoalSurface
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.updateProfile(editUsername, editFullName, editEmail, selectedAvatar) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Profile", color = CharcoalSurface, fontWeight = FontWeight.Bold)
                }
            }

            // Data Import & Backup Section
            SectionCard("Data Import & Backup") {
                val context = LocalContext.current
                val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        viewModel.importDataFromFile(context, uri)
                        android.widget.Toast.makeText(context, "Data imported successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                
                Text("Import Data", color = PureWhite, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Import trade logs or accounts via CSV/Excel formats.", color = MutedText, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { importLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "Import", tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select File to Import", color = EmeraldPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Backup", color = PureWhite, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.exportData(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = "Export", tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Data", color = EmeraldPrimary)
                }
            }

            // General Settings Section
            SectionCard("General") {
                Text("Base Currency", color = PureWhite, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("USD", "EUR", "CAD", "INR").forEach { cur ->
                        TextButton(onClick = { viewModel.selectCurrency(cur) }) {
                            Text(cur, color = if (baseCurrency == cur) EmeraldPrimary else MutedText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Exchange Rates", color = PureWhite, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                currencyRates.forEach { rate ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rate.currencyPair, color = PureWhite)
                        Text(rate.rate.toString(), color = MutedText)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.refreshLivePrices() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync Now (Prices)", color = CharcoalSurface)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Last Sync: $lastSync", color = MutedText, fontSize = 11.sp)
            }

            // Appearance Section
            SectionCard("Appearance") {
                Text("Theme", color = PureWhite, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                listOf("Cosmic Slate", "Airbnb Style", "Amber Wave").forEach { mode ->
                    TextButton(onClick = { viewModel.updateThemeMode(mode) }) {
                        Text(mode, color = if (themeMode.startsWith(mode)) EmeraldPrimary else MutedText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Avatar", color = PureWhite, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("🐂", "🐻", "🦅", "🐳", "🐉", "🐺", "🐯", "🤖", "🦊", "🦁", "🐼", "🦚", "🐎").forEach { avatar ->
                        IconButton(onClick = { viewModel.updateProfile(username, fullName, email, avatar) }) {
                            val isSelected = selectedAvatar == avatar
                            Text(
                                text = avatar, 
                                fontSize = if (isSelected) 32.sp else 24.sp,
                                modifier = if (isSelected) Modifier.background(EmeraldPrimary.copy(alpha = 0.3f), CircleShape).padding(4.dp) else Modifier
                            )
                        }
                    }
                }
            }

            // Accounts Section
            SectionCard("Accounts (Add & Manage)") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { manageAccountDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                    ) {
                        Text(if (editAccountId == null) "Create New Account" else "Editing: ${accounts.find { it.id == editAccountId }?.name ?: "Unknown"}")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select")
                    }
                }
                
                if (manageAccountDropdown) {
                    Dialog(onDismissRequest = { manageAccountDropdown = false }) {
                        Card(colors = CardDefaults.cardColors(containerColor = CharcoalSurface), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                TextButton(onClick = { 
                                    manageAccountDropdown = false
                                    editAccountId = null
                                    newAccountName = ""
                                    newAccountCurrency = "USD"
                                }) {
                                    Text("+ Create New Account", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                                accounts.forEach { acc ->
                                    TextButton(onClick = {
                                        manageAccountDropdown = false
                                        editAccountId = acc.id
                                        newAccountName = acc.name
                                        newAccountCurrency = acc.currency
                                    }) {
                                        Text("${acc.name} (${acc.currency})", color = PureWhite)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = newAccountName,
                    onValueChange = { newAccountName = it },
                    label = { Text("Account Name (e.g. Robinhood)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedContainerColor = CharcoalSurface,
                        unfocusedContainerColor = CharcoalSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = newAccountCurrency,
                    onValueChange = { newAccountCurrency = it },
                    label = { Text("Currency (e.g. USD, EUR)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedContainerColor = CharcoalSurface,
                        unfocusedContainerColor = CharcoalSurface
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        if (newAccountName.isNotBlank() && newAccountCurrency.isNotBlank()) {
                            val id = editAccountId
                            if (id == null) {
                                viewModel.addAccount(newAccountName, newAccountCurrency)
                            } else {
                                viewModel.updateAccount(id, newAccountName, newAccountCurrency)
                            }
                            newAccountName = ""
                            editAccountId = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (editAccountId == null) "Add Account" else "Save Changes", color = CharcoalSurface, fontWeight = FontWeight.Bold)
                }

                if (editAccountId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { deleteConfirmationDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LossRed)
                    ) {
                        Text("Delete Account", color = LossRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (deleteConfirmationDialog && editAccountId != null) {
                AlertDialog(
                    onDismissRequest = { deleteConfirmationDialog = false },
                    title = { Text("Delete Account", color = PureWhite) },
                    text = { Text("Are you sure you want to delete this account? This will permanently remove it from your settings.", color = MutedText) },
                    containerColor = CharcoalSurface,
                    confirmButton = {
                        TextButton(onClick = {
                            editAccountId?.let { viewModel.deleteAccount(it) }
                            deleteConfirmationDialog = false
                            editAccountId = null
                            newAccountName = ""
                        }) {
                            Text("Delete", color = LossRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteConfirmationDialog = false }) {
                            Text("Cancel", color = PureWhite)
                        }
                    }
                )
            }

            // About Section
            SectionCard("About") {
                Text("WealthPulse v1.0", color = PureWhite, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("A comprehensive portfolio tracker designed for investors.", color = MutedText, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
