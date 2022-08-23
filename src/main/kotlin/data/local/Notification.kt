package data.local

import data.api.Comment
import data.api.History
import data.api.IssueHead
import data.api.asChangelogString
import java.util.*

data class Notification constructor(val head: IssueHead, val title: String, val info: String, val user: String, val date: Date) {

    constructor(issue: IssueHead, comment: Comment) : this(issue, "New Comment", comment.body, comment.author.displayName ?: "", comment.created)

    constructor(issue: IssueHead, history: History) : this(issue, "Update", history.items.asChangelogString(), history.author.displayName ?: "", history.created)
}
