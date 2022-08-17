package ui.splitter

import androidx.compose.runtime.*

class SplitterState {
    var isResizing by mutableStateOf(false)
    var isResizeEnabled by mutableStateOf(true)
}