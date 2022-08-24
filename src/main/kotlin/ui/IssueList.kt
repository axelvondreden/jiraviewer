package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.api.Filter
import data.api.IssueHead
import data.api.Result
import data.api.SearchResult
import data.local.Settings
import ui.utils.ErrorText
import ui.utils.Loader
import ui.utils.UiState
import ui.utils.uiStateFrom

private val issueDateStyle = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp)

@ExperimentalComposeUiApi
@Composable
fun IssueList(onOpenIssue: (IssueHead) -> Unit) {
    val repo = Repository.current
    val currentFilter: MutableState<Filter?> = remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        FilterDropdown(currentFilter)
                        Row(modifier = Modifier.padding(end = 10.dp), verticalAlignment = Alignment.Bottom) {
                            val project = remember { mutableStateOf(Settings.settings.projects[0]) }

                            ProjectDropdown(project)
                            Text("-", modifier = Modifier.padding(6.dp))
                            IssueTextField { issue ->
                                repo.getIssue("${project.value}-$issue") {
                                    if (it is Result.Success) {
                                        val iss = it.data
                                        onOpenIssue(IssueHead(iss.id, iss.key, iss.fields.toHeadFields()))
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        content = {
            ListBody(currentFilter, onOpenIssue)
        }
    )
}

@Composable
private fun FilterDropdown(currentFilter: MutableState<Filter?>) = Column {
    var expanded by remember { mutableStateOf(false) }
    val repo = Repository.current
    val notify = NotificationService.current
    val filters = uiStateFrom(currentFilter) { clb: (Result<List<Filter>>) -> Unit -> repo.getFilters(clb) }

    Text("Filter", fontSize = 12.sp)
    Box(Modifier.wrapContentSize(Alignment.CenterStart).border(2.dp, Color.Gray)) {
        Text(currentFilter.value?.name ?: "Filter", modifier = Modifier.clickable(onClick = { expanded = true }).padding(6.dp))
        DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.requiredSizeIn(maxHeight = 600.dp)) {
            filters.value.let { state ->
                when (state) {
                    is UiState.Success -> {
                        state.data.sortedBy { it.id.toInt() }.forEach { filter ->
                            Text(text = filter.name, modifier = Modifier.fillMaxWidth().padding(2.dp).clickable {
                                currentFilter.value = filter
                                if (Settings.settings.updateStrategy == Settings.UpdateStrategy.FILTER) {
                                    notify.clear()
                                }
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
private fun ProjectDropdown(project: MutableState<String>) = Column {
    var expanded by remember { mutableStateOf(false) }

    Text("Project", fontSize = 12.sp)
    Box(Modifier.wrapContentSize(Alignment.CenterStart).border(2.dp, Color.Gray)) {
        Text(project.value, modifier = Modifier.clickable(onClick = { expanded = true }).padding(6.dp))
        DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.requiredSizeIn(maxHeight = 600.dp)) {
            Settings.settings.projects.forEach { p ->
                Text(text = p, modifier = Modifier.fillMaxWidth().padding(2.dp).clickable {
                    project.value = p
                    expanded = false
                })
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
private fun IssueTextField(onSubmit: (String) -> Unit) = Column {
    var issueNumber by remember { mutableStateOf("") }

    Text("Issue", fontSize = 12.sp)
    BasicTextField(
        value = issueNumber,
        onValueChange = { issueNumber = it },
        modifier = Modifier.height(30.dp).border(2.dp, Color.Gray).width(80.dp).onKeyEvent { event ->
            if (event.key == Key.Enter) onSubmit(issueNumber)
            false
        },
        textStyle = TextStyle(color = Color.LightGray, fontSize = 18.sp),
        singleLine = true,
        cursorBrush = SolidColor(Color.Gray),
        decorationBox = { input -> Box(Modifier.padding(6.dp)) { input() } }
    )
}

@Composable
private fun ListBody(currentFilter: MutableState<Filter?>, onOpenIssue: (IssueHead) -> Unit) {
    val repo = Repository.current
    val issues = uiStateFrom(currentFilter.value?.jql) { clb: (Result<SearchResult>) -> Unit ->
        repo.getIssues(currentFilter.value?.jql, clb)
    }

    if (issues.value is UiState.Success && Settings.settings.updateStrategy == Settings.UpdateStrategy.FILTER) {
        val notify = NotificationService.current
        notify.addIssues(*(issues.value as UiState.Success<SearchResult>).data.issues.toTypedArray())
    }

    Box(Modifier.fillMaxSize()) {
        val scroll = rememberScrollState()

        Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Column {
                issues.value.let {
                    when (it) {
                        is UiState.Success -> {
                            it.data.issues.forEach { iss ->
                                Box(modifier = Modifier.clickable { onOpenIssue(iss) }, contentAlignment = Alignment.CenterStart) {
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
        Column(Modifier.padding(2.dp).fillMaxSize().padding(4.dp)) {
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