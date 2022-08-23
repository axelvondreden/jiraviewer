package data.local


import data.api.IssueHead
import data.api.JiraRepository
import data.api.Result
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.*

class NotificationService(private val repo: JiraRepository) {

    private var _notifications = emptyList<Notification>()
    private var dismissed = emptyList<Notification>()

    private val trackedIssues = mutableMapOf<IssueHead, Date>()

    val notifications = flow {
        while (true) {
            emit(_notifications)
            delay(5000)
        }
    }

    init {
        GlobalScope.launch {
            while (true) {
                collectNotifications()
                delay(5000)
            }
        }
    }

    private fun collectNotifications() {
        val list = mutableListOf<Notification>()
        val tracked = trackedIssues.toMap()
        val size = tracked.size
        var count = 0
        tracked.forEach { (head, time) ->
            repo.getComments(head.key) { result ->
                if (result is Result.Success) {
                    list.addAll(result.data.comments.filter { it.created.after(time) }.map { Notification(head, it) })
                }
                repo.getIssue(head.key) { result1 ->
                    if (result1 is Result.Success) {
                        list.addAll(result1.data.changelog?.histories?.filter { it.created.after(time) }?.map { Notification(head, it) } ?: emptyList())
                    }
                    count++
                }
            }
        }
        while (count < size) {
            Thread.sleep(1L)
        }
        _notifications = list.filterNot { it in dismissed }.sortedByDescending { it.date }
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
                dismissed = dismissed.filterNot { it.head == head }
            }
        }
    }

    fun dismiss(notification: Notification) {
        dismissed = dismissed.plus(notification)
        collectNotifications()
    }

    fun clear() {
        trackedIssues.clear()
        dismissed = emptyList()
        collectNotifications()
    }
}