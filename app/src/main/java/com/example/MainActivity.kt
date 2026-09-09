package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.ExchangeViewModel
import com.example.ui.components.CurrencyPickerBottomSheet
import com.example.ui.components.ExchangeTopBar
import com.example.ui.screens.AllCountriesScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.ConverterScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CurrencyExchangeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyExchangeApp(
    viewModel: ExchangeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pinnedCurrencies by viewModel.pinnedCurrencies.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Tracks whether picking 'From' or 'To' currency
    var isPickingCurrencyForSource by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ExchangeTopBar(
                isOffline = uiState.isOffline,
                isLoading = uiState.isLoading,
                lastUpdatedUtc = uiState.lastUpdatedUtc,
                onRefresh = { viewModel.refreshRates() },
                modifier = Modifier.testTag("exchange_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.CONVERTER,
                    onClick = { viewModel.selectTab(AppTab.CONVERTER) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == AppTab.CONVERTER) Icons.Default.CurrencyExchange else Icons.Outlined.CurrencyExchange,
                            contentDescription = "Converter"
                        )
                    },
                    label = {
                        Text(
                            text = "Converter",
                            fontWeight = if (uiState.activeTab == AppTab.CONVERTER) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_converter")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.ALL_COUNTRIES,
                    onClick = { viewModel.selectTab(AppTab.ALL_COUNTRIES) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == AppTab.ALL_COUNTRIES) Icons.Default.Public else Icons.Outlined.Public,
                            contentDescription = "All Countries"
                        )
                    },
                    label = {
                        Text(
                            text = "All Countries",
                            fontWeight = if (uiState.activeTab == AppTab.ALL_COUNTRIES) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_all_countries")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.CALCULATOR,
                    onClick = { viewModel.selectTab(AppTab.CALCULATOR) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == AppTab.CALCULATOR) Icons.Default.Calculate else Icons.Outlined.Calculate,
                            contentDescription = "Travel Calc"
                        )
                    },
                    label = {
                        Text(
                            text = "Travel Calc",
                            fontWeight = if (uiState.activeTab == AppTab.CALCULATOR) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_calculator")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                AppTab.CONVERTER -> {
                    ConverterScreen(
                        uiState = uiState,
                        pinnedCurrencies = pinnedCurrencies,
                        onAmountChanged = { viewModel.setAmount(it) },
                        onKeypadPress = { viewModel.appendKeypadInput(it) },
                        onQuickAmountSelected = { viewModel.setQuickAmount(it) },
                        onSwapCurrencies = { viewModel.swapCurrencies() },
                        onOpenFromPicker = { isPickingCurrencyForSource = true },
                        onOpenToPicker = { isPickingCurrencyForSource = false },
                        onTargetCurrencySelected = { viewModel.setToCurrency(it) }
                    )
                }

                AppTab.ALL_COUNTRIES -> {
                    AllCountriesScreen(
                        uiState = uiState,
                        pinnedCurrencies = pinnedCurrencies,
                        onTogglePin = { viewModel.togglePin(it) },
                        onCurrencyClick = { code ->
                            viewModel.setToCurrency(code)
                            viewModel.selectTab(AppTab.CONVERTER)
                        },
                        onChangeBaseClick = { isPickingCurrencyForSource = true }
                    )
                }

                AppTab.CALCULATOR -> {
                    CalculatorScreen(
                        uiState = uiState,
                        onApplyToConverter = { amount ->
                            viewModel.setQuickAmount(amount)
                            viewModel.selectTab(AppTab.CONVERTER)
                        },
                        onOpenFromPicker = { isPickingCurrencyForSource = true },
                        onOpenToPicker = { isPickingCurrencyForSource = false }
                    )
                }
            }
        }

        // Currency Picker Sheet
        if (isPickingCurrencyForSource != null) {
            val isSource = isPickingCurrencyForSource == true
            CurrencyPickerBottomSheet(
                sheetState = sheetState,
                selectedCode = if (isSource) uiState.fromCurrency else uiState.toCurrency,
                onCurrencySelected = { selected ->
                    if (isSource) {
                        viewModel.setFromCurrency(selected)
                    } else {
                        viewModel.setToCurrency(selected)
                    }
                },
                onDismiss = { isPickingCurrencyForSource = null }
            )
        }
    }
}

// Retained for tests and preview compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Currency Exchange")
    }
}
