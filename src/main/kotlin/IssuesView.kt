@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.*
import org.ocpsoft.prettytime.PrettyTime
import java.awt.Desktop

val Repository = compositionLocalOf<IssueRepository> { error("Undefined repository") }

private val timePrinter = PrettyTime()

private val issueDateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)
private val attachmentTitleStyle = TextStyle(color = Color.LightGray, fontSize = 14.sp)
private val fieldTitleStyle = TextStyle(color = Color.LightGray, fontStyle = FontStyle.Italic, fontSize = 14.sp)
private val labelTitleStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
private val labelValueStyle = TextStyle(fontSize = 14.sp)


@ExperimentalMaterialApi
@Composable
fun IssuesView() {
    MaterialTheme(colors = darkColors(primary = Color(120, 120, 120), onPrimary = Color.White)) {
        Main()
    }
}

@ExperimentalMaterialApi
@Composable
fun Main() {
    val currentIssue: MutableState<IssueHead?> = remember { mutableStateOf(null) }
    val currentFilter: MutableState<Filter?> = remember { mutableStateOf(null) }
    val oldestCommentsFirst = remember { mutableStateOf(true) }
    val commentViewState = remember { mutableStateOf(CommentViewState.COMMENTS) }
    BoxWithConstraints {
        if (maxWidth.value > 1000) {
            TwoColumnsLayout(currentIssue, currentFilter, oldestCommentsFirst, commentViewState)
        } else {
            SingleColumnLayout(currentIssue, currentFilter, oldestCommentsFirst, commentViewState)
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun SingleColumnLayout(
    currentIssue: MutableState<IssueHead?>,
    currentFilter: MutableState<Filter?>,
    oldestCommentsFirst: MutableState<Boolean>,
    commentViewState: MutableState<CommentViewState>
) {
    val issue = currentIssue.value
    if (issue == null) {
        IssuesList(currentIssue, currentFilter)
    } else {
        Column {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(text = issue.key, style = MaterialTheme.typography.h5)
                        },
                        navigationIcon = {
                            Button(onClick = { currentIssue.value = null }) {
                                Text(text = "Back")
                            }
                        }
                    )
                },
                content = {
                    CurrentIssue(currentIssue.value, oldestCommentsFirst, commentViewState)
                }
            )
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun TwoColumnsLayout(
    currentIssue: MutableState<IssueHead?>,
    currentFilter: MutableState<Filter?>,
    oldestCommentsFirst: MutableState<Boolean>,
    commentViewState: MutableState<CommentViewState>
) {
    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth(0.25f), contentAlignment = Alignment.Center) {
            IssuesList(currentIssue, currentFilter)
        }
        CurrentIssue(currentIssue.value, oldestCommentsFirst, commentViewState)
    }
}

@ExperimentalMaterialApi
@Composable
fun CurrentIssue(issueHead: IssueHead?, oldestCommentsFirst: MutableState<Boolean>, commentViewState: MutableState<CommentViewState>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SelectionContainer {
                        Text(
                            text = if (issueHead != null) "${issueHead.key}: ${issueHead.fields.summary}" else "",
                            style = MaterialTheme.typography.h5,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        },
        content = {
            when (issueHead) {
                null -> CurrentIssueStatus { Text("Select issue") }
                else -> {
                    val repo = Repository.current
                    val issueBody = uiStateFrom(issueHead.key) { clb: (Result<Issue>) -> Unit -> repo.getIssue(issueHead.key, clb) }.value
                    when (issueBody) {
                        is UiState.Loading -> CurrentIssueStatus { Loader() }
                        is UiState.Error -> CurrentIssueStatus { Error("data.Issue loading error") }
                        is UiState.Success -> CurrentIssueActive(issueHead, issueBody.data, oldestCommentsFirst, commentViewState)
                    }
                }
            }
        }
    )
}

