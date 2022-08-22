package ui

import ErrorText
import FullsizeInfo
import Loader
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import data.api.*
import data.local.NotificationService
import data.local.Settings.Companion.settings
import org.ocpsoft.prettytime.PrettyTime
import ui.splitter.SplitterState
import ui.splitter.VerticalSplittable
import java.awt.Desktop

val Repository = compositionLocalOf<JiraRepository> { error("Undefined repository") }
val NotificationService = compositionLocalOf<NotificationService> { error("Undefined service") }

val timePrinter = PrettyTime()

private val issueDateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)
private val attachmentTitleStyle = TextStyle(color = Color.LightGray, fontSize = 14.sp)
private val fieldTitleStyle = TextStyle(color = Color.LightGray, fontStyle = FontStyle.Italic, fontSize = 14.sp)
private val labelTitleStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
private val labelValueStyle = TextStyle(fontSize = 14.sp)


@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun IssuesView(onSettings: () -> Unit) {
    val openedIssues = remember { mutableStateListOf<IssueHead>() }
    val issueState = remember { mutableStateOf<IssueHead?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val splitter = SplitterState()
            var width by mutableStateOf(this@BoxWithConstraints.maxWidth * 0.25F)
            val range = 150.dp..(this@BoxWithConstraints.maxWidth * 0.5F)
            VerticalSplittable(Modifier.fillMaxSize(), splitter, onResize = { width = (width + it).coerceIn(range) }) {
                Box(modifier = Modifier.width(width), contentAlignment = Alignment.Center) {
                    IssuesList(openedIssues, issueState)
                }
                OpenedIssues(openedIssues, issueState, onSettings)
            }
            Divider(Modifier.fillMaxWidth(), Color.Gray)
            Footer()
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
private fun OpenedIssues(openedIssues: SnapshotStateList<IssueHead>, issueState: MutableState<IssueHead?>, onSettings: () -> Unit) = Column {
    val notifyService = NotificationService.current
    val notifications = notifyService.notifications.collectAsState(emptyList()).value
    val notifyOpen = remember { mutableStateOf(false) }
    OpenedIssuesNavigationBar(openedIssues, issueState, notifyOpen, onSettings)
    BoxWithConstraints {
        Row(Modifier.fillMaxSize()) {
            CurrentIssue(modifier = Modifier.width(this@BoxWithConstraints.maxWidth - (if (notifyOpen.value) 340.dp else 0.dp)), issueState)
            if (notifyOpen.value) NotificationList(modifier = Modifier.width(340.dp).border(2.dp, Color.DarkGray), notifications)
        }
    }
}

@Composable
private fun Footer() = Row(Modifier.fillMaxWidth().height(24.dp)) {

}

@Composable
private fun OpenedIssuesNavigationBar(
    openedIssues: SnapshotStateList<IssueHead>,
    issueState: MutableState<IssueHead?>,
    notifyOpen: MutableState<Boolean>,
    onSettings: () -> Unit
) = BoxWithConstraints(Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colors.background)) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        if (openedIssues.isNotEmpty()) {
            val index = openedIssues.indexOf(issueState.value).coerceIn(openedIssues.indices)
            ScrollableTabRow(selectedTabIndex = index, modifier = Modifier.width(this@BoxWithConstraints.maxWidth - 100.dp), edgePadding = 10.dp) {
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
        Row(modifier = Modifier.width(100.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { notifyOpen.value = !notifyOpen.value }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "toggle notification view",
                    tint = if (notifyOpen.value) Color.LightGray else MaterialTheme.colors.primary
                )
            }
            IconButton(onClick = { onSettings() }) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "settings", tint = MaterialTheme.colors.primary)
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
private fun CurrentIssue(modifier: Modifier, issueState: MutableState<IssueHead?>) = Scaffold(
    modifier = modifier,
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
    content = {
        val head = issueState.value
        if (head == null) FullsizeInfo { Text("Select issue") }
        else {
            val repo = Repository.current
            when (val issue = uiStateFrom(head.key) { clb: (Result<Issue>) -> Unit -> repo.getIssue(head.key, clb) }.value) {
                is UiState.Loading -> FullsizeInfo { Loader() }
                is UiState.Error -> FullsizeInfo { ErrorText("data.api.Issue loading error") }
                is UiState.Success -> {
                    val issueData = remember { mutableStateOf(issue.data) }
                    when (val editRes = uiStateFrom(issueData.value.key) { clb: (Result<Editmeta>) -> Unit -> repo.getEditmeta(issueData.value.key, clb) }.value) {
                        is UiState.Error -> FullsizeInfo { ErrorText(editRes.exception) }
                        is UiState.Loading -> FullsizeInfo { Loader() }
                        is UiState.Success -> CurrentIssueActive(issueData, editRes.data.fields)
                    }
                }
            }
        }
    }
)

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
private fun CurrentIssueActive(issueState: MutableState<Issue>, editmeta: Map<String, EditMetaField>) {
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
            issueState.value.fields.freetext?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Info-Text") {
                    ParsedText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
                }
            }
            issueState.value.fields.errorMessage?.let {
                Spacer(Modifier.height(6.dp))
                IssueField("Error message") {
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
                IssueField("Description") {
                    ParsedText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp), 16.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            CommentsList(issueState)
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
private fun AttachmentCard(attachment: Attachment, down: () -> Unit) {
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
private fun IssueHeaderInfo(issueState: MutableState<Issue>) {
    FlowRow(horizontalGap = 6.dp, verticalGap = 3.dp) {
        Text(AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(fontSize = 13.sp))
            append("Created: ")
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(timePrinter.format(issueState.value.fields.created))
            issueState.value.fields.reporter?.displayName?.let {
                append(" by ")
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(it)
                pop()
            }
        }.toAnnotatedString())
        issueState.value.fields.updated?.let {
            Text(AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(fontSize = 13.sp))
                append("| Changed: ")
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
                append("| to reproduce: ")
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(it)
            }.toAnnotatedString())
        }
    }
}

