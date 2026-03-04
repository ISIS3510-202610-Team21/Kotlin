package com.example.spendantt.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.spendantt.data.local.dao.ExpenseDao
import com.example.spendantt.data.local.dao.GoalDao
import com.example.spendantt.data.local.dao.IncomeDao
import com.example.spendantt.data.local.dao.LabelDao
import com.example.spendantt.data.local.dao.UserDao
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseLabelCrossRef
import com.example.spendantt.data.local.entity.GoalEntity
import com.example.spendantt.data.local.entity.IncomeEntity
import com.example.spendantt.data.local.entity.LabelEntity
import com.example.spendantt.data.local.entity.UserEntity

/**
 * CAMBIOS vs versión anterior:
 * + IncomeEntity  → funcionalidad 6 (Set a Budget)
 * + GoalEntity    → funcionalidad 7 (Set a Goal)
 * + version: 1→2  → siempre incrementar al agregar/modificar entidades
 *
 * Fase 2: descomentar MIGRATION_2_3 y agregar campos serverId/isSynced
 */
@Database(
    entities = [
        UserEntity::class,
        ExpenseEntity::class,
        LabelEntity::class,
        ExpenseLabelCrossRef::class,
        IncomeEntity::class,    // NUEVO
        GoalEntity::class       // NUEVO
    ],
    version = 2,                // INCREMENTADO de 1 a 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun labelDao(): LabelDao
    abstract fun incomeDao(): IncomeDao     // NUEVO
    abstract fun goalDao(): GoalDao         // NUEVO

    companion object {
        private const val DATABASE_NAME = "spendant_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // ── DESARROLLO: borra y recrea si hay cambios ──
                    // Quitar en producción y usar addMigrations()
                    .fallbackToDestructiveMigration()

                    // Fase 2: reemplazar con:
                    // .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // Migración de versión 1 → 2 (agregar Income y Goal)
        // Descomentar si ya tienes datos reales y no quieres perderlos
        // val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         database.execSQL("""
        //             CREATE TABLE IF NOT EXISTS incomes (
        //                 id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        //                 userId INTEGER NOT NULL,
        //                 name TEXT NOT NULL,
        //                 amount REAL NOT NULL,
        //                 type TEXT NOT NULL,
        //                 recurrenceInterval INTEGER,
        //                 recurrenceUnit TEXT,
        //                 nextOccurrenceDate INTEGER,
        //                 startDate INTEGER NOT NULL,
        //                 createdAt INTEGER NOT NULL
        //             )
        //         """)
        //         database.execSQL("""
        //             CREATE TABLE IF NOT EXISTS goals (
        //                 id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        //                 userId INTEGER NOT NULL,
        //                 name TEXT NOT NULL,
        //                 targetAmount REAL NOT NULL,
        //                 currentAmount REAL NOT NULL,
        //                 deadline INTEGER NOT NULL,
        //                 isCompleted INTEGER NOT NULL,
        //                 createdAt INTEGER NOT NULL
        //             )
        //         """)
        //         database.execSQL("ALTER TABLE users ADD COLUMN displayName TEXT")
        //         database.execSQL("ALTER TABLE users ADD COLUMN handle TEXT")
        //         database.execSQL("ALTER TABLE users ADD COLUMN avatarPath TEXT")
        //         database.execSQL("ALTER TABLE expenses ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
        //         database.execSQL("ALTER TABLE expenses ADD COLUMN recurrenceInterval INTEGER")
        //         database.execSQL("ALTER TABLE expenses ADD COLUMN recurrenceUnit TEXT")
        //         database.execSQL("ALTER TABLE expenses ADD COLUMN nextOccurrenceDate INTEGER")
        //     }
        // }

        // Fase 2: migración 2 → 3 (agregar campos de backend)
        // val MIGRATION_2_3 = object : Migration(2, 3) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         database.execSQL("ALTER TABLE users ADD COLUMN serverId TEXT")
        //         database.execSQL("ALTER TABLE users ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
        //         database.execSQL("ALTER TABLE expenses ADD COLUMN serverId TEXT")
        //         database.execSQL("ALTER TABLE expenses ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
        //         database.execSQL("ALTER TABLE incomes ADD COLUMN serverId TEXT")
        //         database.execSQL("ALTER TABLE incomes ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
        //         database.execSQL("ALTER TABLE goals ADD COLUMN serverId TEXT")
        //         database.execSQL("ALTER TABLE goals ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
        //     }
        // }
    }
}
