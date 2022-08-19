import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import data.api.JiraRepository
import data.api.Myself
import data.local.Settings.Companion.withSettings
import ui.*

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "JiraViewer",
        state = WindowState(size = DpSize(1540.dp, 800.dp))
    ) {
        val errorText = remember { mutableStateOf("") }

        withSettings { settings ->
            MaterialTheme(colors = darkColors(primary = Color(120, 120, 120), onPrimary = Color.White)) {
                if (errorText.value.isNotBlank() || settings.restUrl.isBlank() || settings.loginFormUrl.isBlank() || settings.username.isBlank() || settings.password.isBlank()) {
                    ConnectionSettingsView(settings, errorText)
                } else {
                    val repo = JiraRepository(settings.restUrl, settings.loginFormUrl, settings.username, settings.password)
                    when (val result = uiStateFrom { clb: (data.api.Result<Myself>) -> Unit -> repo.myself(clb) }.value) {
                        is UiState.Error -> errorText.value = result.exception
                        is UiState.Loading -> FullPageLoader()
                        is UiState.Success -> {
                            CompositionLocalProvider(Repository provides repo) {
                                IssuesView()
                            }
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
fun FullPageLoader() {
    Scaffold {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(20.dp)) {
            CircularProgressIndicator()
        }
    }
}