@ExperimentalComposeUiApi
@Composable
private fun IssueFields(issueState: MutableState<Issue>, editmeta: Map<String, EditMetaField>) {
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
private fun AssigneeField(issue: MutableState<Issue>) {
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
private fun PriorityField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("Priority") {
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
private fun StatusField(issue: MutableState<Issue>) {
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
private fun SystemField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
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
private fun TypeField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("Type") {
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
private fun CategoryField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("Category") {
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
private fun AreaField(issue: MutableState<Issue>, editmeta: EditMetaField?) {
    Label("Area") {
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
private fun WorkflowField(issue: MutableState<Issue>) {
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
private fun RequestedParticipantsField(issue: MutableState<Issue>) {
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
private fun WatchersField(issue: MutableState<Issue>) {
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
private fun IssueField(label: String, content: @Composable () -> Unit) {
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
private fun IssuesList(openedIssues: SnapshotStateList<IssueHead>, currentIssue: MutableState<IssueHead?>) {
    val repo = Repository.current
    val currentFilter: MutableState<Filter?> = remember { mutableStateOf(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        FilterDropdown(currentFilter)
                        Row(modifier = Modifier.padding(end = 10.dp), verticalAlignment = Alignment.Bottom) {
                            val project = remember { mutableStateOf(settings.projects[0]) }
                            ProjectDropdown(project)
                            Text("-", modifier = Modifier.padding(6.dp))
                            IssueTextField { issue ->
                                repo.getIssue("${project.value}-$issue") {
                                    if (it is Result.Success) {
                                        val iss = it.data
                                        val head = IssueHead(iss.id, iss.key, iss.fields.toHeadFields())
                                        if (head !in openedIssues) openedIssues += head
                                        currentIssue.value = head
                                    }
                                }
                            }
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
private fun FilterDropdown(currentFilter: MutableState<Filter?>) {
    Column {
        Text("Filter", fontSize = 12.sp)

        var expanded by remember { mutableStateOf(false) }
        val repo = Repository.current
        val filters = uiStateFrom(currentFilter) { clb: (Result<List<Filter>>) -> Unit -> repo.getFilters(clb) }
        Box(Modifier.wrapContentSize(Alignment.CenterStart).border(2.dp, Color.Gray)) {
            Text(currentFilter.value?.name ?: "Filter", modifier = Modifier.clickable(onClick = { expanded = true }).padding(6.dp))
            DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.requiredSizeIn(maxHeight = 600.dp)) {
                filters.value.let { state ->
                    when (state) {
                        is UiState.Success -> {
                            state.data.sortedBy { it.id.toInt() }.forEach { filter ->
                                Text(text = filter.name, modifier = Modifier.fillMaxWidth().padding(2.dp).clickable {
                                    currentFilter.value = filter
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
}

@Composable
private fun ProjectDropdown(project: MutableState<String>) {
    Column {
        Text("Project", fontSize = 12.sp)
        var expanded by remember { mutableStateOf(false) }
        Box(Modifier.wrapContentSize(Alignment.CenterStart).border(2.dp, Color.Gray)) {
            Text(project.value, modifier = Modifier.clickable(onClick = { expanded = true }).padding(6.dp))
            DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.requiredSizeIn(maxHeight = 600.dp)) {
                settings.projects.forEach { p ->
                    Text(text = p, modifier = Modifier.fillMaxWidth().padding(2.dp).clickable {
                        project.value = p
                        expanded = false
                    })
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
private fun IssueTextField(onSubmit: (String) -> Unit) {
    Column {
        Text("Issue", fontSize = 12.sp)
        var issueNumber by remember { mutableStateOf("") }
        BasicTextField(
            value = issueNumber,
            onValueChange = { issueNumber = it },
            modifier = Modifier.height(34.dp).border(2.dp, Color.Gray).width(80.dp).onKeyEvent { event ->
                if (event.key == Key.Enter) onSubmit(issueNumber)
                false
            },
            textStyle = TextStyle(color = Color.LightGray, fontSize = 18.sp),
            singleLine = true,
            cursorBrush = SolidColor(Color.Gray),
            decorationBox = { input -> Box(Modifier.padding(6.dp)) { input() } }
        )
    }
}

@Composable
private fun ListBody(openedIssues: SnapshotStateList<IssueHead>, currentIssue: MutableState<IssueHead?>, currentFilter: MutableState<Filter?>) {
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
private fun ListItem(issueHead: IssueHead) {
    Card(modifier = Modifier.padding(4.dp).fillMaxWidth(), backgroundColor = Color(54, 54, 54)) {
        Column(Modifier.fillMaxSize().padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(text = issueHead.key, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(text = "Created: " + timePrinter.format(issueHead.fields.created), style = issueDateStyle)
                    Text(text = "Changed: " + timePrinter.format(issueHead.fields.updated), style = issueDateStyle)
                }
            }
            Text(issueHead.fields.summary ?: "")
            Row(verticalAlignment = Alignment.CenterVertically) {
                issueHead.fields.status?.name?.let {
                    Text(text = it, modifier = Modifier.padding(2.dp), color = Color.Gray)
                }
                issueHead.fields.priority?.name?.let {
                    Spacer(Modifier.width(5.dp))
                    Box(Modifier.background(it.priorityColor(), RoundedCornerShape(4.dp))) {
                        Text(text = it, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.LightGray)
                    }
                }
            }
        }
    }
}

private fun String.priorityColor() = when (this.lowercase()) {
    "low" -> Color.DarkGray
    "medium" -> Color(0x2f, 0x41, 0x91)
    "high" -> Color(0x9e, 0x5c, 0x11)
    "blocker" -> Color(0x91, 0x2f, 0x41)
    else -> Color(0x3d, 0x3c, 0x3b)
}

@Composable
private fun Label(label: String, content: @Composable RowScope.() -> Unit) {
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
