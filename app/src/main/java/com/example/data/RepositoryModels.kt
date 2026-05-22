package com.example.data

data class Holding(
    val ticker: String,
    val name: String,
    val category: String,
    val sector: String,
    val totalShares: Double,
    val averagePrice: Double,
    val totalCost: Double,
    val currentPrice: Double,
    val currentValue: Double,
    val totalGainLoss: Double,
    val totalGainLossPct: Double,
    val dividendsReceived: Double,
    val yieldOnCost: Double // annualized dividend payout / avg cost
)

data class PortfolioSummary(
    val totalValue: Double,
    val totalCost: Double,
    val totalGainLoss: Double,
    val totalGainLossPct: Double,
    val totalDividendsReceived: Double,
    val averageYield: Double,
    val baseCurrency: String
)

data class GroupedAllocation(
    val groupName: String, // Category or Sector
    val currentValue: Double,
    val percentage: Double,
    val targetPercentage: Double = 0.0
)

data class UpcomingDividendProjected(
    val upcoming: UpcomingDividend,
    val sharesOwned: Double,
    val estimatedPayout: Double,
    val daysRemaining: Int
)

data class DividendTimelineItem(
    val id: Long,
    val dateMillis: Long,
    val ticker: String,
    val amount: Double,
    val accountName: String,
    val notes: String
)

data class RealizedGain(
    val ticker: String,
    val name: String,
    val sharesSold: Double,
    val proceeds: Double,
    val costBasis: Double,
    val gainLoss: Double,
    val gainLossPct: Double,
    val dateMillis: Long
)

data class MonthlyPerformance(
    val month: String, // e.g., "Nov 2025"
    val startValue: Double,
    val netCashFlow: Double,
    val dividends: Double,
    val marketGrowth: Double,
    val endValue: Double,
    val returnPct: Double,
    val sp500ReturnPct: Double,
    val qqqReturnPct: Double,
    val nifty50ReturnPct: Double
)
