package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeDao {
    @Query("SELECT * FROM cached_rates WHERE baseCode = :base")
    fun getRatesForBase(base: String): Flow<List<CachedRateEntity>>

    @Query("SELECT * FROM cached_rates WHERE baseCode = :base")
    suspend fun getRatesForBaseSync(base: String): List<CachedRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<CachedRateEntity>)

    @Query("SELECT * FROM pinned_currencies ORDER BY addedTimestamp ASC")
    fun getPinnedCurrencies(): Flow<List<PinnedCurrencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pinCurrency(pinned: PinnedCurrencyEntity)

    @Query("DELETE FROM pinned_currencies WHERE code = :code")
    suspend fun unpinCurrency(code: String)

    @Query("SELECT COUNT(*) FROM pinned_currencies")
    suspend fun getPinnedCount(): Int
}
