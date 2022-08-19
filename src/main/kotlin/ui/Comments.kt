package ui

import ErrorText
import Loader
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.api.*
import data.local.Settings
import java.io.File

private val fieldTitleStyle = TextStyle(color = Color.LightGray, fontStyle = FontStyle.Italic, fontSize = 14.sp)

@ExperimentalComposeUiApi
@Composable
fun CommentsList(issue: MutableState<Issue>) {
    val repo = Repository.current
    Settings.withSettings { settings ->
        when (val comments = uiStateFrom(issue.value) { clb: (Result<Comments>) -> Unit -> repo.getComments(issue.value.key, clb) }.value) {
            is UiState.Error -> ErrorText(comments.exception)
            is UiState.Loading -> Loader()
            is UiState.Success -> {
                val cList = comments.data.comments.map { DatedIssueItem(comment = it) }
                val hList = issue.value.changelog?.histories?.map { DatedIssueItem(history = it) } ?: emptyList()
                val items = cList.plus(hList)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            modifier = Modifier.height(24.dp),
                            title = { CommentListHeader(settings, items) },
                        )
                    },
                    content = {
                        Box(Modifier.fillMaxSize()) {
                            val scroll = rememberScrollState()
                            Box(Modifier.fillMaxSize().padding(end = 6.dp).verticalScroll(scroll)) {
                                Column {
                                    if (!settings.commentAscending) {
                                        CommentEditor(issue)
                                    }
                                    val sorted = if (settings.commentAscending) items.sortedBy { it.created } else items.sortedByDescending { it.created }
                                    sorted.forEach {
                                        if (it.isComment && settings.commentView != Settings.CommentViewFilter.HISTORY) {
                                            CommentItem(it.comment!!, issue.value.fields.attachment)
                                        } else if (it.isHistory && settings.commentView != Settings.CommentViewFilter.COMMENTS) {
                                            HistoryItem(it.history!!)
                                        }
                                    }
                                    if (settings.commentAscending) {
                                        CommentEditor(issue)
                                    }
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(scroll),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CommentListHeader(settings: Settings, items: List<DatedIssueItem>) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${items.count { it.isComment }} Kommentare", Modifier.padding(top = 4.dp, bottom = 2.dp), style = fieldTitleStyle)
        Spacer(Modifier.width(6.dp))
        Button(
            onClick = { settings.commentAscending = !settings.commentAscending },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(2.dp)
        ) {
            Text(if (settings.commentAscending) "Älteste zuerst" else "Neueste zuerst", fontSize = 12.sp)
        }
        Spacer(Modifier.width(20.dp))
        CommentFilterButton(settings, Settings.CommentViewFilter.ALL, "Alle")
        Spacer(Modifier.width(4.dp))
        CommentFilterButton(settings, Settings.CommentViewFilter.COMMENTS, "Kommentare")
        Spacer(Modifier.width(4.dp))
        CommentFilterButton(settings, Settings.CommentViewFilter.HISTORY, "Historie")
    }
}

@Composable
private fun CommentFilterButton(settings: Settings, filter: Settings.CommentViewFilter, text: String) {
    Button(
        onClick = { settings.commentView = filter },
        modifier = Modifier.height(20.dp),
        contentPadding = PaddingValues(2.dp),
        border = if (settings.commentView == filter) BorderStroke(1.dp, Color.White) else null
    ) {
        Text(text, fontSize = 12.sp)
    }
}


@Composable
private fun CommentItem(comment: Comment, attachments: List<Attachment>?) {
    val repo = Repository.current
    Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(40, 40, 40), border = BorderStroke(1.dp, Color.Gray)) {
        Column(Modifier.fillMaxWidth().padding(2.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = AnnotatedString.Builder().apply {
                        append("Kommentar")
                        comment.author.displayName?.let {
                            append(" von ")
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                            append(it)
                            pop()
                        }
                        append(" ")
                        append(timePrinter.format(comment.created))
                        comment.updateAuthor?.displayName?.let {
                            append("       Geändert")
                            append(" von ")
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                            append(it)
                            pop()
                            append(" ")
                            append(timePrinter.format(comment.updated))
                        }
                    }.toAnnotatedString(),
                    color = Color.Gray
                )
                comment.properties.firstOrNull { it.key == "sd.public.comment" }?.let {
                    if (it.value["internal"] == true) {
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.border(1.dp, Color.Gray)) {
                            Text("intern", fontSize = 13.sp, modifier = Modifier.padding(3.dp))
                        }
                    }
                }
            }
            Divider(color = Color.Gray, thickness = 1.dp)
            ParsedText(comment.body, Modifier.fillMaxWidth().padding(3.dp), 16.sp)
            val references = comment.body.getAttachments()
            if (references.isNotEmpty()) {
                Divider(color = Color.Gray, thickness = 1.dp)
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    references.forEach { ref ->
                        attachments?.firstOrNull { it.filename == ref }?.let {
                            item {
                                when (val att = uiStateFrom(it.content) { clb: (Result<File>) -> Unit -> repo.download(it.content, clb) }.value) {
                                    is UiState.Loading -> CircularProgressIndicator()
                                    is UiState.Error -> Text(att.exception)
                                    is UiState.Success -> ClickableImage(att.data)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.getAttachments() = Regex("!.+\\..+\\|?.*!").findAll(this)
    .map { result -> result.value.drop(1).takeWhile { it != '!' && it != '|' } }.toList()