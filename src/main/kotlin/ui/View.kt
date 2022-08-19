package ui

import ErrorText
import Loader
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import data.api.*
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import org.ocpsoft.prettytime.PrettyTime
import ui.splitter.SplitterState
import ui.splitter.VerticalSplittable
import java.awt.Desktop
import java.io.File
import java.net.URI

val Repository = compositionLocalOf<JiraRepository> { error("Undefined repository") }

private val timePrinter = PrettyTime()

private val issueDateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)
private val attachmentTitleStyle = TextStyle(color = Color.LightGray, fontSize = 14.sp)
private val fieldTitleStyle = TextStyle(color = Color.LightGray, fontStyle = FontStyle.Italic, fontSize = 14.sp)
private val labelTitleStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
private val labelValueStyle = TextStyle(fontSize = 14.sp)
private val scrollbarStyle = ScrollbarStyle(
    minimalHeight = 16.dp,
    thickness = 8.dp,
    shape = RectangleShape,
    hoverDurationMillis = 0,
    unhoverColor = Color.White.copy(alpha = 0.12f),
    hoverColor = Color.White.copy(alpha = 0.12f)
)


@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun IssuesView() {
    val openedIssues: SnapshotStateList<IssueHead> = remember { mutableStateListOf() }
    val currentIssue: MutableState<IssueHead?> = remember { mutableStateOf(null) }
    val currentFilter: MutableState<Filter?> = remember { mutableStateOf(null) }
    val commentState = remember { mutableStateOf(CommentState(CommentFilter.COMMENTS, true)) }
    TwoColLayout(openedIssues, currentIssue, currentFilter, commentState)
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun TwoColLayout(
    openedIssues: SnapshotStateList<IssueHead>,
    issueState: MutableState<IssueHead?>,
    filterState: MutableState<Filter?>,
    commentState: MutableState<CommentState>
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val splitter = SplitterState()
        var width by mutableStateOf(maxWidth * 0.25F)
        val range = 150.dp..(maxWidth * 0.5F)
        VerticalSplittable(Modifier.fillMaxSize(), splitter, onResize = { width = (width + it).coerceIn(range) }) {
            Box(modifier = Modifier.width(width), contentAlignment = Alignment.Center) {
                IssuesList(openedIssues, issueState, filterState)
            }
            OpenedIssues(openedIssues, issueState, commentState, false)
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun OpenedIssues(
    openedIssues: SnapshotStateList<IssueHead>,
    issueState: MutableState<IssueHead?>,
    commentState: MutableState<CommentState>,
    backButton: Boolean
) {
    Column {
        if (openedIssues.isNotEmpty()) {
            val index = openedIssues.indexOf(issueState.value).coerceAtLeast(0)
            ScrollableTabRow(selectedTabIndex = index, modifier = Modifier.fillMaxWidth(), edgePadding = 10.dp) {
                openedIssues.forEachIndexed { i, issueHead ->
                    Tab(selected = i == index, onClick = { issueState.value = issueHead }, modifier = Modifier.height(28.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(issueHead.key)
                            IconButton(onClick = {
                                // close the current tab and open the one to the right
                                val oldIndex = openedIssues.indexOf(issueHead)
                                openedIssues.remove(issueHead)
                                issueState.value = openedIssues.getOrNull(oldIndex.coerceIn(openedIssues.indices))
                            }) {
                                Icon(Icons.Default.Close, "close")
                            }
                        }
                    }
                }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        SelectionContainer {
                            Text(
                                text = if (issueState.value != null) "${issueState.value!!.key}: ${issueState.value!!.fields.summary}" else "",
                                style = MaterialTheme.typography.h5,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = if (backButton) {
                        { Button(onClick = { issueState.value = null }) { Text(text = "Back") } }
                    } else null
                )
            },
            content = { CurrentIssueContent(issueState.value, commentState) }
        )
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun CurrentIssueContent(head: IssueHead?, commentState: MutableState<CommentState>) {
    if (head == null) CurrentIssueStatus { Text("Select issue") }
    else {
        val repo = Repository.current
        when (val issue = uiStateFrom(head.key) { clb: (Result<Issue>) -> Unit -> repo.getIssue(head.key, clb) }.value) {
            is UiState.Loading -> CurrentIssueStatus { Loader() }
            is UiState.Error -> CurrentIssueStatus { ErrorText("data.api.Issue loading error") }
            is UiState.Success -> CurrentIssueActiveContainer(issue.data, commentState)
        }
    }
}

@Composable
fun CurrentIssueStatus(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun CurrentIssueActiveContainer(issue: Issue, commentState: MutableState<CommentState>) {
    val issueState = remember { mutableStateOf(issue) }
    val repo = Repository.current
    when (val editRes = uiStateFrom(issueState.value.key) { clb: (Result<Editmeta>) -> Unit -> repo.getEditmeta(issueState.value.key, clb) }.value) {
        is UiState.Error -> CurrentIssueStatus { ErrorText(editRes.exception) }
        is UiState.Loading -> CurrentIssueStatus { Loader() }
        is UiState.Success -> CurrentIssueActive(issueState, commentState, editRes.data.fields)
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun CurrentIssueActive(issueState: MutableState<Issue>, commentState: MutableState<CommentState>, editmeta: Map<String, EditMetaField>) {
    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(8.dp).fillMaxSize()) {
            IssueHeaderInfo(issueState)
            Spacer(Modifier.height(6.dp))
            IssueFields(issueState, editmeta)
            issueState.value.fields.attachment.takeIf { !it.isNullOrEmpty() }?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Anhänge") {
                    FlowRow(horizontalGap = 3.dp, verticalGap = 3.dp) {
                        val repo = Repository.current
                        it.forEach { attachment ->
                            AttachmentCard(attachment) {
                                repo.download(attachment.content) {
                                    if (it is Result.Success) Desktop.getDesktop().open(it.data)
                                }
                            }
                        }
                    }
                }
            }
            issueState.value.fields.freitext?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Freitext") {
                    ParsedText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
                }
            }
            issueState.value.fields.fehlermeldung?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Fehlermeldung") {
                    ParsedText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
                }
            }
            issueState.value.fields.helpText?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("AP+ URL") {
                    ParsedText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
                }
            }
            issueState.value.fields.helpText2?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("AP+ URL") {
                    ParsedText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
                }
            }
            issueState.value.fields.description?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Beschreibung") {
                    ParsedText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp), 16.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            CommentsList(issueState, commentState)
        }
    }
}

