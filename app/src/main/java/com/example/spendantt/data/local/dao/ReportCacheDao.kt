package com.example.spendantt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spendantt.data.local.entity.ReportCacheEntity

@Dao
interface ReportCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReportCacheEntity)

    @Query("SELECT * FROM report_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getByKey(key: String): ReportCacheEntity?

    @Query("DELETE FROM report_cache WHERE cacheKey = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM report_cache WHERE userId = :userId AND generatedAt >= :since ORDER BY generatedAt DESC")
    suspend fun getRecentForUser(userId: Int, since: Long): List<ReportCacheEntity>

    @Query("DELETE FROM report_cache WHERE generatedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
