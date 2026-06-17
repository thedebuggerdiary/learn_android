package com.debuggerdiary.ep04

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class Task(val id: Int, val title: String, val description: String)

val sampleTasks = listOf(
    Task(1, "Buy milk", "Get 2% from the corner store"),
    Task(2, "Read book", "Finish chapter 5 of Clean Code"),
    Task(3, "Call mom", "Sunday evening works best"),
    Task(4, "Write tests", "Cover the new ViewModel logic"),
    Task(5, "Push to GitHub", "Open a PR for the navigation branch")
)

@Composable
fun ListScreen(
    onTaskClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Text(
                text = "My Tasks",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(sampleTasks, key = { it.id }) { task ->
            ListItem(
                headlineContent = { Text(task.title) },
                supportingContent = { Text(task.description) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open ${task.title}"
                    )
                },
                modifier = Modifier.clickable { onTaskClick(task.id) }
            )
            HorizontalDivider()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListScreenPreview() {
    MaterialTheme {
        ListScreen(onTaskClick = {})
    }
}
