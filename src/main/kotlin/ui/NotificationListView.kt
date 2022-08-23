package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
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
import data.local.Notification

private val dateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)

@Composable
fun NotificationList(
    modifier: Modifier = Modifier,
    notifications: List<Notification>,
    onClick: (Notification) -> Unit,
    onDismiss: (Notification) -> Unit
) = Scaffold(
    modifier = modifier,
    topBar = { },
    content = {
        Box(Modifier.fillMaxSize()) {
            val scroll = rememberScrollState()
            Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Column {
                    notifications.forEach {
                        ListItem(it, onClick, onDismiss)
                    }
                }
            }
        }
    }
)

@Composable
private fun ListItem(
    notification: Notification,
    onClick: (Notification) -> Unit,
    onDismiss: (Notification) -> Unit
) = Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(54, 54, 54)) {
    Column(Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = notification.head.key, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row {
                IconButton(onClick = { onClick(notification) }) {
                    Icon(Icons.Default.ArrowRight, "open issue")
                }
                IconButton(onClick = { onDismiss(notification) }) {
                    Icon(Icons.Default.Delete, "dismiss notification")
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = timePrinter.format(notification.date), style = dateStyle)
        Spacer(Modifier.height(4.dp))
        Text(text = notification.title + " by " + notification.user, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = notification.info, fontSize = 14.sp)
        /*Text(issueHead.fields.summary ?: "")
        Row {
            issueHead.fields.status?.name?.let {
                Text(it, modifier = Modifier.padding(2.dp), color = Color.Gray)
            }
            issueHead.fields.priority?.name?.let {
                Spacer(Modifier.width(5.dp))
                Text(it, modifier = Modifier.padding(2.dp), color = Color.Gray)
            }
        }*/
    }
}
