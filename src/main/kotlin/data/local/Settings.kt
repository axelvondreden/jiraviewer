package data.local

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class Settings {

    private var settings: SettingsDTO

    init {
        settings = try {
            checkSettings()
            jacksonObjectMapper().readValue(File(settingsPath).readText(), SettingsDTO::class.java)
        } catch (e: FileNotFoundException) {
            jacksonObjectMapper().readValue(File(settingsDefaultPath).readText(), SettingsDTO::class.java)
        } catch (e: MissingKotlinParameterException) {
            jacksonObjectMapper().readValue(File(settingsDefaultPath).readText(), SettingsDTO::class.java)
        }
    }

    val restUrl: Flow<String> get() = flowOf(settings.restUrl)
    val loginFormUrl: Flow<String> get() = flowOf(settings.loginFormUrl)
    val username: Flow<String> get() = flowOf(settings.username)
    val password: Flow<String> get() = flowOf(settings.password)

    suspend fun setRestUrl(restUrl: String) {
        settings = settings.copy(restUrl = restUrl)
        save()
    }

    suspend fun setLoginFormUrl(loginFormUrl: String) {
        settings = settings.copy(loginFormUrl = loginFormUrl)
        save()
    }

    suspend fun setUsername(username: String) {
        settings = settings.copy(username = username)
        save()
    }

    suspend fun setPassword(password: String) {
        settings = settings.copy(password = password)
        save()
    }

    private fun save() {
        jacksonObjectMapper().writeValue(FileOutputStream(settingsPath), settings)
    }

    companion object {
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

        const val settingsDefaultPath = "config/options.default.json"
        const val settingsPath = "config/options.json"
    }

    data class SettingsDTO(var restUrl: String, var loginFormUrl: String, var username: String, var password: String)

}