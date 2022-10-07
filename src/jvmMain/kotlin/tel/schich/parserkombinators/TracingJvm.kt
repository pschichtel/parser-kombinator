package tel.schich.parserkombinators

private val enableTracing = System.getProperty("tel.schich.rfc5988.parsing.trace", "false").toBoolean()

actual fun isTracingEnabled(): Boolean = enableTracing