@Composable
fun ParsedText(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 14.sp) {
    val repo = Repository.current
    val aText = text.parseJiraText(fontSize, repo)
    SelectionContainer {
        ClickableText(text = aText, modifier = modifier, onClick = { offset ->
            aText.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let {
                Desktop.getDesktop().browse(URI(it.item))
            }
        })
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun AttachmentCard(attachment: Attachment, down: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.pointerMoveFilter(onEnter = { expanded = true; false }, onExit = { expanded = false; false }),
        border = BorderStroke(1.dp, Color.Gray),
        onClick = down
    ) {
        Column(Modifier.padding(4.dp)) {
            Text(
                text = attachment.filename,
                maxLines = 1,
                style = attachmentTitleStyle,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = if (expanded) Modifier.widthIn(min = 160.dp) else Modifier.width(160.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    text = attachment.mimeType,
                    maxLines = 1,
                    style = issueDateStyle,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = if (expanded) Modifier.widthIn(min = 120.dp) else Modifier.width(120.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(text = timePrinter.format(attachment.created), style = issueDateStyle)
            }
        }
    }
}

@Composable
fun IssueHeaderInfo(issueState: MutableState<Issue>) {
    FlowRow(horizontalGap = 6.dp, verticalGap = 3.dp) {
        Text(AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(fontSize = 13.sp))
            append("Erstellt: ")
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(timePrinter.format(issueState.value.fields.created))
            issueState.value.fields.reporter?.displayName?.let {
                append(" von ")
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(it)
                pop()
            }
        }.toAnnotatedString())
        issueState.value.fields.updated?.let {
            Text(AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(fontSize = 13.sp))
                append("| Geändert: ")
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(timePrinter.format(it))
            }.toAnnotatedString())
        }
        issueState.value.fields.workaround?.value?.let {
            Text(AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(fontSize = 13.sp))
                append("| Workaround: ")
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(it)
            }.toAnnotatedString())
        }
        issueState.value.fields.reproducable?.value?.let {
            Text(AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(fontSize = 13.sp))
                append("| Nachstellbarkeit: ")
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(it)
            }.toAnnotatedString())
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun IssueFields(issueState: MutableState<Issue>, editmeta: Map<String, EditMetaField>) {
    FlowRow(horizontalGap = 3.dp, verticalGap = 3.dp) {
        Column(Modifier.width(220.dp)) {
            AssigneeField(issueState)
            PriorityField(issueState, editmeta["priority"])
        }
        Column(Modifier.width(220.dp)) {
            StatusField(issueState)
            SystemField(issueState, editmeta["customfield_14304"])
        }
        Column(Modifier.width(220.dp)) {
            TypeField(issueState, editmeta["customfield_14302"])
            CategoryField(issueState, editmeta["customfield_13902"])
        }
        Column(Modifier.width(220.dp)) {
            AreaField(issueState, editmeta["customfield_14303"])
            WorkflowField(issueState)
        }
        Column(Modifier.width(220.dp)) {
            RequestedParticipantsField(issueState)
            WatchersField(issueState)
        }
    }
}

