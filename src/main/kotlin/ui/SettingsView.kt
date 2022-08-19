package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.local.Settings.Companion.settings

private val settingsLabelStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)

@Composable
fun SettingsView() {
    Scaffold {
        Box(Modifier.fillMaxSize()) {
            val scroll = rememberScrollState()
            Box(Modifier.fillMaxSize().verticalScroll(scroll), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.width(600.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Comment order", style = settingsLabelStyle, modifier = Modifier.width(200.dp))
                        Button(onClick = { settings.commentAscending = !settings.commentAscending }, modifier = Modifier.width(200.dp)) {
                            Text(if (settings.commentAscending) "Oldest first" else "Newest first")
                        }
                    }
                }
            }
        }
    }
}