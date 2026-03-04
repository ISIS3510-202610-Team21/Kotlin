package com.example.spendantt.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spendantt.data.local.entity.LabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    // ── INSERTAR ──────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<LabelEntity>)

    // ── OBTENER ───────────────────────────────────────────────
    // Flow → la UI se actualiza automáticamente cuando cambian las etiquetas
    @Query("SELECT * FROM labels WHERE userId = :userId ORDER BY name ASC")
    fun getLabelsByUser(userId: Int): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE userId = :userId AND name LIKE '%' || :query || '%'")
    fun searchLabels(userId: Int, query: String): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE id = :labelId LIMIT 1")
    suspend fun getLabelById(labelId: Int): LabelEntity?

    // ── ACTUALIZAR / ELIMINAR ─────────────────────────────────
    @Update
    suspend fun updateLabel(label: LabelEntity)

    @Delete
    suspend fun deleteLabel(label: LabelEntity)

    // Fase 2: descomentar para sincronización con backend
    // @Query("SELECT * FROM labels WHERE isSynced = 0")
    // fun getUnsyncedLabels(): Flow<List<LabelEntity>>
}