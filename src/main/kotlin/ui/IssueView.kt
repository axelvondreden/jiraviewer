package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import data.api.*
import ui.utils.*
import java.awt.Desktop

private val attachmentTitleStyle = TextStyle(color = Color.LightGray, fontSize = 14.sp)
private val fieldTitleStyle = TextStyle(color = Color.LightGray, fontStyle = FontStyle.Italic, fontSize = 14.sp)
private val issueDateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)
private val labelTitleStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
private val labelValueStyle = TextStyle(fontSize = 14.sp)

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun IssueView(modifier: Modifier, issueState: MutableState<IssueHead?>) = Scaffold(
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
            val notify = NotificationService.current
            when (val issue = uiStateFrom(head.key) { clb: (Result<Issue>) -> Unit -> repo.getIssue(head.key, clb) }.value) {
                is UiState.Loading -> FullsizeInfo { Loader() }
                is UiState.Error -> FullsizeInfo { ErrorText("data.api.Issue loading error") }
                is UiState.Success -> {
                    var issueData by remember { mutableStateOf(issue.data) }
                    when (val editRes = uiStateFrom(issueData.key) { clb: (Result<Editmeta>) -> Unit -> repo.getEditmeta(issueData.key, clb) }.value) {
                        is UiState.Error -> FullsizeInfo { ErrorText(editRes.exception) }
                        is UiState.Loading -> FullsizeInfo { Loader() }
                        is UiState.Success -> {
                            CurrentIssueActive(
                                issue = issueData,
                                editmeta = editRes.data.fields,
                                update = { upd -> updateIssue(repo, head, upd) { issueData = it } },
                                transition = { upd -> transitionIssue(repo, head, upd) { issueData = it } }
                            )
                            notify.updateIssue(head)
                        }
                    }
                }
            }
        }
    }
)

private fun updateIssue(repo: JiraRepository, head: IssueHead, update: Update?, onSuccess: (Issue) -> Unit) {
    if (update != null) {
        repo.updateIssue(head.key, update) { result ->
            when (result) {
                is Result.Error -> println(result.exception)
                is Result.Success -> refreshIssue(repo, head, onSuccess)
            }
        }
    } else {
        refreshIssue(repo, head, onSuccess)
    }
}

private fun transitionIssue(repo: JiraRepository, head: IssueHead, update: Update?, onSuccess: (Issue) -> Unit) {
    if (update != null) {
        repo.doTransition(head.key, update) { result ->
            when (result) {
                is Result.Error -> println(result.exception)
                is Result.Success -> refreshIssue(repo, head, onSuccess)
            }
        }
    } else {
        refreshIssue(repo, head, onSuccess)
    }
}