@Composable
fun AssigneeField(issue: MutableState<Issue>) {
    Label("Assignee") {
        Text(
            text = issue.value.fields.assignee?.displayName ?: "",
            style = labelValueStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 3.dp)
        )
    }
}

@Composable
fun PriorityField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("Priorität") {
        Row {
            var edit by remember { mutableStateOf(false) }
            Box {
                Text(
                    text = issue.value.fields.priority?.name ?: "",
                    style = labelValueStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 3.dp)
                )
                if (editmeta != null) {
                    val repo = Repository.current
                    DropdownMenu(expanded = edit, onDismissRequest = { edit = false }) {
                        editmeta.allowedValues?.forEach { allowedValue ->
                            Text(
                                text = allowedValue.name ?: "-",
                                style = TextStyle(fontSize = 13.sp),
                                modifier = Modifier.padding(4.dp).clickable {
                                    val upd = Update(mapOf("priority" to listOf(mapOf("set" to mapOf("name" to allowedValue.name!!)))))
                                    updateIssue(repo, issue, upd)
                                    edit = false
                                }
                            )
                        }
                    }
                }
            }
            if (editmeta != null) {
                IconButton(onClick = { edit = true }, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Filled.Edit, "Edit")
                }
            }
        }
    }
}

@Composable
fun StatusField(issue: MutableState<Issue>) {
    Label("Status") {
        Text(
            text = issue.value.fields.status?.name ?: "",
            style = labelValueStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 3.dp)
        )
    }
}

@Composable
fun SystemField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("System") {
        Row {
            var edit by remember { mutableStateOf(false) }
            Box {
                Text(
                    text = issue.value.fields.system?.value ?: "",
                    style = labelValueStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 3.dp)
                )
                if (editmeta != null) {
                    val repo = Repository.current
                    DropdownMenu(expanded = edit, onDismissRequest = { edit = false }) {
                        editmeta.allowedValues?.forEach {
                            Text(
                                text = it.value ?: "-",
                                style = TextStyle(fontSize = 13.sp),
                                modifier = Modifier.padding(4.dp).clickable {
                                    val upd = Update(mapOf("customfield_14304" to listOf(mapOf("set" to mapOf("value" to it.value!!)))))
                                    updateIssue(repo, issue, upd)
                                    edit = false
                                })
                        }
                    }
                }
            }
            if (editmeta != null) {
                IconButton(onClick = { edit = true }, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Filled.Edit, "Edit")
                }
            }
        }
    }
}

