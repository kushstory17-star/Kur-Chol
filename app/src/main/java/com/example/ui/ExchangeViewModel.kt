package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CurrencyCatalog
import com.example.data.model.CurrencyInfo
import com.example.data.repository.ExchangeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class AppTab {
    CONVERTER,
    ALL_COUNTRIES,
    CALCULATOR
}

data class ConvertedCountryRate(
    val currencyInfo: CurrencyInfo,
    val unitRate: Double,
    val convertedAmount: Double,
    val isPinned: Boolean
)

data class ExchangeUiState(
    val fromCurrency: String = "USD",
    val toCurrency: String = "EUR",
    val amountInput: String = "100",
    val numericAmount: Double = 100.0,
    val rates: Map<String, Double> = emptyMap(),
    val directRate: Double = 0.0,
    val inverseRate: Double = 0.0,
    val convertedResult: Double = 0.0,
    val lastUpdatedUtc: String = "Loading...",
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val activeTab: AppTab = AppTab.CONVERTER
)

class ExchangeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ExchangeRepository(database.exchangeDao())

    private val _uiState = MutableStateFlow(ExchangeUiState())
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    val pinnedCurrencies: StateFlow<List<String>> = repository.pinnedCurrencies
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CurrencyCatalog.defaultPinnedCodes
        )

    init {
        viewModelScope.launch {
            repository.initializeDefaultPinnedIfEmpty()
        }
        loadRates(baseCurrency = _uiState.value.fromCurrency)
    }

    fun selectTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setAmount(amountStr: String) {
        val sanitized = amountStr.replace(",", ".").trim()
        val num = sanitized.toDoubleOrNull() ?: 0.0
        val targetRate = _uiState.value.rates[_uiState.value.toCurrency] ?: 1.0
        val result = num * targetRate

        _uiState.value = _uiState.value.copy(
            amountInput = amountStr,
            numericAmount = num,
            convertedResult = result
        )
    }

    fun appendKeypadInput(key: String) {
        val current = _uiState.value.amountInput
        val next = when (key) {
            "C" -> "0"
            "⌫" -> if (current.length > 1) current.dropLast(1) else "0"
            "." -> if (current.contains(".")) current else "$current."
            else -> if (current == "0") key else current + key
        }
        setAmount(next)
    }

    fun setQuickAmount(amount: Double) {
        val text = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
        setAmount(text)
    }

    fun setFromCurrency(newBase: String) {
        if (newBase == _uiState.value.fromCurrency) return
        _uiState.value = _uiState.value.copy(fromCurrency = newBase)
        loadRates(baseCurrency = newBase)
    }

    fun setToCurrency(newTarget: String) {
        if (newTarget == _uiState.value.toCurrency) return
        val direct = _uiState.value.rates[newTarget] ?: 1.0
        val inverse = if (direct > 0.0) 1.0 / direct else 0.0
        val result = _uiState.value.numericAmount * direct

        _uiState.value = _uiState.value.copy(
            toCurrency = newTarget,
            directRate = direct,
            inverseRate = inverse,
            convertedResult = result
        )
    }

    fun swapCurrencies() {
        val oldFrom = _uiState.value.fromCurrency
        val oldTo = _uiState.value.toCurrency
        _uiState.value = _uiState.value.copy(
            fromCurrency = oldTo,
            toCurrency = oldFrom
        )
        loadRates(baseCurrency = oldTo)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun togglePin(currencyCode: String) {
        viewModelScope.launch {
            val isPinned = pinnedCurrencies.value.contains(currencyCode)
            repository.togglePin(currencyCode, isPinned)
        }
    }

    fun refreshRates() {
        loadRates(baseCurrency = _uiState.value.fromCurrency)
    }

    private fun loadRates(baseCurrency: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getExchangeRates(baseCurrency)
            result.onSuccess { fetchResult ->
                val rates = fetchResult.rates
                val target = _uiState.value.toCurrency
                val direct = rates[target] ?: 1.0
                val inverse = if (direct > 0.0) 1.0 / direct else 0.0
                val converted = _uiState.value.numericAmount * direct

                _uiState.value = _uiState.value.copy(
                    rates = rates,
                    directRate = direct,
                    inverseRate = inverse,
                    convertedResult = converted,
                    lastUpdatedUtc = fetchResult.lastUpdatedUtc,
                    isLoading = false,
                    isOffline = fetchResult.isFromCache,
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to fetch live rates."
                )
            }
        }
    }

    companion object {
        private val symbols = DecimalFormatSymbols(Locale.US)
        private val standardFormatter = DecimalFormat("#,##0.00", symbols)
        private val preciseFormatter = DecimalFormat("#,##0.0000", symbols)
        private val highPrecisionFormatter = DecimalFormat("#,##0.######", symbols)

        fun formatAmount(value: Double): String {
            return when {
                value >= 1000.0 -> standardFormatter.format(value)
                value >= 1.0 -> standardFormatter.format(value)
                value >= 0.0001 -> preciseFormatter.format(value)
                value > 0 -> highPrecisionFormatter.format(value)
                else -> "0.00"
            }
        }

        fun formatRate(rate: Double): String {
            return when {
                rate >= 100.0 -> standardFormatter.format(rate)
                rate >= 1.0 -> preciseFormatter.format(rate)
                else -> highPrecisionFormatter.format(rate)
            }
        }
    }
}
