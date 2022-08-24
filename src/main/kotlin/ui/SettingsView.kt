package ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.local.Settings
import data.local.Settings.Companion.settings
import ui.utils.FlowRow
import java.time.Duration

private val labelStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)
private val subLabelStyle = TextStyle(fontSize = 14.sp)

@ExperimentalComposeUiApi
@Composable
fun SettingsView() = Scaffold {
    Box(Modifier.fillMaxSize()) {
        val scroll = rememberScrollState()

        Box(Modifier.fillMaxSize().verticalScroll(scroll), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.width(600.dp)) {
                SettingsRow("Comment order") {
                    Button(onClick = { settings.commentAscending = !settings.commentAscending }, modifier = Modifier.padding(5.dp).fillMaxWidth()) {
                        Text(if (settings.commentAscending) "Oldest first" else "Newest first")
                    }
                }
                SettingsRow("Projects") {
                    Column(modifier = Modifier.fillMaxWidth().padding(5.dp)) {
                        var text by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth().onKeyEvent {
                                if (it.key == Key.Enter && text.isNotBlank()) {
                                    settings.projects += text
                                    text = ""
                                }
                                false
                            },
                            label = { Text("Project") },
                            trailingIcon = {
                                IconButton(onClick = { settings.projects += text; text = "" }, enabled = text.isNotBlank()) {
                                    Icon(Icons.Default.Add, "add project")
                                }
                            },
                            singleLine = true
                        )
                        FlowRow(horizontalGap = 3.dp, verticalGap = 5.dp) {
                            settings.projects.forEach { project ->
                                Chip(title = project, onDelete = {
                                    if (settings.projects.size > 1) {
                                        settings.projects -= it
                                    }
                                })
                            }
                        }
                    }
                }
                SettingsRow("Background Updates") {
                    Column {
                        var expanded by remember { mutableStateOf(false) }

                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(text = "Strategy", style = subLabelStyle, modifier = Modifier.padding(top = 10.dp, start = 5.dp, bottom = 3.dp))
                                Button(onClick = { expanded = true }, modifier = Modifier.padding(horizontal = 5.dp).fillMaxWidth()) {
                                    Text(settings.updateStrategy.name)
                                    Icon(Icons.Default.ArrowDropDown, "pick update strategy")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.width(this@BoxWithConstraints.maxWidth).requiredSizeIn(maxHeight = 600.dp)
                                ) {
                                    Settings.UpdateStrategy.values().forEach { updateStrategy ->
                                        Column(Modifier.fillMaxWidth().border(1.dp, Color.Gray).clickable { settings.updateStrategy = updateStrategy; expanded = false }) {
                                            Text(updateStrategy.title, Modifier.fillMaxWidth().padding(2.dp))
                                            Text(updateStrategy.description, Modifier.fillMaxWidth().padding(2.dp), color = Color.LightGray, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Text(text = "Interval", style = subLabelStyle, modifier = Modifier.padding(top = 10.dp, start = 5.dp, bottom = 3.dp))
                        Row(modifier = Modifier.padding(horizontal = 5.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            var interval by remember { mutableStateOf(settings.updateInterval.toFloat()) }
                            Slider(
                                value = interval,
                                onValueChange = { interval = it },
                                modifier = Modifier.weight(1F),
                                enabled = settings.updateStrategy != Settings.UpdateStrategy.NONE,
                                valueRange = 10F..600F,
                                steps = 590,
                                onValueChangeFinished = { settings.updateInterval = interval.toInt() }
                            )
                            Text(text = "${interval.toInt()}s", modifier = Modifier.width(40.dp))
                        }
                        Text(text = "Offset (How far to check back in time after startup)", style = subLabelStyle, modifier = Modifier.padding(top = 10.dp, start = 5.dp, bottom = 3.dp))
                        Row(modifier = Modifier.padding(horizontal = 5.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            var offset by remember { mutableStateOf(settings.updateOffset.toFloat()) }
                            Slider(
                                value = offset,
                                onValueChange = { offset = it },
                                modifier = Modifier.weight(1F),
                                enabled = settings.updateStrategy != Settings.UpdateStrategy.NONE,
                                valueRange = 0F..24F,
                                steps = 24,
                                onValueChangeFinished = { settings.updateOffset = offset.toInt() }
                            )
                            Text(text = "${offset.toInt()}h", modifier = Modifier.width(40.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Show notifications for my own actions", style = subLabelStyle, modifier = Modifier.padding(end = 5.dp))
                            Checkbox(
                                checked = settings.updateIncludeOwn,
                                onCheckedChange = { settings.updateIncludeOwn = it },
                                enabled = settings.updateStrategy != Settings.UpdateStrategy.NONE
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(label: String, content: @Composable RowScope.() -> Unit) =
    Row(modifier = Modifier.padding(10.dp).fillMaxWidth().border(width = 1.dp, color = MaterialTheme.colors.onPrimary)) {
        Text(text = label, style = labelStyle, modifier = Modifier.padding(10.dp).width(200.dp))
        content()
    }

@Composable
private fun Chip(title: String, onDelete: (String) -> Unit = {}) =
    Surface(modifier = Modifier.padding(vertical = 4.dp), elevation = 8.dp, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colors.primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.body2, modifier = Modifier.padding(8.dp))
            IconButton(onClick = { onDelete(title) }) {
                Icon(Icons.Default.Delete, "delete project")
            }
        }
    }