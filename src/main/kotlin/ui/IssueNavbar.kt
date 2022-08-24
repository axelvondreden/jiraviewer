package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.api.IssueHead
import data.local.Settings

@Composable
fun IssuesNavigationBar(
    openedIssues: SnapshotStateList<IssueHead>,
    issueState: MutableState<IssueHead?>,
    notifyOpen: MutableState<Boolean>,
    notifyCount: Int,
    onSettings: () -> Unit
) = BoxWithConstraints(Modifier.fillMaxWidth().height(32.dp).background(MaterialTheme.colors.background)) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        if (openedIssues.isNotEmpty()) {
            val notify = NotificationService.current
            val index = openedIssues.indexOf(issueState.value).coerceIn(openedIssues.indices)

            ScrollableTabRow(selectedTabIndex = index, modifier = Modifier.width(this@BoxWithConstraints.maxWidth - 100.dp), edgePadding = 10.dp) {
                openedIssues.forEachIndexed { i, issueHead ->
                    Tab(selected = i == index, onClick = { issueState.value = issueHead }, modifier = Modifier.height(30.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(issueHead.key)
                            IconButton(onClick = {
                                // close the current tab and open the one to the right
                                val oldIndex = openedIssues.indexOf(issueHead)
                                openedIssues.remove(issueHead)
                                if (Settings.settings.updateStrategy == Settings.UpdateStrategy.TABS) {
                                    notify.removeIssues(issueHead)
                                }
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
                BadgedBox(badge = { if (notifyCount > 0) Badge(modifier = Modifier.padding(top = 10.dp)) { Text(notifyCount.toString()) } }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "toggle notification view",
                        tint = if (notifyOpen.value) Color.LightGray else MaterialTheme.colors.primary
                    )
                }
            }
            IconButton(onClick = { onSettings() }) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "settings", tint = MaterialTheme.colors.primary)
            }
        }
    }
}