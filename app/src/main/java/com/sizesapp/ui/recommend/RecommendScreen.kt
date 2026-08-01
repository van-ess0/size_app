package com.sizesapp.ui.recommend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sizesapp.data.db.ClothingCategory
import com.sizesapp.data.sizing.Recommendation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendScreen(viewModel: RecommendViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("What size do I wear?") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = state.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    ClothingCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                viewModel.updateCategory(category)
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.brand,
                onValueChange = viewModel::updateBrand,
                label = { Text("Brand, e.g. Adidas") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::getRecommendation,
                enabled = state.brand.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("What size do I wear?")
            }

            state.result?.let { RecommendationCard(it) }
        }
    }
}

@Composable
private fun RecommendationCard(result: Recommendation) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (result) {
                is Recommendation.DirectMatch -> {
                    Text("Size ${result.basedOn.sizeLabel} ${result.basedOn.sizeSystem}", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    Text("Based on the ${result.basedOn.brand} item you already logged (rated ${result.basedOn.fitRating.name.replace('_', ' ').lowercase()}).")
                    result.note?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
                }
                is Recommendation.Converted -> {
                    Text("Try size ${result.suggestedSize} ${result.sizeSystem}", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    result.note?.let { Text(it) }
                }
                is Recommendation.NoData -> {
                    Text("Not enough data yet")
                    Text(result.reason, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