@Composable
fun CurrentIssueStatus(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@ExperimentalMaterialApi
@Composable
fun CurrentIssueActive(
    issueHead: IssueHead,
    issue: Issue,
    oldestCommentsFirst: MutableState<Boolean>,
    commentViewState: MutableState<CommentViewState>
) {
    val repo = Repository.current
    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(8.dp).fillMaxSize()) {
            Text(AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(fontSize = 14.sp))
                append("Erstellt ")
                append(timePrinter.format(issueHead.fields.created))
                issue.fields.reporter?.displayName?.let {
                    append(" von ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(it)
                    pop()
                }
                append("       Zuletzt geändert ")
                append(timePrinter.format(issueHead.fields.updated))
            }.toAnnotatedString())
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalGap = 3.dp, verticalGap = 3.dp) {
                Column(Modifier.width(220.dp)) {
                    Label("Assignee") {
                        Text(
                            text = issue.fields.assignee?.displayName ?: "",
                            style = labelValueStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 3.dp)
                        )
                    }
                    Label("Priorität") {
                        Text(
                            text = issue.fields.priority?.name ?: "",
                            style = labelValueStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 3.dp)
                        )
                    }
                }
                Column(Modifier.width(220.dp)) {
                    Label("Status") {
                        Text(
                            text = issue.fields.status?.name ?: "",
                            style = labelValueStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 3.dp)
                        )
                    }
                    Label("System") {
                        Text(
                            text = issue.fields.system?.value ?: "",
                            style = labelValueStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 3.dp)
                        )
                    }
                }
                Column(Modifier.width(220.dp)) {
                    Label("Art der Anfrage") {
                        Text(
                            text = issue.fields.issueType?.value ?: "",
                            style = labelValueStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 3.dp)
                        )
                    }
                    Label("Kategorie") {
                        Text(
                            text = issue.fields.category?.value ?: "",
                            style = labelValueStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 3.dp)
                        )
                    }
                }
                Column(Modifier.width(220.dp)) {
                    Label("Bereich") {
                        Text(
                            text = issue.fields.area?.value ?: "",
                            style = labelValueStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 3.dp)
                        )
                    }
                    Label("Dienstleister") {
                        Text(
                            text = issue.fields.dienstleister?.value ?: "",
                            style = labelValueStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 3.dp)
                        )
                    }
                }
                Column(Modifier.width(220.dp)) {
                    val watchers = uiStateFrom(issue.key) { clb: (Result<Watchers>) -> Unit -> repo.getWatchers(issue.key, clb) }.value
                    if (watchers is UiState.Success) {
                        val texts = watchers.data.watchers.map { it.displayName ?: "" }
                        Label("Watchers") {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Text(
                                    text = "${texts.size} watching",
                                    style = labelValueStyle,
                                    modifier = Modifier.padding(end = 3.dp).pointerMoveFilter(
                                        onEnter = {
                                            expanded = true
                                            false
                                        },
                                        onExit = {
                                            expanded = false
                                            false
                                        }
                                    ))
                                DropdownMenu(expanded = expanded, onDismissRequest = {}) {
                                    texts.forEach {
                                        Text(text = it, style = TextStyle(fontSize = 13.sp), modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }

                    val transitions = uiStateFrom(issue.key) { clb: (Result<Transitions>) -> Unit ->
                        repo.getTransitions(issue.key, clb)
                    }.value
                    if (transitions is UiState.Success) {
                        val trans = transitions.data.transitions
                        Label("Workflow") {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Button(
                                    onClick = { expanded = true },
                                    modifier = Modifier.height(18.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("...", fontSize = 12.sp)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    trans.forEach {
                                        Text(
                                            text = it.name,
                                            style = TextStyle(fontSize = 13.sp),
                                            modifier = Modifier.padding(4.dp).clickable {
                                                //TODO: call transition
                                            })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            IssueField("Anhänge") {
                FlowRow(horizontalGap = 3.dp, verticalGap = 3.dp) {
                    issue.fields.attachment?.forEach {
                        Card(onClick = {
                            repo.downloadAttachment(it.content) {
                                if (it is Result.Success) {
                                    Desktop.getDesktop().open(it.data)
                                }
                            }
                        }, border = BorderStroke(1.dp, Color.Gray)) {
                            Column(Modifier.padding(4.dp)) {
                                Text(
                                    text = it.filename,
                                    maxLines = 1,
                                    style = attachmentTitleStyle,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(160.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Text(
                                        text = it.mimeType,
                                        maxLines = 1,
                                        style = issueDateStyle,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(text = timePrinter.format(it.created), style = issueDateStyle)
                                }
                            }
                        }
                    }
                }
            }

            issue.fields.freitext?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Freitext") {
                    SelectionContainer(Modifier.border(1.dp, Color.LightGray).fillMaxWidth()) {
                        Text(text = it, modifier = Modifier.padding(3.dp))
                    }
                }
            }
            issue.fields.helpText?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("AP+ URL") {
                    SelectionContainer(Modifier.border(1.dp, Color.LightGray).fillMaxWidth()) {
                        Text(text = it, modifier = Modifier.padding(3.dp))
                    }
                }
            }
            issue.fields.description?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Beschreibung") {
                    SelectionContainer(Modifier.border(1.dp, Color.LightGray).fillMaxWidth()) {
                        Text(text = it, modifier = Modifier.padding(3.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            CommentsList(issue.datedItems, oldestCommentsFirst, commentViewState)
        }
    }
}

@Composable
fun IssueField(label: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expanded = !expanded }) {
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(18.dp)) {
                Icon(if (expanded) Icons.Filled.Close else Icons.Filled.Add, "")
            }
            Text(text = label, modifier = Modifier.padding(0.dp, 0.dp, 4.dp, 0.dp), style = fieldTitleStyle)
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
fun IssuesList(currentIssue: MutableState<IssueHead?>, currentFilter: MutableState<Filter?>) {
    Column {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        FilterDropdown(currentIssue, currentFilter)
                    }
                )
            },
            content = {
                ListBody(currentIssue, currentFilter)
            }
        )
    }
}

@Composable
fun CommentsList(
    items: List<DatedIssueItem>,
    oldestCommentsFirst: MutableState<Boolean>,
    commentViewState: MutableState<CommentViewState>
) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${items.count { it.isComment }} Kommentare",
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                style = fieldTitleStyle
            )
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = { oldestCommentsFirst.value = !oldestCommentsFirst.value },
                modifier = Modifier.height(20.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(if (oldestCommentsFirst.value) "Älteste zuerst" else "Neueste zuerst", fontSize = 12.sp)
            }
            Spacer(Modifier.width(20.dp))
            Button(
                onClick = { commentViewState.value = CommentViewState.ALL },
                modifier = Modifier.height(20.dp),
                contentPadding = PaddingValues(2.dp),
                border = if (commentViewState.value == CommentViewState.ALL) BorderStroke(1.dp, Color.White) else null
            ) {
                Text("Alle", fontSize = 12.sp)
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = { commentViewState.value = CommentViewState.COMMENTS },
                modifier = Modifier.height(20.dp),
                contentPadding = PaddingValues(2.dp),
                border = if (commentViewState.value == CommentViewState.COMMENTS) BorderStroke(1.dp, Color.White) else null
            ) {
                Text("Kommentare", fontSize = 12.sp)
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = { commentViewState.value = CommentViewState.HISTORY },
                modifier = Modifier.height(20.dp),
                contentPadding = PaddingValues(2.dp),
                border = if (commentViewState.value == CommentViewState.HISTORY) BorderStroke(1.dp, Color.White) else null
            ) {
                Text("Historie", fontSize = 12.sp)
            }
        }
        Divider(color = Color.DarkGray, thickness = 1.dp)
        Box(Modifier.fillMaxSize()) {
            val scroll = rememberScrollState()
            Box(Modifier.fillMaxSize().padding(end = 6.dp).verticalScroll(scroll)) {
                Column {
                    val sorted = if (oldestCommentsFirst.value) items.sortedBy { it.created } else items.sortedByDescending { it.created }
                    sorted.forEach {
                        if (it.isComment && commentViewState.value != CommentViewState.HISTORY) {
                            CommentItem(it.comment!!)
                        } else if (it.isHistory && commentViewState.value != CommentViewState.COMMENTS) {
                            HistoryItem(it.history!!)
                        }
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

@Composable
fun FilterDropdown(currentIssue: MutableState<IssueHead?>, currentFilter: MutableState<Filter?>) {
    var expanded by remember { mutableStateOf(false) }
    val repo = Repository.current
    val filters = uiStateFrom(currentFilter) { clb: (Result<List<Filter>>) -> Unit -> repo.getFilters(clb) }
    Box(Modifier.fillMaxWidth().wrapContentSize(Alignment.CenterStart).border(2.dp, Color.Gray)) {
        Text(currentFilter.value?.name ?: "Filter", modifier = Modifier.clickable(onClick = { expanded = true }).padding(6.dp))
        DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.requiredSizeIn(maxHeight = 600.dp)) {
            filters.value.let { state ->
                when (state) {
                    is UiState.Success -> {
                        state.data.sortedBy { it.id.toInt() }.forEach { filter ->
                            Text(text = filter.name, modifier = Modifier.fillMaxWidth().padding(2.dp).clickable {
                                currentFilter.value = filter
                                currentIssue.value = null
                                expanded = false
                            })
                            if (filter.id == "-1") {
                                Divider()
                            }
                        }
                    }
                    is UiState.Loading -> Loader()
                    is UiState.Error -> Error(state.exception)
                }
            }
        }
    }

    // default filter on startup
    if (currentFilter.value == null && filters.component1() is UiState.Success) {
        currentFilter.value = (filters.component1() as UiState.Success<List<Filter>>).data.first { it.id == "-2" }
    }
}

@Composable
fun ListBody(currentIssue: MutableState<IssueHead?>, currentFilter: MutableState<Filter?>) {
    val repo = Repository.current
    val issues = uiStateFrom(currentFilter.value?.jql) { clb: (Result<SearchResult>) -> Unit ->
        repo.getIssues(currentFilter.value?.jql, clb)
    }

    Box(Modifier.fillMaxSize()) {
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Column {
                issues.value.let {
                    when (it) {
                        is UiState.Success -> {
                            it.data.issues.forEach { iss ->
                                Box(Modifier.clickable { currentIssue.value = iss }, contentAlignment = Alignment.CenterStart) {
                                    ListItem(iss)
                                }
                            }
                        }
                        is UiState.Loading -> Loader()
                        is UiState.Error -> Error(it.exception)
                    }
                }
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
fun ListItem(issueHead: IssueHead) {
    Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(54, 54, 54)) {
        Column(Modifier.fillMaxSize().padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(text = issueHead.key, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(text = "Erstellt: " + timePrinter.format(issueHead.fields.created), style = issueDateStyle)
                    Text(text = "Geändert: " + timePrinter.format(issueHead.fields.updated), style = issueDateStyle)
                }
            }
            Text(issueHead.fields.summary ?: "")
            Row {
                issueHead.fields.status?.name?.let {
                    Text(it, modifier = Modifier.padding(2.dp))
                }
                issueHead.fields.priority?.name?.let {
                    Spacer(Modifier.width(5.dp))
                    Text(it, modifier = Modifier.padding(2.dp))
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(40, 40, 40), border = BorderStroke(1.dp, Color.Gray)) {
        Column(Modifier.fillMaxWidth().padding(2.dp)) {
            Text(AnnotatedString.Builder().apply {
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
            }.toAnnotatedString())
            Divider(color = Color.Gray, thickness = 1.dp)
            SelectionContainer(Modifier.fillMaxWidth()) {
                Text(text = comment.body, modifier = Modifier.padding(3.dp))
            }
        }
    }
}

@Composable
fun HistoryItem(history: History) {
    Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(40, 40, 40), border = BorderStroke(1.dp, Color.Gray)) {
        Column(Modifier.fillMaxWidth().padding(2.dp)) {
            Text(AnnotatedString.Builder().apply {
                append("Änderung")
                history.author.displayName?.let {
                    append(" von ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(it)
                    pop()
                }
                append(" ")
                append(timePrinter.format(history.created))
            }.toAnnotatedString())
            Divider(color = Color.Gray, thickness = 1.dp)
            history.items.forEach {
                Row(Modifier.fillMaxWidth()) {
                    var fromHover by remember { mutableStateOf(false) }
                    var toHover by remember { mutableStateOf(false) }
                    Text(
                        text = it.field,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(200.dp)
                    )
                    Text(
                        text = it.fromString ?: "",
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        maxLines = if (fromHover) Int.MAX_VALUE else 1,
                        overflow = if (fromHover) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.3F).pointerMoveFilter(
                            onEnter = {
                                fromHover = true
                                false
                            },
                            onExit = {
                                fromHover = false
                                false
                            }
                        )
                    )
                    Text("  ->  ")
                    Text(
                        text = it.toString ?: "",
                        fontFamily = FontFamily.Monospace,
                        maxLines = if (toHover) Int.MAX_VALUE else 1,
                        overflow = if (toHover) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().pointerMoveFilter(
                            onEnter = {
                                toHover = true
                                false
                            },
                            onExit = {
                                toHover = false
                                false
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun Label(label: String, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(3.dp).border(1.dp, Color.Gray), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$label:", style = labelTitleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 3.dp))
        content()
    }
}

@Composable
fun Loader() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        CircularProgressIndicator()
    }
}

@Composable
fun Error(err: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(text = err, style = TextStyle(color = MaterialTheme.colors.error, fontWeight = FontWeight.Bold))
    }
}

enum class CommentViewState {
    COMMENTS, HISTORY, ALL
}