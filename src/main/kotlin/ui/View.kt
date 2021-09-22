package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.window.Dialog
import data.*
import org.ocpsoft.prettytime.PrettyTime
import java.awt.Desktop

val Repository = compositionLocalOf<JiraRepository> { error("Undefined repository") }

private val timePrinter = PrettyTime()

private val issueDateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)
private val attachmentTitleStyle = TextStyle(color = Color.LightGray, fontSize = 14.sp)
private val fieldTitleStyle = TextStyle(color = Color.LightGray, fontStyle = FontStyle.Italic, fontSize = 14.sp)
private val labelTitleStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
private val labelValueStyle = TextStyle(fontSize = 14.sp)


@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun IssuesView() {
    MaterialTheme(colors = darkColors(primary = Color(120, 120, 120), onPrimary = Color.White)) {
        Main()
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun Main() {
    val currentIssue: MutableState<IssueHead?> = remember { mutableStateOf(null) }
    val currentFilter: MutableState<Filter?> = remember { mutableStateOf(null) }
    val commentState = remember { mutableStateOf(CommentState(CommentFilter.COMMENTS, true)) }
    BoxWithConstraints {
        if (maxWidth.value > 1000) {
            TwoColLayout(currentIssue, currentFilter, commentState)
        } else {
            SingleColLayout(currentIssue, currentFilter, commentState)
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun SingleColLayout(issueState: MutableState<IssueHead?>, filterState: MutableState<Filter?>, commentState: MutableState<CommentState>) {
    if (issueState.value == null) {
        IssuesList(issueState, filterState)
    } else {
        CurrentIssue(issueState, commentState, true)
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun TwoColLayout(issueState: MutableState<IssueHead?>, filterState: MutableState<Filter?>, commentState: MutableState<CommentState>) {
    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth(0.25f), contentAlignment = Alignment.Center) {
            IssuesList(issueState, filterState)
        }
        CurrentIssue(issueState, commentState, false)
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun CurrentIssue(issueState: MutableState<IssueHead?>, commentState: MutableState<CommentState>, backButton: Boolean) {
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

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun CurrentIssueContent(head: IssueHead?, commentState: MutableState<CommentState>) {
    if (head == null) CurrentIssueStatus { Text("Select issue") }
    else {
        val repo = Repository.current
        val issue = uiStateFrom(head.key) { clb: (Result<Issue>) -> Unit -> repo.getIssue(head.key, clb) }.value
        when (issue) {
            is UiState.Loading -> CurrentIssueStatus { Loader() }
            is UiState.Error -> CurrentIssueStatus { Error("data.Issue loading error") }
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
    val editRes = uiStateFrom(issueState.value.key) { clb: (Result<Editmeta>) -> Unit -> repo.getEditmeta(issueState.value.key, clb) }.value
    when (editRes) {
        is UiState.Error -> CurrentIssueStatus { Error(editRes.exception) }
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
                    SelectionContainer(Modifier.border(1.dp, Color.LightGray).fillMaxWidth()) {
                        Text(text = it, modifier = Modifier.padding(3.dp))
                    }
                }
            }
            issueState.value.fields.fehlermeldung?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Fehlermeldung") {
                    SelectionContainer(Modifier.border(1.dp, Color.LightGray).fillMaxWidth()) {
                        Text(text = it, modifier = Modifier.padding(3.dp))
                    }
                }
            }
            issueState.value.fields.helpText?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("AP+ URL") {
                    SelectionContainer(Modifier.border(1.dp, Color.LightGray).fillMaxWidth()) {
                        Text(text = it, modifier = Modifier.padding(3.dp))
                    }
                }
            }
            issueState.value.fields.helpText2?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("AP+ URL") {
                    SelectionContainer(Modifier.border(1.dp, Color.LightGray).fillMaxWidth()) {
                        Text(text = it, modifier = Modifier.padding(3.dp))
                    }
                }
            }
            issueState.value.fields.description?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Beschreibung") {
                    SelectionContainer(Modifier.border(1.dp, Color.LightGray).fillMaxWidth()) {
                        Text(text = it, modifier = Modifier.padding(3.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            CommentsList(issueState, commentState)
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun AttachmentCard(attachment: Attachment, down: () -> Unit) {
    Card(border = BorderStroke(1.dp, Color.Gray), onClick = down) {
        Column(Modifier.padding(4.dp)) {
            Text(
                text = attachment.filename,
                maxLines = 1,
                style = attachmentTitleStyle,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(160.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    text = attachment.mimeType,
                    maxLines = 1,
                    style = issueDateStyle,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(120.dp)
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

@Composable
fun IssuesList(currentIssue: MutableState<IssueHead?>, currentFilter: MutableState<Filter?>) {
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

@ExperimentalComposeUiApi
@Composable
fun CommentsList(issue: MutableState<Issue>, commentState: MutableState<CommentState>) {
    val repo = Repository.current
    val comments = uiStateFrom(issue.value) { clb: (Result<Comments>) -> Unit -> repo.getComments(issue.value.key, clb) }.value
    when (comments) {
        is UiState.Error -> Error(comments.exception)
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
                                val sorted =
                                    if (commentState.value.ascending) items.sortedBy { it.created } else items.sortedByDescending { it.created }
                                sorted.forEach {
                                    if (it.isComment && commentState.value.filter != CommentFilter.HISTORY) {
                                        CommentItem(it.comment!!)
                                    } else if (it.isHistory && commentState.value.filter != CommentFilter.COMMENTS) {
                                        HistoryItem(it.history!!)
                                    }
                                }
                                if (commentState.value.ascending) {
                                    CommentEditor(issue)
                                }
                            }
                        }
                        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
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
            Row(Modifier.fillMaxWidth()) {
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

data class CommentState(var filter: CommentFilter, var ascending: Boolean)

enum class CommentFilter {
    COMMENTS, HISTORY, ALL
}