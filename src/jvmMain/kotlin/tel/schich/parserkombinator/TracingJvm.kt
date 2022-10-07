package tel.schich.parserkombinator

private val enableTracing = System.getProperty("tel.schich.parser-kombinator.trace", "false").toBoolean()

actual fun isTracingEnabled(): Boolean = enableTracing
