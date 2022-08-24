package data.local

import data.api.Comment
import data.api.History
import data.api.IssueHead
import data.api.asFormattedStrings
import java.util.*

data class Notification constructor(val head: IssueHead, val title: String, val info: List<String>, val user: String, val date: Date, val isComment: Boolean) {

    constructor(issue: IssueHead, comment: Comment) : this(issue, "Comment", listOf(comment.body), comment.author.displayName ?: "", comment.created, true)

    constructor(issue: IssueHead, history: History) : this(issue, "Update", history.items.asFormattedStrings(), history.author.displayName ?: "", history.created, false)
}
