package de.emailspeicher.zufallsklingelton

data class Track(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val order: Long,
)

enum class SwitchMode {
    DAILY,
    AFTER_INCOMING_CALL,
}
