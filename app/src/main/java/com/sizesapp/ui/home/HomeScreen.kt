package com.sizesapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sizesapp.data.db.ClosetItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onScanClick: () -> Unit,
    onItemClick: (Long) -> Unit,
) {
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("My closet") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onScanClick) {
                Icon(Icons.Filled.Add, contentDescription = "Scan a new item")
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No items yet. Tap + to scan your first label.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(items, key = { it.id }) { item ->
                    ClosetItemRow(item, onClick = { onItemClick(item.id) })
                }
            }
        }
    }
}

@Composable
private fun ClosetItemRow(item: ClosetItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        ListItem(
            headlineContent = { Text("${item.brand} -- ${item.category.name.lowercase().replaceFirstChar { it.uppercase() }}") },
            supportingContent = {
                Text("Size ${item.sizeLabel} (${item.sizeSystem}) -- ${item.fitRating.name.replace('_', ' ').lowercase()}")
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
