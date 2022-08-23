import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.darkColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import data.api.JiraRepository
import data.api.Myself
import data.local.NotificationService
import data.local.Settings.Companion.settings
import ui.*
import ui.utils.FullsizeInfo
import ui.utils.Loader
import ui.utils.UiState
import ui.utils.uiStateFrom

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
fun main() = application {
    val settingsOpened = remember { mutableStateOf(false) }
    if (settingsOpened.value) {
        Window(
            onCloseRequest = { settingsOpened.value = false },
            state = WindowState(position = WindowPosition(Alignment.Center), size = DpSize(800.dp, 1000.dp)),
            title = "JiraViewer - Settings",
            alwaysOnTop = true
        ) {
            MaterialTheme(colors = darkColors(primary = Color(120, 120, 120), onPrimary = Color.White)) {
                SettingsView()
            }
        }
    }
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(position = WindowPosition(Alignment.Center), size = DpSize(1540.dp, 800.dp)),
        title = "JiraViewer"
    ) {
        MaterialTheme(colors = darkColors(primary = Color(120, 120, 120), onPrimary = Color.White)) {
            val errorText = remember { mutableStateOf("") }
            if (errorText.value.isNotBlank() || settings.restUrl.isBlank() || settings.loginFormUrl.isBlank() || settings.username.isBlank() || settings.password.isBlank()) {
                InitialSettingsView(settings, errorText)
            } else {
                val repo = JiraRepository()
                when (val result = uiStateFrom { clb: (data.api.Result<Myself>) -> Unit -> repo.myself(clb) }.value) {
                    is UiState.Error -> errorText.value = result.exception
                    is UiState.Loading -> Scaffold { FullsizeInfo { Loader() } }
                    is UiState.Success -> {
                        val notificationService = NotificationService(repo)
                        CompositionLocalProvider(Repository provides repo, NotificationService provides notificationService) {
                            MainView(onSettings = { settingsOpened.value = true })
                        }
                    }
                }
            }
        }
    }
}
