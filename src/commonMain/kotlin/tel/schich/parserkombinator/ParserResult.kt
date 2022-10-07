package tel.schich.parserkombinator

sealed interface ParserResult<out T> {

    val rest: StringSlice

    data class Ok<T>(val value: T, override val rest: StringSlice) : ParserResult<T>
    data class Error(val message: String, override val rest: StringSlice) : ParserResult<Nothing>
}