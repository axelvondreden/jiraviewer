package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.api.Issue
import data.api.Result

@ExperimentalComposeUiApi
@Composable
fun CommentEditor(issue: MutableState<Issue>) {
    Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(40, 40, 40), border = BorderStroke(1.dp, Color.Gray)) {
        Column(Modifier.fillMaxWidth().padding(2.dp)) {
            val open = remember { mutableStateOf(false) }
            if (!open.value) {
                Button(onClick = { open.value = true }, modifier = Modifier.height(22.dp), contentPadding = PaddingValues(2.dp)) {
                    Text("Kommentieren", fontSize = 13.sp)
                }
            } else {
                CommentEditorActivated(issue, open)
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun CommentEditorActivated(issue: MutableState<Issue>, open: MutableState<Boolean>) {
    val textState = remember { mutableStateOf(TextFieldValue()) }
    CommentEditorFunctionRow(textState)
    TextField(
        value = textState.value,
        onValueChange = { textState.value = it },
        modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { shortcuts(it, textState) }
    )
    CommentEditorSubmitRow(issue, textState, open)
}

@ExperimentalComposeUiApi
fun shortcuts(event: KeyEvent, textState: MutableState<TextFieldValue>): Boolean {
    if (event.type != KeyEventType.KeyUp) return false
    if (!event.isCtrlPressed || event.isAltPressed || event.isShiftPressed || event.isMetaPressed) return false
    when (event.key) {
        Key.B -> textState.updateText("*")
        Key.I -> textState.updateText("_")
        Key.U -> textState.updateText("+")
    }
    return true
}

@Composable
fun CommentEditorFunctionRow(textState: MutableState<TextFieldValue>) {
    Row(Modifier.fillMaxWidth()) {
        IconButton(onClick = { textState.updateText("*") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.FormatBold, "Bold")
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { textState.updateText("_") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.FormatItalic, "Italic")
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { textState.updateText("+") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.FormatUnderlined, "Underline")
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { textState.updateText("-") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.FormatStrikethrough, "Strikethrough")
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { textState.updateText("??") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.FormatQuote, "Citation")
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { textState.addLine("*") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.FormatListBulleted, "Bulletlist")
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { textState.addLine("#") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.FormatListNumbered, "Numberedlist")
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { textState.addLine("||Head 1||Head 2||\n|Col 1|Col 2|") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.TableRows, "Table")
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { textState.addLine("{code:java}\nString potato;\n{code}") }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.Code, "Code")
        }
    }
}

@Composable
fun CommentEditorSubmitRow(issue: MutableState<Issue>, textState: MutableState<TextFieldValue>, open: MutableState<Boolean>) {
    Row(Modifier.fillMaxWidth()) {
        val repo = Repository.current
        Button(
            onClick = {
                if (textState.value.text.isNotBlank()) {
                    repo.addComment(issue.value.key, textState.value.text, false) { res ->
                        if (res is Result.Success) {
                            repo.getIssue(issue.value.key) {
                                if (it is Result.Success) {
                                    issue.value = it.data
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.height(24.dp),
            contentPadding = PaddingValues(2.dp)
        ) {
            Text("Kommentieren", fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = {
                if (textState.value.text.isNotBlank()) {
                    repo.addComment(issue.value.key, textState.value.text, true) { res ->
                        if (res is Result.Success) {
                            repo.getIssue(issue.value.key) {
                                if (it is Result.Success) {
                                    issue.value = it.data
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.height(24.dp),
            contentPadding = PaddingValues(2.dp)
        ) {
            Text("intern Kommentieren", fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Button(onClick = { open.value = false }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(2.dp)) {
            Text("Abbrechen", fontSize = 13.sp)
        }
    }
}

private fun MutableState<TextFieldValue>.updateText(selected: String) {
    var newSel = value.selection
    val new = if (value.selection.collapsed) {
        val t = value.text.take(newSel.min) + selected + selected + value.text.drop(newSel.min)
        newSel = TextRange(newSel.min + selected.length, newSel.min + selected.length)
        t
    } else {
        val range = newSel.min until newSel.max
        newSel = TextRange(newSel.min + selected.length, newSel.max + selected.length)
        value.text.replaceRange(range, selected + value.text.substring(range) + selected)
    }
    value = value.copy(text = new, selection = newSel)
}

private fun MutableState<TextFieldValue>.addLine(line: String) {
    value = value.copy(text = value.text + "\n$line")
}
