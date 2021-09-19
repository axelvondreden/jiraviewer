package data

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class Issue(val id: String, val key: String, val fields: IssueFields, val changelog: Changelog? = null) {
    val datedItems by lazy {
        val cList = fields.comment?.comments?.map { DatedIssueItem(comment = it) } ?: emptyList()
        val hList = changelog?.histories?.map { DatedIssueItem(history = it) } ?: emptyList()
        cList.plus(hList)
    }
}

data class IssueFields(
    val summary: String?,
    val description: String?,
    val reporter: DisplayName?,
    val assignee: DisplayName?,
    val status: Name?,
    val priority: Name?,
    val attachment: List<Attachment>?,
    val comment: Comments?,
    @JsonProperty("customfield_10000") val requestedParticipants: List<DisplayName?>?,
    @JsonProperty("customfield_13902") val category: Value?,
    @JsonProperty("customfield_14302") val issueType: Value?,
    @JsonProperty("customfield_14303") val area: Value?,
    @JsonProperty("customfield_14304") val system: Value?,
    @JsonProperty("customfield_15501") val helpText: String?,
    @JsonProperty("customfield_15318") val freitext: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT") val created: Date,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT") val updated: Date?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT") val resolutiondate: Date?
)

class DatedIssueItem(val comment: Comment? = null, val history: History? = null) {
    val created get() = if (isComment) comment!!.created else history!!.created
    val isComment get() = comment != null
    val isHistory get() = history != null
}

data class Watchers(val watchCount: Int, val watchers: List<DisplayName>)

data class Comments(val total: Int, val comments: List<Comment>)

data class Comment(
    val author: DisplayName,
    val updateAuthor: DisplayName?,
    val body: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT")
    val created: Date,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT")
    val updated: Date?
)

data class Changelog(val total: Int, val histories: List<History>)

data class History(
    val author: DisplayName,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT")
    val created: Date,
    val items: List<HistoryItem>
)

data class HistoryItem(val field: String, val fromString: String?, val toString: String?)

data class Attachment(
    val id: String,
    val filename: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT")
    val created: Date,
    val mimeType: String,
    val content: String
)

data class Transitions(val transitions: List<Transition>)

data class Transition(val name: String, val to: TransitionTo)

data class TransitionTo(val id: String)

data class Editmeta(@JsonAnySetter val fields: Map<String, EditMetaField>)

data class EditMetaField(
    val name: String,
    val fieldId: String,
    val schema: EditmetaFieldSchema,
    val operations: List<String>,
    val allowedValues: List<AllowedValue>?
)

data class EditmetaFieldSchema(val type: String)

data class AllowedValue(val value: String?, val name: String?, val id: String, val disabled: Boolean?)
