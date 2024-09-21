package xyz.regulad.supir.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow

@Composable
fun <T : Any> FlowLazyColumn(
    flow: Flow<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    val items = produceState(initialValue = emptyList<T>()) {
        flow.collect { item ->
            value = value + item
        }
    }

    val lazyColumnState = rememberLazyListState()

    if (items.value.isEmpty()) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Loading...")
            Spacer(modifier = Modifier.height(8.dp))
        }
    } else {
        LazyColumn(modifier = modifier, state = lazyColumnState) {
            items(items.value) { item ->
                itemContent(item)
            }
        }
    }
}