private fun updateIssue(repo: JiraRepository, issue: MutableState<Issue>, update: Update) {
    repo.updateIssue(issue.value.key, update) { result ->
        when (result) {
            is Result.Error -> println(result.exception)
            is Result.Success -> {
                repo.getIssue(issue.value.key) {
                    when (it) {
                        is Result.Error -> println(it.exception)
                        is Result.Success -> issue.value = it.data
                    }
                }
            }
        }
    }
}

@Composable
fun TypeField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("Art") {
        Row {
            var edit by remember { mutableStateOf(false) }
            Box {
                Text(
                    text = issue.value.fields.issueType?.value ?: "",
                    style = labelValueStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 3.dp)
                )
                if (editmeta != null) {
                    val repo = Repository.current
                    DropdownMenu(expanded = edit, onDismissRequest = { edit = false }) {
                        editmeta.allowedValues?.forEach {
                            Text(
                                text = it.value ?: "-",
                                style = TextStyle(fontSize = 13.sp),
                                modifier = Modifier.padding(4.dp).clickable {
                                    val upd = Update(mapOf("customfield_14302" to listOf(mapOf("set" to mapOf("value" to it.value!!)))))
                                    updateIssue(repo, issue, upd)
                                    edit = false
                                })
                        }
                    }
                }
            }
            if (editmeta != null) {
                IconButton(onClick = { edit = true }, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Filled.Edit, "Edit")
                }
            }
        }
    }
}

@Composable
fun CategoryField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("Kategorie") {
        Row {
            var edit by remember { mutableStateOf(false) }
            Box {
                Text(
                    text = issue.value.fields.category?.value ?: "",
                    style = labelValueStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 3.dp)
                )
                if (editmeta != null) {
                    val repo = Repository.current
                    DropdownMenu(
                        expanded = edit,
                        onDismissRequest = { edit = false },
                        modifier = Modifier.requiredSizeIn(maxHeight = 600.dp)
                    ) {
                        editmeta.allowedValues?.forEach {
                            Text(
                                text = it.value ?: "-",
                                style = TextStyle(fontSize = 13.sp),
                                modifier = Modifier.padding(4.dp).clickable {
                                    val upd = Update(mapOf("customfield_13902" to listOf(mapOf("set" to mapOf("value" to it.value!!)))))
                                    updateIssue(repo, issue, upd)
                                    edit = false
                                })
                        }
                    }
                }
            }
            if (editmeta != null) {
                IconButton(onClick = { edit = true }, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Filled.Edit, "Edit")
                }
            }
        }
    }
}

@Composable
fun AreaField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("Bereich") {
        Row {
            var edit by remember { mutableStateOf(false) }
            Box {
                Text(
                    text = issue.value.fields.area?.value ?: "",
                    style = labelValueStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 3.dp)
                )
                if (editmeta != null) {
                    val repo = Repository.current
                    DropdownMenu(expanded = edit, onDismissRequest = { edit = false }) {
                        editmeta.allowedValues?.forEach {
                            Text(
                                text = it.value ?: "-",
                                style = TextStyle(fontSize = 13.sp),
                                modifier = Modifier.padding(4.dp).clickable {
                                    val upd = Update(mapOf("customfield_14303" to listOf(mapOf("set" to mapOf("value" to it.value!!)))))
                                    updateIssue(repo, issue, upd)
                                    edit = false
                                })
                        }
                    }
                }
            }
            if (editmeta != null) {
                IconButton(onClick = { edit = true }, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Filled.Edit, "Edit")
                }
            }
        }
    }
}

