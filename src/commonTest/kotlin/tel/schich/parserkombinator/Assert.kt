package tel.schich.parserkombinator

import kotlin.test.fail

fun <T> assertWorks(block: () -> T): T {
    try {
        return block()
    } catch (t: Throwable) {
        fail("Block didn't work!", t)
    }
}