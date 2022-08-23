package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.api.IssueHead
import data.local.Notification

private val dateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)

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

@Composable
private fun ListItem(notification: Notification, onDismiss: (Notification) -> Unit) =
    Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(54, 54, 54)) {
        Column(Modifier.padding(2.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = timePrinter.format(notification.date), style = dateStyle)
                    Text(text = notification.head.key, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Row {
                    IconButton(onClick = { onDismiss(notification) }, modifier = Modifier.border(1.dp, Color.DarkGray)) {
                        Icon(Icons.Default.Delete, "dismiss notification")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(text = notification.title + " by " + notification.user, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = notification.info, fontSize = 14.sp)
        }
    }
