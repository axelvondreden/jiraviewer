package data.local

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
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

    val projects: SettingsCollection<String> = SettingsCollection(mutableStateListOf(*settingsDto.projects.toTypedArray())) {
        settingsDto.projects = it
        save()
    }

    var updateStrategy: UpdateStrategy
        get() = UpdateStrategy.valueOf(_updateStrategy.value)
        set(value) {
            _updateStrategy.value = value.name
            settingsDto.updateStrategy = value.name
            save()
        }

    var updateInterval: Int
        get() = _updateInterval.value
        set(value) {
            _updateInterval.value = value
            settingsDto.updateInterval = value
            save()
        }

    var updateOffset: Int
        get() = _updateOffset.value
        set(value) {
            _updateOffset.value = value
            settingsDto.updateOffset = value
            save()
        }

    var updateIncludeOwn: Boolean
        get() = _updateIncludeOwn.value
        set(value) {
            _updateIncludeOwn.value = value
            settingsDto.updateIncludeOwn = value
            save()
        }

    private val _restUrl = mutableStateOf(settingsDto.restUrl)
    private val _loginFormUrl = mutableStateOf(settingsDto.loginFormUrl)
    private val _username = mutableStateOf(settingsDto.username)
    private val _password = mutableStateOf(settingsDto.password)
    private val _commentView = mutableStateOf(settingsDto.commentView)
    private val _commentAscending = mutableStateOf(settingsDto.commentAscending)
    private val _updateStrategy = mutableStateOf(settingsDto.updateStrategy)
    private val _updateInterval = mutableStateOf(settingsDto.updateInterval)
    private val _updateOffset = mutableStateOf(settingsDto.updateOffset)
    private val _updateIncludeOwn = mutableStateOf(settingsDto.updateIncludeOwn)

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
        var commentAscending: Boolean,
        var projects: List<String>,
        var updateStrategy: String,
        var updateInterval: Int,
        var updateOffset: Int,
        var updateIncludeOwn: Boolean
    )

    enum class CommentViewFilter(val title: String) {
        COMMENTS("Comments"), HISTORY("History"), ALL("All")
    }

    enum class UpdateStrategy(val title: String, val description: String) {
        NONE("None", "Does nothing"),
        TABS("Tabs", "Periodically checks opened tabs for changes"),
        FILTER("Filter", "Periodically checks all issues from the currently selected filter")
    }

    class SettingsCollection<T>(private val items: SnapshotStateList<T>, private val onChange: (List<T>) -> Unit) : MutableList<T> {

        override fun clear() {
            items.clear()
            onChange(items)
        }

        override fun add(index: Int, element: T) {
            items.add(index, element)
            onChange(items)
        }

        override val size get() = items.size
        override fun addAll(elements: Collection<T>) = items.addAll(elements).also { if (it) onChange(items) }
        override fun addAll(index: Int, elements: Collection<T>) = items.addAll(index, elements).also { if (it) onChange(items) }
        override fun add(element: T) = items.add(element).also { onChange(items) }
        override fun containsAll(elements: Collection<T>) = items.containsAll(elements)
        override fun contains(element: T) = items.contains(element)
        override fun get(index: Int) = items[index]
        override fun isEmpty() = items.isEmpty()
        override fun iterator() = items.iterator()
        override fun listIterator() = items.listIterator()
        override fun listIterator(index: Int) = items.listIterator(index)
        override fun removeAt(index: Int) = items.removeAt(index).also { onChange(items) }
        override fun subList(fromIndex: Int, toIndex: Int) = items.subList(fromIndex, toIndex)
        override fun set(index: Int, element: T) = items.set(index, element)
        override fun retainAll(elements: Collection<T>) = items.retainAll(elements).also { onChange(items) }
        override fun removeAll(elements: Collection<T>) = items.removeAll(elements).also { onChange(items) }
        override fun remove(element: T) = items.remove(element).also { onChange(items) }
        override fun lastIndexOf(element: T) = items.lastIndexOf(element)
        override fun indexOf(element: T) = items.indexOf(element)
    }
}