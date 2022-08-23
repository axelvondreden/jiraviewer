import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import data.api.JiraRepository
import data.api.Myself
import data.local.NotificationService
import data.local.Settings.Companion.settings
import ui.*

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
fun main() = application {
    var settingsOpened by remember { mutableStateOf(false) }
    if (settingsOpened) {
        Window(onCloseRequest = { settingsOpened = false }, title = "JiraViewer - Settings", state = WindowState(position = WindowPosition(Alignment.Center), size = DpSize(800.dp, 1000.dp))) {
            MaterialTheme(colors = darkColors(primary = Color(120, 120, 120), onPrimary = Color.White)) {
                SettingsView()
            }
        }
    }
    Window(onCloseRequest = ::exitApplication, title = "JiraViewer", state = WindowState(position = WindowPosition(Alignment.Center), size = DpSize(1540.dp, 800.dp))) {
        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item("Settings", onClick = { settingsOpened = true }, shortcut = KeyShortcut(Key.S, ctrl = true, alt = true))
            }
        }
        MaterialTheme(colors = darkColors(primary = Color(120, 120, 120), onPrimary = Color.White)) {
            val errorText = remember { mutableStateOf("") }
            if (errorText.value.isNotBlank() || settings.restUrl.isBlank() || settings.loginFormUrl.isBlank() || settings.username.isBlank() || settings.password.isBlank()) {
                ConnectionSettingsView(settings, errorText)
            } else {
                val repo = JiraRepository()
                when (val result = uiStateFrom { clb: (data.api.Result<Myself>) -> Unit -> repo.myself(clb) }.value) {
                    is UiState.Error -> errorText.value = result.exception
                    is UiState.Loading -> Scaffold { FullsizeInfo { Loader() } }
                    is UiState.Success -> {
                        val notificationService = NotificationService(repo)
                        CompositionLocalProvider(Repository provides repo, NotificationService provides notificationService) {
                            IssuesView()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Loader() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorText(err: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(text = err, style = TextStyle(color = MaterialTheme.colors.error, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun FullsizeInfo(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}