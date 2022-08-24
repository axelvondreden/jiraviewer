package ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.api.IssueHead
import data.local.Notification

private val dateStyle = SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)

@ExperimentalMaterialApi
@Composable
fun NotificationList(modifier: Modifier, items: List<Notification>, onOpenIssue: (IssueHead) -> Unit, onDismiss: (Notification) -> Unit) = Scaffold(
    modifier = modifier,
    topBar = { },
    content = {
        Box(Modifier.fillMaxSize()) {
            val scroll = rememberScrollState()
            Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Column {
                    items.forEach {
                        Box(modifier = Modifier.clickable { onOpenIssue(it.head) }, contentAlignment = Alignment.CenterStart) {
                            ListItem(it, onDismiss)
                        }
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
)

@ExperimentalMaterialApi
@Composable
private fun ListItem(notification: Notification, onDismiss: (Notification) -> Unit) {
    var expandedState by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expandedState) 180f else 0f)
    val repo = Repository.current

    Card(
        modifier = Modifier.padding(4.dp).fillMaxWidth().animateContentSize(tween(durationMillis = 300, easing = LinearOutSlowInEasing)),
        backgroundColor = Color(54, 54, 54)
    ) {
        Column(Modifier.padding(2.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1F)) {
                    Text(
                        text = notification.head.key + ": " + notification.head.fields.summary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    val typeText = AnnotatedString.Builder().apply {
                        pushStyle(SpanStyle(color = Color.LightGray, fontSize = 13.sp))
                        append("${notification.title} by ")
                        pushStyle(SpanStyle(color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        append(notification.user + " ")
                        pushStyle(dateStyle)
                        append(timePrinter.format(notification.date))
                    }.toAnnotatedString()
                    Text(text = typeText, overflow = TextOverflow.Ellipsis, maxLines = 1)
                }
                Row(modifier = Modifier.wrapContentWidth()) {
                    IconButton(onClick = { expandedState = false; onDismiss(notification) }) {
                        Icon(Icons.Default.Delete, "dismiss notification")
                    }
                }
            }
            var iconNeeded by remember { mutableStateOf(false) }
            if (expandedState) {
                iconNeeded = true
                if (notification.isComment) {
                    Text(notification.info.first().parseJiraText(13.sp, repo))
                } else {
                    notification.info.forEach {
                        Text(text = it, fontSize = 13.sp)
                    }
                }
            } else if (notification.isComment) {
                Text(
                    text = notification.info.first().parseJiraText(13.sp, repo),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                    onTextLayout = { if (it.didOverflowHeight) iconNeeded = true }
                )
            } else {
                if (notification.info.size > 3) iconNeeded = true
                notification.info.take(3).forEach { change ->
                    Text(
                        text = change,
                        fontSize = 13.sp,
                        softWrap = false,
                        maxLines = 1,
                        onTextLayout = { if (it.didOverflowHeight) iconNeeded = true }
                    )
                }
            }
            if (iconNeeded) {
                Row(modifier = Modifier.clickable { expandedState = !expandedState }.height(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "toggle notification expanse",
                        modifier = Modifier.alpha(ContentAlpha.medium).rotate(rotationState)
                    )
                }
            }
        }
    }
}
