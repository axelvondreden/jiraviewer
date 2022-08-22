package ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
fun NotificationList(modifier: Modifier = Modifier, notifications: List<Notification>) = Scaffold(
    modifier = modifier,
    topBar = { },
    content = {
        Box(Modifier.fillMaxSize()) {
            val scroll = rememberScrollState()
            Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Column {
                    notifications.forEach {
                        ListItem(it)
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }

    }
)

@Composable
private fun ListItem(notification: Notification) = Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(54, 54, 54)) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = notification.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Text(text = timePrinter.format(notification.date), style = dateStyle)
        }
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
