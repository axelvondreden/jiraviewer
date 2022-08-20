package data.api

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import data.local.Settings.Companion.settings
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: String) : Result<Nothing>()
}

class JiraRepository {

    private val baseUrl = settings.restUrl
    private val loginUrl = settings.loginFormUrl
    private val user = settings.username
    private val password = settings.password

    private var cookies: List<String> = emptyList()

    private val userCache = mutableMapOf<String, String>()

    init {
        //DEBUG:
        //FuelManager.instance.addRequestInterceptor(LogRequestInterceptor)
        //FuelManager.instance.addResponseInterceptor(LogResponseInterceptor)
    }

    fun myself(callback: (Result<Myself>) -> Unit) {
        getWithLogin("$baseUrl/myself", callback) {
            callback(Result.Success(it))
        }
    }

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
        getWithLogin("$baseUrl/issue/$key?expand=changelog&fields=-comment", callback) {
            callback(Result.Success(it))
        }
    }

    fun getUser(key: String, callback: (Result<DisplayName>) -> Unit) {
        if (userCache.containsKey(key)) {
            callback(Result.Success(DisplayName(userCache[key]!!)))
        } else {
            getWithLogin("$baseUrl/user?username=$key", callback) {
                userCache[key] = it.displayName ?: ""
                callback(Result.Success(it))
            }
        }
    }

    fun updateIssue(key: String, update: Update, callback: (Result<Boolean>) -> Unit) {
        putWithLogin("$baseUrl/issue/$key", update, callback) {
            callback(Result.Success(true))
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

    fun getComments(key: String, callback: (Result<Comments>) -> Unit) {
        getWithLogin("$baseUrl/issue/$key/comment?expand=properties", callback) {
            callback(Result.Success(it))
        }
    }

    fun getTransitions(key: String, callback: (Result<Transitions>) -> Unit) {
        getWithLogin("$baseUrl/issue/$key/transitions?expand=transitions.fields", callback) {
            callback(Result.Success(it))
        }
    }

    fun doTransition(key: String, transition: Update, callback: (Result<Boolean>) -> Unit) {
        postWithLogin("$baseUrl/issue/$key/transitions", transition, callback) {
            callback(Result.Success(true))
        }
    }

    fun addComment(key: String, text: String, internal: Boolean, callback: (Result<Boolean>) -> Unit) {
        @Suppress("unused")
        val obj = object : Any() {
            val body = text
            val properties = if (internal) listOf(object : Any() {
                val key = "sd.public.comment"
                val value = object : Any() {
                    val internal = true
                }
            }) else null
        }
        postWithLogin("$baseUrl/issue/$key/comment", obj, callback) {
            callback(Result.Success(true))
        }
    }

    fun getFilters(callback: (Result<List<Filter>>) -> Unit) {
        getWithLogin("$baseUrl/filter/favourite", callback) {
            callback(Result.Success(defaultFilters.plus(it)))
        }
    }

    fun download(path: String, callback: (Result<File>) -> Unit) {
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
                    withLogin(callback) {
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
                    withLogin(callback) {
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
                    withLogin(callback) {
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

    private inline fun postWithLogin(
        path: String,
        body: Any,
        crossinline callback: (Result<Boolean>) -> Unit,
        crossinline onSuccess: () -> Unit
    ) {
        Fuel.post(path)
            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
            .objectBody(body)
            .response { _, response, result ->
                if (response.isSuccessful && result.component2() != null && response.isJsonResponse()) {
                    callback(Result.Error(result.component2()?.message ?: ""))
                } else if (response.isSuccessful) {
                    onSuccess()
                } else {
                    withLogin(callback) {
                        Fuel.post(path)
                            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
                            .objectBody(body)
                            .response { _, response, result ->
                                if (response.isSuccessful) {
                                    onSuccess()
                                } else {
                                    callback(Result.Error(result.component2()?.message ?: ""))
                                }
                            }
                    }
                }
            }
    }

    private inline fun putWithLogin(
        path: String,
        body: Any,
        crossinline callback: (Result<Boolean>) -> Unit,
        crossinline onSuccess: () -> Unit
    ) {
        Fuel.put(path)
            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
            .objectBody(body)
            .response { _, response, result ->
                if (response.isSuccessful && result.component2() != null && response.isJsonResponse()) {
                    callback(Result.Error(result.component2()?.message ?: ""))
                } else if (response.isSuccessful) {
                    onSuccess()
                } else {
                    withLogin(callback) {
                        Fuel.put(path)
                            .appendHeader(*cookies.map { Headers.COOKIE to it }.toTypedArray())
                            .objectBody(body)
                            .response { _, response, result ->
                                if (response.isSuccessful) {
                                    onSuccess()
                                } else {
                                    callback(Result.Error(result.component2()?.message ?: ""))
                                }
                            }
                    }
                }
            }
    }

    private inline fun <T> withLogin(crossinline callback: (Result<T>) -> Unit, crossinline action: () -> Unit) {
        val encodedPw = URLEncoder.encode(password, StandardCharsets.UTF_8)
        Fuel.post(loginUrl)
            .body("username=$user&password=$encodedPw&login-form-type=pwd")
            .response { _, response, result ->
                val html = result.get().decodeToString()
                // search for error message on html result like a retard
                val error = html.split("<div class=\"error-msg\">").getOrNull(1)
                    ?.replace("\n", " ")?.replace("\t", " ")
                    ?.replace(Regex("\\s+"), " ")
                    ?.dropWhile { it != '>' }?.drop(1)
                    ?.takeWhile { it != '<' }

                if (error.isNullOrBlank()) {
                    cookies = response.headers[Headers.SET_COOKIE].toList()
                    action()
                } else {
                    callback(Result.Error(error))
                }
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