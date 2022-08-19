package ui

import ErrorText
import Loader
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.DpSize
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

val Repository = compositionLocalOf<JiraRepository> { error("Undefined repository") }

val timePrinter = PrettyTime()

private val issueDateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)
private val attachmentTitleStyle = TextStyle(color = Color.LightGray, fontSize = 14.sp)
private val fieldTitleStyle = TextStyle(color = Color.LightGray, fontStyle = FontStyle.Italic, fontSize = 14.sp)
private val labelTitleStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
private val labelValueStyle = TextStyle(fontSize = 14.sp)


@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun IssuesView() {
    val openedIssues: SnapshotStateList<IssueHead> = remember { mutableStateListOf() }
    val currentIssue: MutableState<IssueHead?> = remember { mutableStateOf(null) }
    TwoColLayout(openedIssues, currentIssue)
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun TwoColLayout(openedIssues: SnapshotStateList<IssueHead>, issueState: MutableState<IssueHead?>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val splitter = SplitterState()
        var width by mutableStateOf(maxWidth * 0.25F)
        val range = 150.dp..(maxWidth * 0.5F)
        VerticalSplittable(Modifier.fillMaxSize(), splitter, onResize = { width = (width + it).coerceIn(range) }) {
            Box(modifier = Modifier.width(width), contentAlignment = Alignment.Center) {
                IssuesList(openedIssues, issueState)
            }
            OpenedIssues(openedIssues, issueState)
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun OpenedIssues(openedIssues: SnapshotStateList<IssueHead>, issueState: MutableState<IssueHead?>) {
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
                                issueState.value = if (openedIssues.isEmpty()) null else openedIssues.getOrNull(oldIndex.coerceIn(openedIssues.indices))
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
                    }
                )
            },
            content = { CurrentIssueContent(issueState.value) }
        )
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun CurrentIssueContent(head: IssueHead?) {
    if (head == null) CurrentIssueStatus { Text("Select issue") }
    else {
        val repo = Repository.current
        when (val issue = uiStateFrom(head.key) { clb: (Result<Issue>) -> Unit -> repo.getIssue(head.key, clb) }.value) {
            is UiState.Loading -> CurrentIssueStatus { Loader() }
            is UiState.Error -> CurrentIssueStatus { ErrorText("data.api.Issue loading error") }
            is UiState.Success -> CurrentIssueActiveContainer(issue.data)
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
fun CurrentIssueActiveContainer(issue: Issue) {
    val issueState = remember { mutableStateOf(issue) }
    val repo = Repository.current
    when (val editRes = uiStateFrom(issueState.value.key) { clb: (Result<Editmeta>) -> Unit -> repo.getEditmeta(issueState.value.key, clb) }.value) {
        is UiState.Error -> CurrentIssueStatus { ErrorText(editRes.exception) }
        is UiState.Loading -> CurrentIssueStatus { Loader() }
        is UiState.Success -> CurrentIssueActive(issueState, editRes.data.fields)
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun CurrentIssueActive(issueState: MutableState<Issue>, editmeta: Map<String, EditMetaField>) {
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
            CommentsList(issueState)
        }
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
fun IssuesList(openedIssues: SnapshotStateList<IssueHead>, currentIssue: MutableState<IssueHead?>) {
    val repo = Repository.current
    val currentFilter: MutableState<Filter?> = remember { mutableStateOf(null) }
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
