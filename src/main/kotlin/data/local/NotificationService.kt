package data.local

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.*

class NotificationService {

    private val _notifications = mutableListOf<Notification>()

    val notifications = flow {
        while (true) {
            emit(_notifications.toList())
            delay(5000)
        }
    }

    init {
        GlobalScope.launch {
            while (true) {
                _notifications += Notification("Notify", Date())
                delay(5000)
            }
        }
    }
}