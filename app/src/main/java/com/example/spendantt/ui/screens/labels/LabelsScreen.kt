package com.example.spendantt.ui.screens.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendantt.data.local.entity.LabelEntity
import com.example.spendantt.ui.components.BlackButton
import com.example.spendantt.ui.theme.*

@Composable
fun LabelsScreen(
    labelsGroupedByCategory: Map<String, List<LabelEntity>>,
    selectedLabelIds: Set<Int>,
    onLabelToggle: (LabelEntity) -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpendAntWhite)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpendAntGreen)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SpendAntBlack
                    )
                }
                Text(
                    text = "Labels",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpendAntFontFamily,
                    color = SpendAntBlack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                labelsGroupedByCategory.forEach { (category, labels) ->
                    item {
                        LabelCategorySection(
                            categoryName = category,
                            labels = labels,
                            selectedLabelIds = selectedLabelIds,
                            onLabelToggle = onLabelToggle
                        )
                    }
                }
            }

            // Done Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                BlackButton(
                    text = "Done",
                    onClick = onDone,
                    width = 200.dp,
                    height = 50.dp,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun LabelCategorySection(
    categoryName: String,
    labels: List<LabelEntity>,
    selectedLabelIds: Set<Int>,
    onLabelToggle: (LabelEntity) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category Header
        Text(
            text = categoryName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SpendAntFontFamily,
            color = SpendAntBlack
        )

        // Labels Flow
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEach { label ->
                LabelChip(
                    label = label,
                    isSelected = selectedLabelIds.contains(label.id),
                    onClick = { onLabelToggle(label) }
                )
            }
        }
    }
}

@Composable
private fun LabelChip(
    label: LabelEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) SpendAntGreen else SpendAntWhite
    val borderColor = if (isSelected) SpendAntGreen else Color(0xFFE0E0E0)
    val textColor = SpendAntBlack

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = label.name,
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontFamily = SpendAntFontFamily,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
