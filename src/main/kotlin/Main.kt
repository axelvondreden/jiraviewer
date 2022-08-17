import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import data.JiraRepository
import ui.IssuesView
import ui.Repository

/**
 * [args]: baseUrl, loginUrl, username, password
 */
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
fun main(args: Array<String>) = application {
    val repo = JiraRepository(args[0], args[1], args[2], args[3])
    Window(
        onCloseRequest = ::exitApplication,
        title = "JiraViewer",
        state = WindowState(size = DpSize(1540.dp, 800.dp))
    ) {
        CompositionLocalProvider(Repository provides repo) {
            IssuesView()
        }
    }
}