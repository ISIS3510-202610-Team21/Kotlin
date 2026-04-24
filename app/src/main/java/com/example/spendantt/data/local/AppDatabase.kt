package com.example.spendantt.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 * + LabelEntity.category → funcionalidad 8 (Labels por categorías)
 * + version: 1→2→3→4  → siempre incrementar al agregar/modificar entidades
 *
 * Fase 2: descomentar MIGRATION_4_5 y agregar campos serverId/isSynced
 */
@Database(
    entities = [
        UserEntity::class,
        ExpenseEntity::class,
        LabelEntity::class,
        ExpenseLabelCrossRef::class,
        IncomeEntity::class,
        GoalEntity::class
    ],
    version = 4,
    exportSchema = false
)
// LOCAL STORAGE | Dilan | 10pts | BD Relacional Room: userDao() — UserEntity persistida en SQLite; usada en LoginViewModel y RegisterViewModel para autenticación local
// LOCAL STORAGE | William | 10pts | BD Relacional Room: expenseDao() + labelDao() — relación many-to-many ExpenseEntity↔LabelEntity con ExpenseLabelCrossRef
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun labelDao(): LabelDao
    abstract fun incomeDao(): IncomeDao
    abstract fun goalDao(): GoalDao

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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migración 2→3: agrega firebaseUid a users
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users ADD COLUMN firebaseUid TEXT")
            }
        }

        // Migración 3→4: agrega category a labels
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE labels ADD COLUMN category TEXT")
            }
        }
    }
}
