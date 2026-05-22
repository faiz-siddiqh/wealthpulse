package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.PortfolioRepository
import com.example.ui.MainPortfolioApp
import com.example.ui.PortfolioViewModel
import com.example.ui.PortfolioViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize Database and Repository
    val database = AppDatabase.getDatabase(this)
    val repository = PortfolioRepository(
        accountDao = database.accountDao(),
        investmentDao = database.investmentDao(),
        transactionDao = database.transactionDao(),
        upcomingDividendDao = database.upcomingDividendDao(),
        currencyRateDao = database.currencyRateDao()
    )
    
    // Create ViewModel using custom injection Factory
    val viewModelFactory = PortfolioViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[PortfolioViewModel::class.java]

    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      MyApplicationTheme(themeName = themeMode) {
        MainPortfolioApp(viewModel = viewModel)
      }
    }
  }
}
