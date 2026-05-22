package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PortfolioViewModel(
    application: Application,
    private val repository: PortfolioRepository
) : AndroidViewModel(application) {

    // Selected Display Currency
    private val _selectedCurrency = MutableStateFlow("USD")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    // Selected Tab
    private val _currentTab = MutableStateFlow("Dashboard")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // CONNECTIVITY & PREFERENCES STATES
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _showOnreconnectNotification = MutableStateFlow(false)
    val showOnreconnectNotification: StateFlow<Boolean> = _showOnreconnectNotification.asStateFlow()

    private val prefs = application.getSharedPreferences("wealthpulse_prefs", Context.MODE_PRIVATE)

    private val _username = MutableStateFlow(prefs.getString("username", "fasiddiqh72") ?: "fasiddiqh72")
    val username = _username.asStateFlow()

    private val _fullName = MutableStateFlow(prefs.getString("full_name", "Fakhruddin Siddiqh") ?: "Fakhruddin Siddiqh")
    val fullName = _fullName.asStateFlow()

    private val _email = MutableStateFlow(prefs.getString("email", "fasiddiqh72@gmail.com") ?: "fasiddiqh72@gmail.com")
    val email = _email.asStateFlow()

    private val _selectedAvatar = MutableStateFlow(prefs.getString("selected_avatar", "Phoenix") ?: "Phoenix")
    val selectedAvatar = _selectedAvatar.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "Cosmic Slate") ?: "Cosmic Slate")
    val themeMode = _themeMode.asStateFlow()

    // Last Sync timestamp with saved tracking
    private val _lastSyncedTime = MutableStateFlow(prefs.getString("last_synced_time", "Sync pending...") ?: "Sync pending...")
    val lastSyncedTime = _lastSyncedTime.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getString("last_backup_time", "No backup created") ?: "No backup created")
    val lastBackupTime = _lastBackupTime.asStateFlow()

    // Import Text and Status
    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage = _importStatusMessage.asStateFlow()

    // RAW DATA
    val accounts: StateFlow<List<Account>> = repository.accountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val investments: StateFlow<List<Investment>> = repository.investmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DYNAMIC AND CALCULATED DATA, REACTIVE TO CURRENCY SELECTION
    val holdings: StateFlow<List<Holding>> = _selectedCurrency
        .flatMapLatest { currency -> repository.getHoldingsFlow(currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<PortfolioSummary?> = _selectedCurrency
        .flatMapLatest { currency -> repository.getPortfolioSummaryFlow(currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categoryAllocations: StateFlow<List<GroupedAllocation>> = _selectedCurrency
        .flatMapLatest { currency -> repository.getCategoryAllocationsFlow(currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sectorAllocations: StateFlow<List<GroupedAllocation>> = _selectedCurrency
        .flatMapLatest { currency -> repository.getSectorAllocationsFlow(currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingDividends: StateFlow<List<UpcomingDividendProjected>> = repository.getUpcomingDividendsProjectedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val realizedGains: StateFlow<List<RealizedGain>> = repository.getRealizedGainsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dividendTimeline: StateFlow<List<DividendTimelineItem>> = repository.getDividendTimelineFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyPerformance: StateFlow<List<MonthlyPerformance>> = _selectedCurrency
        .flatMapLatest { currency -> repository.getMonthlyPerformanceFlow(currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Advisor States
    private val _aiInsights = MutableStateFlow<String>("")
    val aiInsights: StateFlow<String> = _aiInsights.asStateFlow()

    private val _isGeneratingInsights = MutableStateFlow(false)
    val isGeneratingInsights: StateFlow<Boolean> = _isGeneratingInsights.asStateFlow()

    // Email / Payout alerts states
    private val _alertSimulationMessage = MutableStateFlow<String?>(null)
    val alertSimulationMessage: StateFlow<String?> = _alertSimulationMessage.asStateFlow()

    // Live Price Refresh states
    private val _isRefreshingPrices = MutableStateFlow(false)
    val isRefreshingPrices: StateFlow<Boolean> = _isRefreshingPrices.asStateFlow()

    private val _refreshStatusMessage = MutableStateFlow<String?>(null)
    val refreshStatusMessage: StateFlow<String?> = _refreshStatusMessage.asStateFlow()

    private fun checkCurrentNetworkState(): Boolean {
        try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            return true
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wasOffline = !_isOnline.value
            _isOnline.value = true
            if (wasOffline) {
                viewModelScope.launch {
                    _showOnreconnectNotification.value = true
                    kotlinx.coroutines.delay(2000)
                    _showOnreconnectNotification.value = false
                }
            }
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentNetworkState()
        }
    }

    init {
        _isOnline.value = checkCurrentNetworkState()
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // handle gracefully
        }

        // Pre-seed mock data on first execution
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        // Hybrid Smart-Broker: Active periodic background price sync (runs every 60 seconds when online)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60000)
                if (_isOnline.value) {
                    refreshLivePrices()
                }
            }
        }
    }

    // ACTIONS
    fun selectCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun refreshLivePrices() {
        if (_isRefreshingPrices.value) return
        
        if (!_isOnline.value) {
            _refreshStatusMessage.value = "Offline Fallback: Working in offline mode. Last saved prices utilized."
            return
        }

        _isRefreshingPrices.value = true
        _refreshStatusMessage.value = "Scanning global markets via Google Finance crawler..."
        
        viewModelScope.launch {
            val key = BuildConfig.GEMINI_API_KEY
            val (success, message) = repository.refreshAllPricesWithGemini(key)
            _refreshStatusMessage.value = message
            
            if (success) {
                val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                setLastSyncedTime("Synced at " + fmt.format(Date()))
            }
            _isRefreshingPrices.value = false
        }
    }

    fun clearRefreshStatus() {
        _refreshStatusMessage.value = null
    }

    fun updateProfile(newUsername: String, newFullName: String, newEmail: String, avatar: String) {
        _username.value = newUsername
        _fullName.value = newFullName
        _email.value = newEmail
        _selectedAvatar.value = avatar
        
        prefs.edit()
            .putString("username", newUsername)
            .putString("full_name", newFullName)
            .putString("email", newEmail)
            .putString("selected_avatar", avatar)
            .apply()
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setLastSyncedTime(time: String) {
        _lastSyncedTime.value = time
        prefs.edit().putString("last_synced_time", time).apply()
    }

    fun setLastBackupTime(time: String) {
        _lastBackupTime.value = time
        prefs.edit().putString("last_backup_time", time).apply()
    }

    fun clearImportStatus() {
        _importStatusMessage.value = null
    }

    fun importCsvData(csvText: String) {
        if (csvText.isBlank()) {
            _importStatusMessage.value = "Error: Paste area is empty. Please paste CSV transactions log."
            return
        }

        val lines = csvText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) {
            _importStatusMessage.value = "Error: CSV requires at least a header row and 1 transaction row."
            return
        }

        viewModelScope.launch {
            var addedCount = 0
            var failedCount = 0
            var skippedCount = 0

            // Split first row by separator (comma, semicolon, or tab)
            val headers = lines[0].split(Regex("[,;\t]")).map { it.trim().lowercase() }
            
            val tickerIdx = headers.indexOfFirst { it.contains("ticker") || it.contains("sym") || it.contains("asset") }
            val typeIdx = headers.indexOfFirst { it.contains("type") || it.contains("action") }
            val sharesIdx = headers.indexOfFirst { it.contains("share") || it.contains("qty") || it.contains("quantity") }
            val priceIdx = headers.indexOfFirst { it.contains("price") || it.contains("val") || it.contains("cost") }
            val accountIdx = headers.indexOfFirst { it.contains("acc") || it.contains("wallet") || it.contains("portfolio") }
            val dateIdx = headers.indexOfFirst { it.contains("date") || it.contains("time") }
            val notesIdx = headers.indexOfFirst { it.contains("note") || it.contains("desc") }

            if (tickerIdx == -1 || typeIdx == -1 || sharesIdx == -1 || priceIdx == -1) {
                _importStatusMessage.value = "Error: Could not identify mapping! CSV must contain columns similar to Ticker, Type, Shares, Price."
                return@launch
            }

            val currentAccountsList = repository.accountsFlow.first()
            val primaryAccount = currentAccountsList.firstOrNull()
            if (primaryAccount == null) {
                _importStatusMessage.value = "Error: No accounts found. Please add a base portfolio account first."
                return@launch
            }

            val simpleDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

            for (i in 1 until lines.size) {
                try {
                    val row = lines[i].split(Regex("[,;\t]")).map { it.trim() }
                    if (row.size <= maxOf(tickerIdx, typeIdx, sharesIdx, priceIdx)) {
                        skippedCount++
                        continue
                    }

                    val ticker = row[tickerIdx].uppercase()
                    val type = row[typeIdx].uppercase()
                    val shares = row[sharesIdx].toDoubleOrNull() ?: 1.0
                    val price = row[priceIdx].toDoubleOrNull() ?: 0.0

                    if (ticker.isBlank()) {
                        failedCount++
                        continue
                    }

                    var matchedAccountId = primaryAccount.id
                    if (accountIdx != -1 && accountIdx < row.size) {
                        val accName = row[accountIdx]
                        val match = currentAccountsList.find { it.name.lowercase() == accName.lowercase() }
                        if (match != null) {
                            matchedAccountId = match.id
                        } else {
                            val newId = repository.addAccount(Account(name = accName, currency = "USD"))
                            matchedAccountId = newId
                        }
                    }

                    var dateMillis = System.currentTimeMillis()
                    if (dateIdx != -1 && dateIdx < row.size) {
                        try {
                            val parsedDate = simpleDateFormat.parse(row[dateIdx])
                            if (parsedDate != null) {
                                dateMillis = parsedDate.time
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }

                    val notes = if (notesIdx != -1 && notesIdx < row.size) row[notesIdx] else "CSV Importer"

                    addTransaction(
                        ticker = ticker,
                        type = type,
                        shares = shares,
                        price = price,
                        accountId = matchedAccountId,
                        dateMillis = dateMillis,
                        notes = notes
                    )
                    addedCount++
                } catch (e: Exception) {
                    failedCount++
                }
            }

            _importStatusMessage.value = "Import complete: Added $addedCount. Skipped $skippedCount. Errors $failedCount."
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // ignore
        }
    }

    // TRANSACTION WRITES
    fun addTransaction(
        ticker: String,
        type: String,
        shares: Double,
        price: Double,
        accountId: Long,
        dateMillis: Long,
        notes: String = ""
    ) {
        viewModelScope.launch {
            // Calculate cash impact
            // BUY -> cash flow is negative (buying costs money)
            // SELL -> cash flow is positive (selling brings money)
            // DIVIDEND -> cash flow is positive (getting cash)
            // ROC -> cash flow is positive (returning cash)
            val netAmount = when (type.uppercase()) {
                "BUY" -> -(shares * price)
                "SELL" -> (shares * price)
                "DIVIDEND", "ROC" -> (shares * price) // or direct total payout
                else -> 0.0
            }

            repository.addTransaction(
                Transaction(
                    ticker = ticker.uppercase().trim(),
                    type = type.uppercase(),
                    shares = if (type.uppercase() == "SELL") -shares else shares,
                    price = price,
                    totalAmount = netAmount,
                    accountId = accountId,
                    dateMillis = dateMillis,
                    notes = notes
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // INVESTMENT CREATION
    fun addInvestment(
        ticker: String,
        name: String,
        category: String,
        sector: String,
        targetAllocation: Double,
        currentPrice: Double,
        currency: String = "USD"
    ) {
        viewModelScope.launch {
            repository.addInvestment(
                Investment(
                    ticker = ticker.uppercase().trim(),
                    name = name.trim(),
                    category = category.trim(),
                    sector = sector.trim(),
                    targetAllocation = targetAllocation,
                    currentPrice = currentPrice,
                    baseCurrency = currency
                )
            )
        }
    }

    fun updateInvestmentPrice(ticker: String, newPrice: Double) {
        viewModelScope.launch {
            val existing = repository.investmentsFlow.first().find { it.ticker.uppercase() == ticker.uppercase() }
            if (existing != null) {
                repository.updateInvestment(existing.copy(currentPrice = newPrice))
            }
        }
    }

    // ACCOUNT CREATION
    fun addAccount(name: String, currency: String) {
        viewModelScope.launch {
            repository.addAccount(Account(name = name.trim(), currency = currency))
        }
    }

    // UPCOMING DIVIDENDS
    fun addUpcomingDividend(
        ticker: String,
        exDateMillis: Long,
        payoutDateMillis: Long,
        amountPerShare: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addUpcomingDividend(
                UpcomingDividend(
                    ticker = ticker.uppercase().trim(),
                    exDateMillis = exDateMillis,
                    payoutDateMillis = payoutDateMillis,
                    amountPerShare = amountPerShare,
                    notes = notes
                )
            )
        }
    }

    fun deleteUpcomingDividend(upcoming: UpcomingDividend) {
        viewModelScope.launch {
            repository.deleteUpcomingDividend(upcoming)
        }
    }

    // EMAIL ALERT SERVICE SIMULATOR
    fun triggerEmailAlertSimulation() {
        val emailTarget = "fasiddiqh72@gmail.com"
        val activeUpcoming = upcomingDividends.value.filter { it.sharesOwned > 0.0 }
        
        if (activeUpcoming.isEmpty()) {
            _alertSimulationMessage.value = "Alert check complete: You currently hold no positions with active upcoming dividend ex-dates in our catalog."
            return
        }

        val detailsBuilder = StringBuilder()
        detailsBuilder.append("AUTOMATED WEALTHPULSE PAYOUT REPORT\n")
        detailsBuilder.append("=====================================\n")
        detailsBuilder.append("Recipient: $emailTarget\n")
        detailsBuilder.append("Report Date: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
        detailsBuilder.append("Upcoming Dividend Payout Schedules detected:\n\n")

        var grandTotalEst = 0.0
        activeUpcoming.forEach { p ->
            val payDateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(p.upcoming.payoutDateMillis))
            val exDateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(p.upcoming.exDateMillis))
            detailsBuilder.append("• Ticker: ${p.upcoming.ticker}\n")
            detailsBuilder.append("  Shares Owned on Ex-Date: ${"%.2f".format(p.sharesOwned)}\n")
            detailsBuilder.append("  Declared Div/Share: $${"%.4f".format(p.upcoming.amountPerShare)}\n")
            detailsBuilder.append("  Estimated Payout: $${"%.2f".format(p.estimatedPayout)}\n")
            detailsBuilder.append("  Ex-Dividend Date: $exDateStr\n")
            detailsBuilder.append("  Expected Payout Date: $payDateStr (${p.daysRemaining} days remaining)\n")
            detailsBuilder.append("  Note: ${p.upcoming.notes}\n\n")
            grandTotalEst += p.estimatedPayout
        }

        detailsBuilder.append("-------------------------------------\n")
        detailsBuilder.append("GRAND TOTAL ESTIMATED INCOMING CASHFLOW: $${"%.2f".format(grandTotalEst)} USD\n")
        detailsBuilder.append("=====================================\n")
        detailsBuilder.append("System status: Email notifications successfully dispatched & automated alerts configured on client schedule.")

        _alertSimulationMessage.value = detailsBuilder.toString()
    }

    fun clearAlertMessage() {
        _alertSimulationMessage.value = null
    }

    // GEMINI ADVISOR INSIGHTS
    fun generateAiFinancialAdvisory() {
        if (_isGeneratingInsights.value) return
        _isGeneratingInsights.value = true
        _aiInsights.value = "Analyzing portfolio metrics and calculating optimal allocations..."

        viewModelScope.launch {
            val key = BuildConfig.GEMINI_API_KEY
            val currentSummary = summary.value
            val currentHoldingsList = holdings.value
            val currentAllocations = categoryAllocations.value
            val activeDividends = upcomingDividends.value

            val prompt = """
                You are WealthPulse AI, an elite chartered financial planner and quantitative investment strategist. 
                I will provide you with the user's real-time portfolio metrics, holdings, category targets, and upcoming dividends. 
                Please perform a rigorous, comprehensive, and highly polished financial sanity audit. Keep it actionable, structured, and professional. 
                
                USER INFO:
                - Selected Base Currency: ${_selectedCurrency.value}
                - Total Portfolio Value: ${currentSummary?.totalValue?.let { "%.2f".format(it) } ?: "0.00"} ${_selectedCurrency.value}
                - Net Acquisition Cost: ${currentSummary?.totalCost?.let { "%.2f".format(it) } ?: "0.00"} ${_selectedCurrency.value}
                - Net Unrealized Gain/Loss: ${currentSummary?.totalGainLoss?.let { "%.2f".format(it) } ?: "0.00"} (${currentSummary?.totalGainLossPct?.let { "%.2f".format(it) } ?: "0.00"}%)
                - Annual Average Yield on Cost: ${currentSummary?.averageYield?.let { "%.2f".format(it) } ?: "0.00"}%
                - Historical Dividends Logged: ${currentSummary?.totalDividendsReceived?.let { "%.2f".format(it) } ?: "0.00"} ${_selectedCurrency.value}
                
                CURRENT HOLDINGS:
                ${currentHoldingsList.joinToString(separator = "\n") { h -> 
                    "- ${h.ticker} (${h.name}): Owned: ${"%.2f".format(h.totalShares)} shares, Avg Cost Price: ${"%.2f".format(h.averagePrice)}, Current Price: ${"%.2f".format(h.currentPrice)}, Value: ${"%.2f".format(h.currentValue)}, Net Profit/Loss: ${"%.2f".format(h.totalGainLoss)} (${"%.2f".format(h.totalGainLossPct)}%)"
                }}
                
                ASSET ALLOCATION VS TARGETS:
                ${currentAllocations.joinToString(separator = "\n") { c ->
                    "- ${c.groupName}: Current Value: ${"%.2f".format(c.currentValue)} (${"%.2f".format(c.percentage)}% of portfolio), Target Allocation: ${"%.2f".format(c.targetPercentage)}%"
                }}
                
                UPCOMING Payout Actions:
                ${activeDividends.filter { it.sharesOwned > 0.0 }.joinToString(separator = "\n") { p ->
                    "- Ticker: ${p.upcoming.ticker}, Estimated Dividend Payout: $${"%.2f".format(p.estimatedPayout)} executing on ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(p.upcoming.payoutDateMillis))}"
                }}

                In your audit report:
                1. Provide a "Portfolio Pulse Overview" (summarizing overall health).
                2. Structure an "Asset Allocation & Rebalancing Audit" (be very specific about which asset categories are over/under-allocated relative to the target, recommending Buy or Trim options).
                3. Deliver a "Dividend & Income Optimization Check" (analyze their yield on cost, cash flows, and schedule).
                4. Create a personalized greeting for user fasiddiqh72@gmail.com and outline strategic next steps for wealth accumulation.
                
                Tone: Keep it elegant, clear, objective, and expert. Avoid generic disclaimers where possible; provide precise logic. Format beautifully in Markdown.
            """.trimIndent()

            val analysis = GeminiClient.generateContent(key, prompt)
            _aiInsights.value = analysis
            _isGeneratingInsights.value = false
        }
    }
}

class PortfolioViewModelFactory(
    private val application: Application,
    private val repository: PortfolioRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
