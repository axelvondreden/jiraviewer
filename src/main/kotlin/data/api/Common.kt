package data.api

data class DisplayName(val displayName: String?)

data class Name(val name: String?)

data class Value(val value: String?)

data class Filter(
    val id: String,
    val name: String,
    val jql: String
)

data class Myself(val name: String, val displayName: String)