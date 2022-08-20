package ui

import ErrorText
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import data.local.Settings

@Composable
fun ConnectionSettingsView(settings: Settings, errorText: MutableState<String>) {
    Scaffold {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            var restUrlLocal by remember { mutableStateOf(settings.restUrl) }
            TextField(
                value = restUrlLocal,
                onValueChange = { restUrlLocal = it },
                modifier = Modifier.width(600.dp),
                label = { Text("Jira API URL") },
                placeholder = { Text("https://jira-example-domain.com/jira/rest/api/latest") },
                isError = restUrlLocal.isBlank(),
                singleLine = true
            )
            var loginFormUrlLocal by remember { mutableStateOf(settings.loginFormUrl) }
            TextField(
                value = loginFormUrlLocal,
                onValueChange = { loginFormUrlLocal = it },
                modifier = Modifier.width(600.dp).padding(top = 20.dp),
                label = { Text("Jira Login Form URL") },
                placeholder = { Text("https://jira-example-domain.com/pkmslogin.form") },
                isError = loginFormUrlLocal.isBlank(),
                singleLine = true
            )
            var usernameLocal by remember { mutableStateOf(settings.username) }
            TextField(
                value = usernameLocal,
                onValueChange = { usernameLocal = it },
                modifier = Modifier.width(600.dp).padding(top = 20.dp),
                label = { Text("Username") },
                placeholder = { Text("Horst") },
                isError = usernameLocal.isBlank(),
                singleLine = true
            )
            var passwordLocal by remember { mutableStateOf(settings.password) }
            TextField(
                value = passwordLocal,
                onValueChange = { passwordLocal = it },
                modifier = Modifier.width(600.dp).padding(top = 20.dp),
                label = { Text("Password") },
                isError = passwordLocal.isBlank(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Button(
                onClick = {
                    settings.restUrl = restUrlLocal
                    settings.loginFormUrl = loginFormUrlLocal
                    settings.username = usernameLocal
                    settings.password = passwordLocal
                    errorText.value = ""
                },
                modifier = Modifier.padding(top = 20.dp),
                enabled = restUrlLocal.isNotBlank() && loginFormUrlLocal.isNotBlank() && usernameLocal.isNotBlank() && passwordLocal.isNotBlank()
            ) {
                Text("Connect")
            }
            if (errorText.value.isNotBlank()) {
                ErrorText(errorText.value)
            }
        }
    }
}