@Composable
fun WorkflowField(issue: MutableState<Issue>) {
    val repo = Repository.current
    val transitions =
        uiStateFrom(issue.value) { clb: (Result<Transitions>) -> Unit -> repo.getTransitions(issue.value.key, clb) }.value
    if (transitions is UiState.Success) {
        val dialogItem: MutableState<Transition?> = remember { mutableStateOf(null) }
        val trans = transitions.data.transitions
        Label("Workflow") {
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }, modifier = Modifier.height(18.dp), contentPadding = PaddingValues(2.dp)) {
                    Text("...", fontSize = 12.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    trans.forEach { transition ->
                        Text(text = transition.name, style = TextStyle(fontSize = 13.sp), modifier = Modifier.padding(4.dp).clickable {
                            if (!transition.fields.isNullOrEmpty()) {
                                dialogItem.value = transition
                            } else {
                                repo.doTransition(issue.value.key, Update(transition = TransitionTo(transition.id))) { res ->
                                    when (res) {
                                        is Result.Error -> println(res.exception)
                                        is Result.Success -> {
                                            repo.getIssue(issue.value.key) {
                                                when (it) {
                                                    is Result.Error -> println(it.exception)
                                                    is Result.Success -> issue.value = it.data
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            expanded = false
                        })
                    }
                }
            }
        }
        if (dialogItem.value != null) {
            Dialog(onCloseRequest = { dialogItem.value = null }, title = dialogItem.value!!.name) {
                Column(Modifier.fillMaxSize().background(Color.Black)) {
                    if (dialogItem.value == null) return@Column
                    val map by remember { mutableStateOf(mutableMapOf<String, List<Map<String, Any>>>()) }
                    dialogItem.value!!.fields?.forEach { (name, editmeta) ->
                        var text by remember { mutableStateOf("") }
                        Text(editmeta.name, modifier = Modifier.fillMaxWidth())
                        TextField(text, onValueChange = {
                            text = it
                            map[name] = listOf(mapOf(editmeta.operations.first() to text))
                        }, modifier = Modifier.fillMaxWidth())
                    }
                    Button(onClick = {
                        repo.doTransition(issue.value.key, Update(map, transition = TransitionTo(dialogItem.value!!.id))) { res ->
                            when (res) {
                                is Result.Error -> println(res.exception)
                                is Result.Success -> {
                                    repo.getIssue(issue.value.key) {
                                        when (it) {
                                            is Result.Error -> println(it.exception)
                                            is Result.Success -> issue.value = it.data
                                        }
                                    }
                                }
                            }
                        }
                        dialogItem.value = null
                    }) {
                        Text("Go")
                    }
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun RequestedParticipantsField(issue: MutableState<Issue>) {
    val names = issue.value.fields.requestedParticipants?.mapNotNull { it?.displayName } ?: emptyList()
    Label("Requested") {
        var expanded by remember { mutableStateOf(false) }
        Box {
            Text(
                text = "${names.size} requested",
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
                )
            )
            DropdownMenu(expanded = expanded, onDismissRequest = {}) {
                names.forEach {
                    Text(text = it, style = TextStyle(fontSize = 13.sp), modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun WatchersField(issue: MutableState<Issue>) {
    val repo = Repository.current
    val watchers = uiStateFrom(issue.value.key) { clb: (Result<Watchers>) -> Unit -> repo.getWatchers(issue.value.key, clb) }.value
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
                    )
                )
                DropdownMenu(expanded = expanded, onDismissRequest = {}) {
                    texts.forEach {
                        Text(text = it, style = TextStyle(fontSize = 13.sp), modifier = Modifier.padding(4.dp))
                    }
                }
            }
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

@ExperimentalComposeUiApi
@Composable
fun IssuesList(openedIssues: SnapshotStateList<IssueHead>, currentIssue: MutableState<IssueHead?>, currentFilter: MutableState<Filter?>) {
    val repo = Repository.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FilterDropdown(currentIssue, currentFilter)
                        var searchText by remember { mutableStateOf("") }
                        var loading by remember { mutableStateOf(false) }
                        var error by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = searchText,
                                onValueChange = { error = false; searchText = it },
                                modifier = Modifier.fillMaxWidth().onKeyEvent { event ->
                                    if (event.key == Key.Enter) {
                                        loading = true
                                        repo.getIssue(searchText) {
                                            when (it) {
                                                is Result.Error -> {
                                                    error = true
                                                    loading = false
                                                }

                                                is Result.Success -> {
                                                    loading = false
                                                    val iss = it.data
                                                    val head = IssueHead(iss.id, iss.key, iss.fields.toHeadFields())
                                                    if (head !in openedIssues) openedIssues += head
                                                    currentIssue.value = head
                                                }
                                            }
                                        }
                                    }
                                    false
                                },
                                placeholder = { Text("Search") },
                                trailingIcon = {
                                    if (error) {
                                        Icon(Icons.Filled.Error, "error", tint = Color.Red)
                                    }
                                    if (loading) {
                                        CircularProgressIndicator()
                                    }
                                },
                                singleLine = true
                            )
                        }
                    }
                }
            )
        },
        content = {
            ListBody(openedIssues, currentIssue, currentFilter)
        }
    )
}

@ExperimentalComposeUiApi
@Composable
fun CommentsList(issue: MutableState<Issue>, commentState: MutableState<CommentState>) {
    val repo = Repository.current
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
                        title = { CommentListHeader(items, commentState) },
                    )
                },
                content = {
                    Box(Modifier.fillMaxSize()) {
                        val scroll = rememberScrollState()
                        Box(Modifier.fillMaxSize().padding(end = 6.dp).verticalScroll(scroll)) {
                            Column {
                                if (!commentState.value.ascending) {
                                    CommentEditor(issue)
                                }
                                val sorted = if (commentState.value.ascending) items.sortedBy { it.created } else items.sortedByDescending { it.created }
                                sorted.forEach {
                                    if (it.isComment && commentState.value.filter != CommentFilter.HISTORY) {
                                        CommentItem(it.comment!!, issue.value.fields.attachment)
                                    } else if (it.isHistory && commentState.value.filter != CommentFilter.COMMENTS) {
                                        HistoryItem(it.history!!)
                                    }
                                }
                                if (commentState.value.ascending) {
                                    CommentEditor(issue)
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scroll),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            style = scrollbarStyle
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun CommentListHeader(items: List<DatedIssueItem>, commentState: MutableState<CommentState>) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${items.count { it.isComment }} Kommentare", Modifier.padding(top = 4.dp, bottom = 2.dp), style = fieldTitleStyle)
        Spacer(Modifier.width(6.dp))
        Button(
            onClick = { commentState.value = commentState.value.copy(ascending = !commentState.value.ascending) },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(2.dp)
        ) {
            Text(if (commentState.value.ascending) "Älteste zuerst" else "Neueste zuerst", fontSize = 12.sp)
        }
        Spacer(Modifier.width(20.dp))
        CommentFilterButton(CommentFilter.ALL, "Alle", commentState)
        Spacer(Modifier.width(4.dp))
        CommentFilterButton(CommentFilter.COMMENTS, "Kommentare", commentState)
        Spacer(Modifier.width(4.dp))
        CommentFilterButton(CommentFilter.HISTORY, "Historie", commentState)
    }
}

@Composable
fun CommentFilterButton(filter: CommentFilter, text: String, commentState: MutableState<CommentState>) {
    Button(
        onClick = { commentState.value = commentState.value.copy(filter = filter) },
        modifier = Modifier.height(20.dp),
        contentPadding = PaddingValues(2.dp),
        border = if (commentState.value.filter == filter) BorderStroke(1.dp, Color.White) else null
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun FilterDropdown(currentIssue: MutableState<IssueHead?>, currentFilter: MutableState<Filter?>) {
    var expanded by remember { mutableStateOf(false) }
    val repo = Repository.current
    val filters = uiStateFrom(currentFilter) { clb: (Result<List<Filter>>) -> Unit -> repo.getFilters(clb) }
    Box(Modifier.fillMaxWidth(0.5F).wrapContentSize(Alignment.CenterStart).border(2.dp, Color.Gray)) {
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
                    is UiState.Error -> ErrorText(state.exception)
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
fun ListBody(openedIssues: SnapshotStateList<IssueHead>, currentIssue: MutableState<IssueHead?>, currentFilter: MutableState<Filter?>) {
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
                                Box(
                                    modifier = Modifier.clickable {
                                        if (iss !in openedIssues) openedIssues += iss
                                        currentIssue.value = iss
                                    },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    ListItem(iss)
                                }
                            }
                        }

                        is UiState.Loading -> Loader()
                        is UiState.Error -> ErrorText(it.exception)
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
                    Text(it, modifier = Modifier.padding(2.dp), color = Color.Gray)
                }
                issueHead.fields.priority?.name?.let {
                    Spacer(Modifier.width(5.dp))
                    Text(it, modifier = Modifier.padding(2.dp), color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment, attachments: List<Attachment>?) {
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

@Composable
fun ClickableImage(file: File) {
    var showDialog by remember { mutableStateOf(false) }
    val img = makeFromEncoded(file.readBytes()).toComposeImageBitmap()
    Image(
        bitmap = img,
        contentDescription = "",
        modifier = Modifier.padding(4.dp).width(200.dp).heightIn(max = 200.dp).clickable { showDialog = true },
        contentScale = ContentScale.Fit,
        alignment = Alignment.TopCenter
    )
    if (showDialog) {
        Dialog(
            onCloseRequest = { showDialog = false },
            state = rememberDialogState(size = DpSize(img.width.dp, img.height.dp)),
            undecorated = false,
            resizable = false
        ) {
            Image(
                bitmap = img,
                contentDescription = "",
                modifier = Modifier.padding(4.dp).fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopCenter
            )
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun HistoryItem(history: History) {
    Card(Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(40, 40, 40), border = BorderStroke(1.dp, Color.Gray)) {
        Column(Modifier.fillMaxWidth().padding(2.dp)) {
            Text(
                text = AnnotatedString.Builder().apply {
                    append("Änderung")
                    history.author.displayName?.let {
                        append(" von ")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(it)
                        pop()
                    }
                    append(" ")
                    append(timePrinter.format(history.created))
                }.toAnnotatedString(),
                color = Color.Gray
            )
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
        Text(
            "$label:",
            style = labelTitleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 3.dp)
        )
        content()
    }
}

data class CommentState(var filter: CommentFilter, var ascending: Boolean)

enum class CommentFilter {
    COMMENTS, HISTORY, ALL
}

private fun String.parseJiraText(fontSize: TextUnit = 14.sp, repo: JiraRepository): AnnotatedString {
    val lines = split("\n").map { it.split(Regex("(\\s+| )")) }
    return AnnotatedString.Builder().apply {
        pushStyle(SpanStyle(color = Color(219, 219, 219), fontSize = fontSize))
        var count = 0
        lines.forEachIndexed { index, line ->
            if (index > 0) {
                append("\n")
                count++
            }
            line.forEach { word ->
                count += when {
                    word.isHyperlink() -> {
                        pushStringAnnotation(tag = "URL", annotation = word)
                        withStyle(style = SpanStyle(color = Color(88, 129, 252))) {
                            append(word)
                        }
                        pop()
                        word.length
                    }

                    word.isMention() -> {
                        val user = word.getUserFromMention()
                        var translated = ""
                        repo.getUser(user) {
                            val displayName = when (it) {
                                is Result.Error -> "[ERROR]"
                                is Result.Success -> it.data.displayName ?: "[ERROR]"
                            }
                            translated = word.replace(Regex("\\[~\\S*]"), displayName)
                        }
                        while (translated.isEmpty()) {
                            Thread.sleep(1L)
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(translated)
                        }
                        translated.length
                    }

                    else -> {
                        append(word)
                        word.length
                    }
                }
                append(" ")
                count++
            }
        }
    }.toAnnotatedString()
}

private fun String.getAttachments() = Regex("!.+\\..+\\|?.*!").findAll(this).map {it.value }.toList()

private fun String.isHyperlink() = startsWith("http")

private fun String.isMention() = contains(Regex("\\[~\\S*]"))

private fun String.getUserFromMention() = dropWhile { it != '~' }.drop(1).takeWhile { it != ']' }
