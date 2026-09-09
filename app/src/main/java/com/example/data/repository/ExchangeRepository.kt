package com.example.data.repository

import com.example.data.local.CachedRateEntity
import com.example.data.local.ExchangeDao
import com.example.data.local.PinnedCurrencyEntity
import com.example.data.model.CurrencyCatalog
import com.example.data.remote.ApiClient
import com.example.data.remote.ExchangeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class RateFetchResult(
    val rates: Map<String, Double>,
    val lastUpdatedUtc: String,
    val isFromCache: Boolean
)

class ExchangeRepository(
    private val dao: ExchangeDao,
    private val apiService: ExchangeApiService = ApiClient.apiService
) {
    val pinnedCurrencies: Flow<List<String>> = dao.getPinnedCurrencies().map { list ->
        list.map { it.code }
    }

    suspend fun initializeDefaultPinnedIfEmpty() {
        withContext(Dispatchers.IO) {
            if (dao.getPinnedCount() == 0) {
                CurrencyCatalog.defaultPinnedCodes.forEach { code ->
                    dao.pinCurrency(PinnedCurrencyEntity(code = code))
                }
            }
        }
    }

    suspend fun togglePin(code: String, isCurrentlyPinned: Boolean) {
        withContext(Dispatchers.IO) {
            if (isCurrentlyPinned) {
                dao.unpinCurrency(code)
            } else {
                dao.pinCurrency(PinnedCurrencyEntity(code = code))
            }
        }
    }

    suspend fun getExchangeRates(baseCurrency: String): Result<RateFetchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLatestRates(baseCurrency)
                val rates = response.rates
                if (response.result == "success" && !rates.isNullOrEmpty()) {
                    val updateTime = response.timeLastUpdateUtc ?: "Just now"
                    val entities = rates.map { (target, rate) ->
                        CachedRateEntity(
                            baseCode = baseCurrency,
                            targetCode = target,
                            rate = rate,
                            lastUpdatedUtc = updateTime
                        )
                    }
                    dao.insertRates(entities)
                    Result.success(
                        RateFetchResult(
                            rates = rates,
                            lastUpdatedUtc = updateTime,
                            isFromCache = false
                        )
                    )
                } else {
                    fallbackToCache(baseCurrency)
                }
            } catch (e: Exception) {
                fallbackToCache(baseCurrency)
            }
        }
    }

    private suspend fun fallbackToCache(baseCurrency: String): Result<RateFetchResult> {
        val cached = dao.getRatesForBaseSync(baseCurrency)
        return if (cached.isNotEmpty()) {
            val rateMap = cached.associate { it.targetCode to it.rate }
            val lastUpdated = cached.firstOrNull()?.lastUpdatedUtc ?: "Offline cache"
            Result.success(
                RateFetchResult(
                    rates = rateMap,
                    lastUpdatedUtc = "$lastUpdated (Offline)",
                    isFromCache = true
                )
            )
        } else {
            Result.failure(Exception("Unable to fetch live rates and no cached data available."))
        }
    }
}
