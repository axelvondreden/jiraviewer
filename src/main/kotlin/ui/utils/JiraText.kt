package ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import data.api.JiraRepository
import data.api.Result
import java.awt.Desktop
import java.net.URI

@Composable
fun JiraText(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 14.sp) {
    val repo = Repository.current
    val aText = text.parseJiraText(fontSize, repo)
    SelectionContainer {
        ClickableText(text = aText, modifier = modifier, onClick = { offset ->
            aText.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let {
                Desktop.getDesktop().browse(URI(it.item))
            }
        })
    }
}

private fun String.parseJiraText(fontSize: TextUnit = 14.sp, repo: JiraRepository): AnnotatedString {
    val lines = split("\n").map { it.split(Regex("(\\s+| )")) }
    return AnnotatedString.Builder().apply {
        pushStyle(SpanStyle(color = Color(219, 219, 219), fontSize = fontSize))
        var count = 0
        lines.forEachIndexed { index, line ->
            if (index > 0) {
                append("\n")
                count++
            }
            line.forEach { word ->
                count += when {
                    word.isHyperlink() -> {
                        pushStringAnnotation(tag = "URL", annotation = word)
                        withStyle(style = SpanStyle(color = Color(88, 129, 252))) {
                            append(word)
                        }
                        pop()
                        word.length
                    }

                    word.isMention() -> {
                        val user = word.getUserFromMention()
                        var translated = ""
                        repo.getUser(user) {
                            val displayName = when (it) {
                                is Result.Error -> "[ERROR]"
                                is Result.Success -> it.data.displayName ?: "[ERROR]"
                            }
                            translated = word.replace(Regex("\\[~\\S*]"), displayName)
                        }
                        while (translated.isEmpty()) {
                            Thread.sleep(1L)
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(translated)
                        }
                        translated.length
                    }

                    else -> {
                        append(word)
                        word.length
                    }
                }
                append(" ")
                count++
            }
        }
    }.toAnnotatedString()
}

private fun String.isHyperlink() = startsWith("http")

private fun String.isMention() = contains(Regex("\\[~\\S*]"))

private fun String.getUserFromMention() = dropWhile { it != '~' }.drop(1).takeWhile { it != ']' }