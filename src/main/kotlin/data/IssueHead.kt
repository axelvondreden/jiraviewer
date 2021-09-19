package data

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.*

data class IssueHead(val id: String, val key: String, val fields: IssueHeadFields)

data class IssueHeadFields(
    val summary: String?,
    val description: String?,
    val status: Name?,
    val priority: Name?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT") val created: Date,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", timezone = "GMT") val updated: Date?
)

data class SearchResult(val total: Int, val issues: List<IssueHead>)
