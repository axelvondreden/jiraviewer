package data.local

import androidx.compose.runtime.mutableStateOf
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class Settings private constructor() {

    var restUrl: String
        get() = _restUrl.value
        set(value) {
            _restUrl.value = value
            settingsDto.restUrl = value
            save()
        }

    var loginFormUrl: String
        get() = _loginFormUrl.value
        set(value) {
            _loginFormUrl.value = value
            settingsDto.loginFormUrl = value
            save()
        }

    var username: String
        get() = _username.value
        set(value) {
            _username.value = value
            settingsDto.username = value
            save()
        }

    var password: String
        get() = _password.value
        set(value) {
            _password.value = value
            settingsDto.password = value
            save()
        }

    var commentView: CommentViewFilter
        get() = CommentViewFilter.valueOf(_commentView.value)
        set(value) {
            _commentView.value = value.name
            settingsDto.commentView = value.name
            save()
        }

    var commentAscending: Boolean
        get() = _commentAscending.value
        set(value) {
            _commentAscending.value = value
            settingsDto.commentAscending = value
            save()
        }

    private val _restUrl = mutableStateOf(settingsDto.restUrl)
    private val _loginFormUrl = mutableStateOf(settingsDto.loginFormUrl)
    private val _username = mutableStateOf(settingsDto.username)
    private val _password = mutableStateOf(settingsDto.password)
    private val _commentView = mutableStateOf(settingsDto.commentView)
    private val _commentAscending = mutableStateOf(settingsDto.commentAscending)

    private fun save() {
        jacksonObjectMapper().writeValue(FileOutputStream(settingsPath), settingsDto)
    }

    companion object {

        private const val settingsDefaultPath = "config/settings.default.json"
        const val settingsPath = "config/settings.json"

        private val settingsDto: SettingsDTO = try {
            checkSettings()
            jacksonObjectMapper().readValue(File(settingsPath).readText(), SettingsDTO::class.java)
        } catch (e: FileNotFoundException) {
            jacksonObjectMapper().readValue(File(settingsDefaultPath).readText(), SettingsDTO::class.java)
        } catch (e: MissingKotlinParameterException) {
            jacksonObjectMapper().readValue(File(settingsDefaultPath).readText(), SettingsDTO::class.java)
        }

        val settings = Settings()

        private fun checkSettings() {
            val objectMapper = jacksonObjectMapper()
            val defaultTree = objectMapper.readTree(File(settingsDefaultPath))
            val json = File(settingsPath)
            if (!json.exists()) {
                json.createNewFile()
                objectMapper.writeValue(FileOutputStream(settingsPath), defaultTree)
            } else {
                val userTree = objectMapper.readTree(File(settingsPath))
                if (defaultTree != userTree) {
                    updateSettings(defaultTree, userTree)
                    objectMapper.writeValue(FileOutputStream(settingsPath), userTree)
                }
            }
        }

        private fun updateSettings(default: JsonNode, user: JsonNode) {
            default.fields().forEach { field ->
                if (!user.has(field.key)) {
                    (user as ObjectNode).set<ObjectNode>(field.key, field.value)
                } else if (field.value.isContainerNode) {
                    updateSettings(field.value, user[field.key])
                }
            }
        }
    }

    data class SettingsDTO(
        var restUrl: String,
        var loginFormUrl: String,
        var username: String,
        var password: String,
        var commentView: String,
        var commentAscending: Boolean
    )

    enum class CommentViewFilter {
        COMMENTS, HISTORY, ALL
    }
}