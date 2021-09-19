import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import data.*
import java.io.File

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: String) : Result<Nothing>()
}

class IssueRepository(private val baseUrl: String, private val loginUrl: String, private val user: String, private val password: String) {

    private var cookies: List<String> = emptyList()

    fun getIssues(filter: String?, callback: (Result<SearchResult>) -> Unit) {
        if (filter.isNullOrBlank()) {
            callback(Result.Error("Missing Filter"))
            return
        }
        val search = SearchRequest(filter, 0, 100, headFields)
        postWithLogin("$baseUrl/search", search, callback) {
            callback(Result.Success(it))
        }
    }

    fun getIssue(key: String, callback: (Result<Issue>) -> Unit) {
        getWithLogin("$baseUrl/issue/$key?expand=changelog", callback) {
            callback(Result.Success(it))
        }
    }

    fun getEditmeta(key: String, callback: (Result<Editmeta>) -> Unit) {
        getWithLogin("$baseUrl/issue/$key/editmeta", callback) {
            callback(Result.Success(it))
        }
    }

    fun getWatchers(key: String, callback: (Result<Watchers>) -> Unit) {
        getWithLogin("$baseUrl/issue/$key/watchers", callback) {
            callback(Result.Success(it))
        }
    }

    fun getTransitions(key: String, callback: (Result<Transitions>) -> Unit) {
        getWithLogin("$baseUrl/issue/$key/transitions?expand=transitions.fields", callback) {
            callback(Result.Success(it))
        }
    }

    fun getFilters(callback: (Result<List<Filter>>) -> Unit) {
        getWithLogin("$baseUrl/filter/favourite", callback) {
            callback(Result.Success(defaultFilters.plus(it)))
        }
    }

    fun downloadAttachment(path: String, callback: (Result<File>) -> Unit) {
        downloadWithLogin(path, callback) {
            callback(Result.Success(it))
        }
    }

    private inline fun downloadWithLogin(
        path: String,
        crossinline callback: (Result<File>) -> Unit,
        crossinline onSuccess: (File) -> Unit
    ) {
        val filename = path.split("/").last().split(".")
        val file = File.createTempFile(filename[0], ".${filename.getOrElse(1) { "dat" }}")
        Fuel.download(path)
            .fileDestination { _, _ -> file }
            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
            .response { _, response, result ->
                if (response.isSuccessful && result.component2() != null) {
                    callback(Result.Error(result.component2()?.message ?: ""))
                } else if (response.isSuccessful && result.component1() != null && response.isJsonResponse()) {
                    onSuccess(file)
                } else {
                    withLogin {
                        Fuel.download(path)
                            .fileDestination { _, _ -> file }
                            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
                            .response { _, response1, result1 ->
                                if (response1.isSuccessful && result1.component1() != null) {
                                    onSuccess(file)
                                } else {
                                    callback(Result.Error(result.component2()?.message ?: ""))
                                }
                            }
                    }
                }
            }
    }

    private inline fun <reified T> getWithLogin(
        path: String,
        crossinline callback: (Result<T>) -> Unit,
        crossinline onSuccess: (T) -> Unit
    ) {
        Fuel.get(path)
            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
            .responseObject { _: Request, response: Response, result: com.github.kittinunf.result.Result<T, FuelError> ->
                if (response.isSuccessful && result.component2() != null && response.isJsonResponse()) {
                    callback(Result.Error(result.component2()?.message ?: ""))
                } else if (response.isSuccessful && result.component1() != null && response.isJsonResponse()) {
                    onSuccess(result.get())
                } else {
                    withLogin {
                        Fuel.get(path)
                            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
                            .responseObject { _: Request, response1: Response, result1: com.github.kittinunf.result.Result<T, FuelError> ->
                                if (response1.isSuccessful && result1.component1() != null && response1.isJsonResponse()) {
                                    onSuccess(result1.get())
                                } else {
                                    callback(Result.Error(result.component2()?.message ?: ""))
                                }
                            }
                    }
                }
            }
    }

    private inline fun <reified T> postWithLogin(
        path: String,
        body: Any,
        crossinline callback: (Result<T>) -> Unit,
        crossinline onSuccess: (T) -> Unit
    ) {
        Fuel.post(path)
            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
            .objectBody(body)
            .responseObject { _: Request, response: Response, result: com.github.kittinunf.result.Result<T, FuelError> ->
                if (response.isSuccessful && result.component2() != null && response.isJsonResponse()) {
                    callback(Result.Error(result.component2()?.message ?: ""))
                } else if (response.isSuccessful && result.component1() != null && response.isJsonResponse()) {
                    onSuccess(result.get())
                } else {
                    withLogin {
                        Fuel.post(path)
                            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
                            .objectBody(body)
                            .responseObject { _: Request, response1: Response, result1: com.github.kittinunf.result.Result<T, FuelError> ->
                                if (response1.isSuccessful && result1.component1() != null && response1.isJsonResponse()) {
                                    onSuccess(result1.get())
                                } else {
                                    callback(Result.Error(result.component2()?.message ?: ""))
                                }
                            }
                    }
                }
            }
    }

    private fun withLogin(action: () -> Unit) {
        Fuel.post(loginUrl)
            .body("username=$user&password=$password&login-form-type=pwd")
            .response { _, response, _ ->
                cookies = response.headers[Headers.SET_COOKIE].toList()
                action.invoke()
            }
    }

    private fun Response.isJsonResponse() = headers[Headers.CONTENT_TYPE].any { it.contains("application/json") }

    companion object {
        private val headFields = listOf(
            "summary",
            "description",
            "reporter",
            "assignee",
            "created",
            "resolutiondate",
            "status",
            "priority",
            "updated"
        )

        val defaultFilters = listOf(
            Filter(
                "-2",
                "My open issues",
                "assignee = currentUser() AND resolution = Unresolved order by updated DESC"
            ),
            Filter("-1", "Reported by me", "reporter = currentUser() order by created DESC")
        )
    }

    data class SearchRequest(
        val jql: String,
        val startAt: Int,
        val maxResults: Int,
        val fields: List<String>
    )
}