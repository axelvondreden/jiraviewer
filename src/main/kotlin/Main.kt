import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

/**
 * [args]: baseUrl, loginUrl, username, password
 */
@ExperimentalMaterialApi
fun main(args: Array<String>) = application {
    val repo = IssueRepository(args[0], args[1], args[2], args[3])
    Window(
        onCloseRequest = ::exitApplication,
        title = "JiraViewer",
        state = WindowState(size = WindowSize(1540.dp, 800.dp))
    ) {
        CompositionLocalProvider(Repository provides repo) {
            IssuesView()
        }
    }
}