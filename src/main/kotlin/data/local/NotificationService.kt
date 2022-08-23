package data.local


import androidx.compose.runtime.mutableStateListOf
import data.api.IssueHead
import data.api.JiraRepository
import data.api.Result
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class NotificationService(private val repo: JiraRepository) {

    val notifications = mutableStateListOf<Notification>()
    private var dismissed = mutableSetOf<Notification>()

    private val trackedIssues = mutableMapOf<IssueHead, Date>()

    init {
        GlobalScope.launch {
            while (true) {
                collectNotifications()
                delay(10000)
            }
        }
    }

    private fun collectNotifications() {
        trackedIssues.forEach { (head, time) ->
            val d = time.toInstant().plusSeconds(-3600 * 24)
            repo.getComments(head.key) { result ->
                if (result is Result.Success) {
                    notifications.addAll(result.data.comments.filter { it.created.toInstant().isAfter(d) }
                        .map { Notification(head, it) }.filterNot { it in dismissed || it in notifications })
                }
                repo.getIssue(head.key) { result1 ->
                    if (result1 is Result.Success) {
                        notifications.addAll(result1.data.changelog?.histories?.filter { it.created.toInstant().isAfter(d) }
                            ?.map { Notification(head, it) }?.filterNot { it in dismissed || it in notifications } ?: emptyList())
                    }
                }
            }
        }
    }

    fun addIssues(vararg issues: IssueHead) {
        synchronized(trackedIssues) {
            issues.forEach {
                trackedIssues.putIfAbsent(it, Date())
            }
        }
    }

    fun updateIssue(issue: IssueHead) {
        synchronized(trackedIssues) {
            trackedIssues[issue] = Date()
        }
    }

    fun removeIssues(vararg issues: IssueHead) {
        synchronized(trackedIssues) {
            issues.forEach { head ->
                trackedIssues.remove(head)
            }
        }
    }

    fun dismiss(notification: Notification) {
        dismissed += notification
        notifications.remove(notification)
    }

    fun clear() {
        trackedIssues.clear()
    }
}