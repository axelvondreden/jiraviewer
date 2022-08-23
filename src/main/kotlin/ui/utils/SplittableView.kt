package ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skiko.Cursor


class SplitterState {
    var isResizing by mutableStateOf(false)
    var isResizeEnabled by mutableStateOf(true)
}


@Composable
fun HorizontalSplittable(modifier: Modifier, state: SplitterState, onResize: (delta: Dp) -> Unit, children: @Composable () -> Unit) = Layout(
    content = {
        children()
        HorizontalSplitter(state, onResize)
    },
    modifier = modifier,
    measurePolicy = { measurables, constraints ->
        require(measurables.size == 3)

        val firstPlaceable = measurables[0].measure(constraints.copy(minHeight = 0))
        val secondHeight = constraints.maxHeight - firstPlaceable.height
        val secondPlaceable = measurables[1].measure(
            Constraints(
                minWidth = constraints.maxWidth,
                maxWidth = constraints.maxWidth,
                minHeight = secondHeight,
                maxHeight = secondHeight
            )
        )
        val splitterPlaceable = measurables[2].measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            firstPlaceable.place(0, 0)
            secondPlaceable.place(0, firstPlaceable.height)
            splitterPlaceable.place(0, firstPlaceable.height)
        }
    }
)

@Composable
fun VerticalSplittable(modifier: Modifier, state: SplitterState, onResize: (delta: Dp) -> Unit, children: @Composable () -> Unit) = Layout(
    content = {
        children()
        VerticalSplitter(state, onResize)
    },
    modifier = modifier,
    measurePolicy = { measurables, constraints ->
        require(measurables.size == 3)

        val firstPlaceable = measurables[0].measure(constraints.copy(minWidth = 0))
        val secondWidth = constraints.maxWidth - firstPlaceable.width
        val secondPlaceable = measurables[1].measure(
            Constraints(
                minWidth = secondWidth,
                maxWidth = secondWidth,
                minHeight = constraints.maxHeight,
                maxHeight = constraints.maxHeight
            )
        )
        val splitterPlaceable = measurables[2].measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            firstPlaceable.place(0, 0)
            secondPlaceable.place(firstPlaceable.width, 0)
            splitterPlaceable.place(firstPlaceable.width, 0)
        }
    })


@Composable
private fun HorizontalSplitter(splitterState: SplitterState, onResize: (delta: Dp) -> Unit) = Box {
    val density = LocalDensity.current
    val state = rememberDraggableState { with(density) { onResize(it.toDp()) } }
    Box(Modifier.height(8.dp).fillMaxWidth().draggableIf(state, splitterState, Orientation.Vertical) { splitterState.isResizeEnabled })
    Box(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colors.onBackground))
}

@Composable
private fun VerticalSplitter(splitterState: SplitterState, onResize: (delta: Dp) -> Unit) = Box {
    val density = LocalDensity.current
    val state = rememberDraggableState { with(density) { onResize(it.toDp()) } }
    Box(Modifier.width(8.dp).fillMaxHeight().draggableIf(state, splitterState, Orientation.Horizontal) { splitterState.isResizeEnabled })
    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colors.onBackground))
}

private fun Modifier.draggableIf(state: DraggableState, splitterState: SplitterState, orientation: Orientation, condition: () -> Boolean): Modifier {
    return if (condition()) this.draggable(
        state = state,
        orientation = orientation,
        startDragImmediately = true,
        onDragStarted = { splitterState.isResizing = true },
        onDragStopped = { splitterState.isResizing = false }
    ).pointerHoverIcon(PointerIcon(Cursor(if (orientation == Orientation.Horizontal) Cursor.E_RESIZE_CURSOR else Cursor.N_RESIZE_CURSOR))) else this
}