private fun refreshIssue(repo: JiraRepository, head: IssueHead, onSuccess: (Issue) -> Unit) {
    repo.getIssue(head.key) {
        when (it) {
            is Result.Error -> println(it.exception)
            is Result.Success -> onSuccess(it.data)
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
private fun CurrentIssueActive(
    issue: Issue,
    editmeta: Map<String, EditMetaField>,
    update: (Update?) -> Unit,
    transition: (Update?) -> Unit
) = Box(Modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(8.dp).fillMaxSize()) {
        IssueHeaderInfo(issue)
        Spacer(Modifier.height(6.dp))
        IssueFields(issue, editmeta, update, transition)
        issue.fields.attachment.takeIf { !it.isNullOrEmpty() }?.let {
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
        issue.fields.freetext?.let {
            Spacer(Modifier.height(6.dp))
            IssueField("Info-Text") {
                JiraText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
            }
        }
        issue.fields.errorMessage?.let {
            Spacer(Modifier.height(6.dp))
            IssueField("Error message") {
                JiraText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
            }
        }
        issue.fields.helpText?.let {
            Spacer(Modifier.height(6.dp))
            IssueField("AP+ URL") {
                JiraText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
            }
        }
        issue.fields.helpText2?.let {
            Spacer(Modifier.height(6.dp))
            IssueField("AP+ URL") {
                JiraText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp))
            }
        }
        issue.fields.description?.let {
            Spacer(Modifier.height(6.dp))
            IssueField("Description") {
                JiraText(it, Modifier.border(1.dp, Color.LightGray).fillMaxWidth().padding(3.dp), 16.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        CommentsList(issue, update)
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
private fun IssueHeaderInfo(issue: Issue) = FlowRow(horizontalGap = 6.dp, verticalGap = 3.dp) {
    Text(AnnotatedString.Builder().apply {
        pushStyle(SpanStyle(fontSize = 13.sp))
        append("Created: ")
        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
        append(timePrinter.format(issue.fields.created))
        issue.fields.reporter?.displayName?.let {
            append(" by ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(it)
            pop()
        }
    }.toAnnotatedString())
    issue.fields.updated?.let {
        Text(AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(fontSize = 13.sp))
            append("| Changed: ")
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(timePrinter.format(it))
        }.toAnnotatedString())
    }
    issue.fields.workaround?.value?.let {
        Text(AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(fontSize = 13.sp))
            append("| Workaround: ")
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(it)
        }.toAnnotatedString())
    }
    issue.fields.reproducable?.value?.let {
        Text(AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(fontSize = 13.sp))
            append("| to reproduce: ")
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(it)
        }.toAnnotatedString())
    }
}

@ExperimentalComposeUiApi
@Composable
private fun IssueFields(issue: Issue, editmeta: Map<String, EditMetaField>, update: (Update?) -> Unit, transition: (Update?) -> Unit) =
    FlowRow(horizontalGap = 3.dp, verticalGap = 3.dp) {
        Column(Modifier.width(220.dp)) {
            AssigneeField(issue)
            PriorityField(issue, editmeta["priority"], update)
        }
        Column(Modifier.width(220.dp)) {
            StatusField(issue)
            SystemField(issue, editmeta["customfield_14304"], update)
        }
        Column(Modifier.width(220.dp)) {
            TypeField(issue, editmeta["customfield_14302"], update)
            CategoryField(issue, editmeta["customfield_13902"], update)
        }
        Column(Modifier.width(220.dp)) {
            AreaField(issue, editmeta["customfield_14303"], update)
            WorkflowField(issue, transition)
        }
        Column(Modifier.width(220.dp)) {
            RequestedParticipantsField(issue)
            WatchersField(issue)
        }
    }

@Composable
private fun AssigneeField(issue: Issue) = Label("Assignee") {
    Text(
        text = issue.fields.assignee?.displayName ?: "",
        style = labelValueStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(end = 3.dp)
    )
}

@Composable
private fun PriorityField(issue: Issue, editmeta: EditMetaField?, update: (Update?) -> Unit) = Label("Priority") {
    Row {
        var edit by remember { mutableStateOf(false) }

        Box {
            Text(
                text = issue.fields.priority?.name ?: "",
                style = labelValueStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 3.dp)
            )
            if (editmeta != null) {
                DropdownMenu(expanded = edit, onDismissRequest = { edit = false }) {
                    editmeta.allowedValues?.forEach { allowedValue ->
                        Text(
                            text = allowedValue.name ?: "-",
                            style = TextStyle(fontSize = 13.sp),
                            modifier = Modifier.padding(4.dp).clickable {
                                update(Update(mapOf("priority" to listOf(mapOf("set" to mapOf("name" to allowedValue.name!!))))))
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

@Composable
private fun StatusField(issue: Issue) = Label("Status") {
    Text(
        text = issue.fields.status?.name ?: "",
        style = labelValueStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(end = 3.dp)
    )
}

@Composable
private fun SystemField(issue: Issue, editmeta: EditMetaField?, update: (Update?) -> Unit) = Label("System") {
    Row {
        var edit by remember { mutableStateOf(false) }

        Box {
            Text(
                text = issue.fields.system?.value ?: "",
                style = labelValueStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 3.dp)
            )
            if (editmeta != null) {
                DropdownMenu(expanded = edit, onDismissRequest = { edit = false }) {
                    editmeta.allowedValues?.forEach {
                        Text(
                            text = it.value ?: "-",
                            style = TextStyle(fontSize = 13.sp),
                            modifier = Modifier.padding(4.dp).clickable {
                                update(Update(mapOf("customfield_14304" to listOf(mapOf("set" to mapOf("value" to it.value!!))))))
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

@Composable
private fun TypeField(issue: Issue, editmeta: EditMetaField?, update: (Update?) -> Unit) = Label("Type") {
    Row {
        var edit by remember { mutableStateOf(false) }

        Box {
            Text(
                text = issue.fields.issueType?.value ?: "",
                style = labelValueStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 3.dp)
            )
            if (editmeta != null) {
                DropdownMenu(expanded = edit, onDismissRequest = { edit = false }) {
                    editmeta.allowedValues?.forEach {
                        Text(
                            text = it.value ?: "-",
                            style = TextStyle(fontSize = 13.sp),
                            modifier = Modifier.padding(4.dp).clickable {
                                update(Update(mapOf("customfield_14302" to listOf(mapOf("set" to mapOf("value" to it.value!!))))))
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

@Composable
private fun CategoryField(issue: Issue, editmeta: EditMetaField?, update: (Update?) -> Unit) = Label("Category") {
    Row {
        var edit by remember { mutableStateOf(false) }

        Box {
            Text(
                text = issue.fields.category?.value ?: "",
                style = labelValueStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 3.dp)
            )
            if (editmeta != null) {
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
                                update(Update(mapOf("customfield_13902" to listOf(mapOf("set" to mapOf("value" to it.value!!))))))
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

@Composable
private fun AreaField(issue: Issue, editmeta: EditMetaField?, update: (Update?) -> Unit) = Label("Area") {
    Row {
        var edit by remember { mutableStateOf(false) }

        Box {
            Text(
                text = issue.fields.area?.value ?: "",
                style = labelValueStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 3.dp)
            )
            if (editmeta != null) {
                DropdownMenu(expanded = edit, onDismissRequest = { edit = false }) {
                    editmeta.allowedValues?.forEach {
                        Text(
                            text = it.value ?: "-",
                            style = TextStyle(fontSize = 13.sp),
                            modifier = Modifier.padding(4.dp).clickable {
                                update(Update(mapOf("customfield_14303" to listOf(mapOf("set" to mapOf("value" to it.value!!))))))
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

@Composable
private fun WorkflowField(issue: Issue, transition: (Update?) -> Unit) {
    val repo = Repository.current
    val transitions = uiStateFrom(issue) { clb: (Result<Transitions>) -> Unit -> repo.getTransitions(issue.key, clb) }.value
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
                                transition(Update(transition = TransitionTo(transition.id)))
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
                        transition(Update(map, transition = TransitionTo(dialogItem.value!!.id)))
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
private fun RequestedParticipantsField(issue: Issue) {
    val names = issue.fields.requestedParticipants?.mapNotNull { it?.displayName } ?: emptyList()

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
private fun WatchersField(issue: Issue) {
    val repo = Repository.current
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

@Composable
private fun Label(label: String, content: @Composable RowScope.() -> Unit) =
    Row(modifier = Modifier.fillMaxWidth().padding(3.dp).border(1.dp, Color.Gray), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "$label:",
            style = labelTitleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 3.dp)
        )
        content()
    }
