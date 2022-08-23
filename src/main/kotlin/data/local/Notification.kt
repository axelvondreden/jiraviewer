package data.local

import data.api.Comment
import data.api.History
import data.api.IssueHead
import java.util.*

data class Notification constructor(val head: IssueHead, val title: String, val date: Date) {

    constructor(issue: IssueHead, comment: Comment) : this(issue, "New Comment", comment.created)

    constructor(issue: IssueHead, history: History) : this(issue, "Update", history.created)
}
