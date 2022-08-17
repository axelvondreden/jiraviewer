package ui.splitter

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skiko.Cursor

@Composable
fun HorizontalSplittable(
    modifier: Modifier,
    splitterState: SplitterState,
    onResize: (delta: Dp) -> Unit,
    children: @Composable () -> Unit
) = Layout({
    children()
    HorizontalSplitter(splitterState, onResize)
}, modifier, measurePolicy = { measurables, constraints ->
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
})

@Composable
fun HorizontalSplitter(
    splitterState: SplitterState,
    onResize: (delta: Dp) -> Unit,
    color: Color = MaterialTheme.colors.onBackground
) = Box {
    val density = LocalDensity.current
    Box(
        Modifier
            .height(8.dp)
            .fillMaxWidth()
            .run {
                if (splitterState.isResizeEnabled) {
                    this.draggable(
                        state = rememberDraggableState {
                            with(density) {
                                onResize(it.toDp())
                            }
                        },
                        orientation = Orientation.Vertical,
                        startDragImmediately = true,
                        onDragStarted = { splitterState.isResizing = true },
                        onDragStopped = { splitterState.isResizing = false }
                    ).cursorForVerticalResize()
                } else {
                    this
                }
            }
    )

    Box(
        Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(color)
    )
}

fun Modifier.cursorForVerticalResize(): Modifier = pointerHoverIcon(PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)))
