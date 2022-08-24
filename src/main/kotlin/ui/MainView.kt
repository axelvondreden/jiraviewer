package ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import data.api.IssueHead
import data.api.JiraRepository
import data.local.NotificationService
import data.local.Settings
import data.local.Settings.Companion.settings
import org.ocpsoft.prettytime.PrettyTime
import ui.utils.SplitterState
import ui.utils.VerticalSplittable

val Repository = compositionLocalOf<JiraRepository> { error("Undefined repository") }
val NotificationService = compositionLocalOf<NotificationService> { error("Undefined service") }

val timePrinter = PrettyTime()


@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun MainView(onSettings: () -> Unit) {
    val openedIssues = remember { mutableStateListOf<IssueHead>() }
    val openedIssue = remember { mutableStateOf<IssueHead?>(null) }
    val notify = NotificationService.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val splitter = SplitterState()
            var width by mutableStateOf(this@BoxWithConstraints.maxWidth * 0.25F)
            val range = 150.dp..(this@BoxWithConstraints.maxWidth * 0.5F)

            VerticalSplittable(Modifier.fillMaxSize(), splitter, onResize = { width = (width + it).coerceIn(range) }) {
                Box(modifier = Modifier.width(width), contentAlignment = Alignment.Center) {
                    IssueList(onOpenIssue = { head -> openIssue(openedIssue, openedIssues, head) { notify.addIssues(it) } })
                }
                Column {
                    val notifyOpen = remember { mutableStateOf(false) }

                    IssuesNavigationBar(openedIssues, openedIssue, notifyOpen, onSettings)
                    Row(Modifier.fillMaxWidth()) {
                        IssueView(modifier = Modifier.weight(1F), openedIssue)

                        if (notifyOpen.value) {
                            NotificationList(
                                modifier = Modifier.width(340.dp).border(2.dp, Color.DarkGray),
                                items = notify.notifications.sortedByDescending { it.date },
                                onOpenIssue = { head -> openIssue(openedIssue, openedIssues, head) { notify.addIssues(it) } },
                                onDismiss = { notify.dismiss(it) }
                            )
                        }
                    }
                }
            }
            Footer(24.dp)
        }
    }
}

private fun openIssue(openedIssue: MutableState<IssueHead?>, openedIssues: SnapshotStateList<IssueHead>, head: IssueHead, onNotifyAdd: (IssueHead) -> Unit) {
    if (head !in openedIssues) {
        openedIssues += head
        if (settings.updateStrategy == Settings.UpdateStrategy.TABS) {
            onNotifyAdd(head)
        }
    }
    openedIssue.value = head
}

@Composable
private fun Footer(height: Dp) = Row(Modifier.fillMaxWidth().height(height)) {

}
