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

    private val timeOffset = -3600 * 24L

    init {
        GlobalScope.launch {
            while (true) {
                collectForAll()
                delay(10000)
            }
        }
    }

    private fun collectForAll() {
        trackedIssues.keys.forEach(::collectNotifications)
    }

    private fun collectNotifications(head: IssueHead) {
        val d = trackedIssues[head]?.toInstant()?.plusSeconds(timeOffset) ?: return
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

    fun addIssues(vararg issues: IssueHead) {
        issues.forEach {
            if (trackedIssues.putIfAbsent(it, Date()) == null) {
                collectNotifications(it)
            }
        }
    }

    fun updateIssue(issue: IssueHead) {
        trackedIssues[issue] = Date()
    }

    fun removeIssues(vararg issues: IssueHead) {
        issues.forEach { head ->
            trackedIssues.remove(head)
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