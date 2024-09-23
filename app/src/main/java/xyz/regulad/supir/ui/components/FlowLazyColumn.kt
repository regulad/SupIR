package xyz.regulad.supir.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow

@Composable
fun <T : Any> FlowLazyColumn(
    flow: Flow<T>,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = { Text("Loading...") },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    var flowFinished by remember { mutableStateOf(false) }
    val items = produceState(initialValue = emptyList<T>()) {
        flowFinished = false
        flow.collect { item ->
            value = value + item
        }
        flowFinished = true
    }

    val lazyColumnState = rememberLazyListState()

    if (items.value.isEmpty()) {
        Column(modifier = modifier) {
            loadingContent()
        }
    } else {
        LazyColumn(modifier = modifier, state = lazyColumnState) {
            items(items.value) { item ->
                itemContent(item)
            }

            if (!flowFinished) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    loadingContent()
                }
            }
        }
    }
}
