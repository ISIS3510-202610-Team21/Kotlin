package com.example.spendantt.data.preferences

import android.content.Context

// LOCAL STORAGE | Nano | 5pts | Preferences: SharedPreferences para persistir el goal seleccionado por usuario entre sesiones, clave compuesta por userId para aislamiento multi-usuario
class GoalPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedGoalId(userId: Int): Int? {
        val value = prefs.getInt(selectedGoalKey(userId), NO_GOAL_SELECTED)
        return value.takeIf { it != NO_GOAL_SELECTED }
    }

    fun setSelectedGoalId(userId: Int, goalId: Int) {
        prefs.edit().putInt(selectedGoalKey(userId), goalId).apply()
    }

    fun clearSelectedGoalId(userId: Int) {
        prefs.edit().remove(selectedGoalKey(userId)).apply()
    }

    private fun selectedGoalKey(userId: Int): String = "selected_goal_$userId"

    companion object {
        private const val PREFS_NAME = "goal_prefs"
        private const val NO_GOAL_SELECTED = -1
    }
}
