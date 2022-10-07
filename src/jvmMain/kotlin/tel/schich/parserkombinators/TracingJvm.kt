package tel.schich.parserkombinators

private val enableTracing = System.getProperty("tel.schich.parser-kombinator.trace", "false").toBoolean()

actual fun isTracingEnabled(): Boolean = enableTracing
