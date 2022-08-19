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
import kotlinx.coroutines.launch
import ui.*

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "JiraViewer",
        state = WindowState(size = DpSize(1540.dp, 800.dp))
    ) {
        val scope = rememberCoroutineScope()
        val errorText = remember { mutableStateOf("") }

        withSettings { settings ->
            val restUrl = remember { mutableStateOf("") }
            val loginFormUrl = remember { mutableStateOf("") }
            val username = remember { mutableStateOf("") }
            val password = remember { mutableStateOf("") }
            settings.restUrl.collectAsState("", scope.coroutineContext).value.let {
                if (it.isNotBlank() && restUrl.value.isBlank()) restUrl.value = it
            }
            settings.loginFormUrl.collectAsState("", scope.coroutineContext).value.let {
                if (it.isNotBlank() && loginFormUrl.value.isBlank()) loginFormUrl.value = it
            }
            settings.username.collectAsState("", scope.coroutineContext).value.let {
                if (it.isNotBlank() && username.value.isBlank()) username.value = it
            }
            settings.password.collectAsState("", scope.coroutineContext).value.let {
                if (it.isNotBlank() && password.value.isBlank()) password.value = it
            }

            MaterialTheme(colors = darkColors(primary = Color(120, 120, 120), onPrimary = Color.White)) {
                if (errorText.value.isNotBlank() || restUrl.value.isBlank() || loginFormUrl.value.isBlank() || username.value.isBlank() || password.value.isBlank()) {
                    ConnectionSettingsInput(restUrl, loginFormUrl, username, password, errorText)
                } else {
                    val repo = JiraRepository(restUrl.value, loginFormUrl.value, username.value, password.value)
                    when (val result = uiStateFrom { clb: (data.api.Result<Myself>) -> Unit -> repo.myself(clb) }.value) {
                        is UiState.Error -> errorText.value = result.exception
                        is UiState.Loading -> FullPageLoader()
                        is UiState.Success -> {
                            scope.launch {
                                settings.setRestUrl(restUrl.value)
                                settings.setLoginFormUrl(loginFormUrl.value)
                                settings.setUsername(username.value)
                                settings.setPassword(password.value)
                            }
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