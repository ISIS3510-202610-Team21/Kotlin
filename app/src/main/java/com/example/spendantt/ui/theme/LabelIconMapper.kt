package com.example.spendantt.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.spendantt.R

object LabelIconMapper {
    /**
     * Mapea el nombre del label a su icono drawable correspondiente
     */
    fun getIconForLabel(labelName: String): Int {
        return when (labelName.lowercase().replace(" ", "").replace("/", "")) {
            "universityfees" -> R.drawable.universityfees
            "learningmaterials" -> R.drawable.learningmaterials
            "commute" -> R.drawable.commute
            "food" -> R.drawable.food
            "grouphangouts" -> R.drawable.grouphangouts
            "fooddelivery" -> R.drawable.fooddelivery
            "entertainment", "entertaiment" -> R.drawable.entertaiment
            "subscriptions" -> R.drawable.subscriptions
            "gifts" -> R.drawable.gifts
            "rent" -> R.drawable.rent
            "utilities" -> R.drawable.utilities
            "services" -> R.drawable.services
            "groceries" -> R.drawable.groceries
            "personalcare" -> R.drawable.personalcare
            "transport" -> R.drawable.transport
            "owed" -> R.drawable.owed
            "impulse" -> R.drawable.impulse
            "emergency" -> R.drawable.emergency
            else -> R.drawable.food // Default fallback
        }
    }

    /**
     * Mapea la categoría a su color correspondiente
     */
    fun getColorForCategory(category: String?): Color {
        return when (category) {
            "Academic Essentials" -> CategoryBlue
            "Lifestyle & Social" -> CategoryOrange
            "Living Expenses" -> CategoryMintGreen
            "Strategic & Utility Tags" -> CategoryCoralPink
            else -> CategoryYellow
        }
    }

    /**
     * Lista de todos los colores disponibles para rotación
     */
    private val allCategoryColors = listOf(
        CategoryBlue,
        CategoryOrange,
        CategoryYellow,
        CategoryCoralPink,
        CategoryMintGreen,
        CategoryLavenderPurple,
        CategorySkyCyan,
        CategoryChartreuseGreen,
        CategoryDeepRubyRed,
        CategoryCoralPeach,
        CategoryCobaltBlue,
        CategorySoftSageGreen,
        CategoryDeepPlumPurple,
        CategoryGoldOcher
    )

    /**
     * Obtiene un color basado en el hash del nombre para consistencia
     */
    fun getConsistentColorForLabel(labelName: String): Color {
        val index = kotlin.math.abs(labelName.hashCode()) % allCategoryColors.size
        return allCategoryColors[index]
    }
}
