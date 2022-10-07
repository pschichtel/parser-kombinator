package tel.schich.parserkombinators

import kotlin.test.Test
import kotlin.test.assertEquals

sealed interface Json {
    sealed interface Number : Json

    data class Object(val value: Map<String, Json>) : Json
    data class Array(val value: List<Json>) : Json
    data class String(val value: kotlin.String) : Json
    data class Integer(val value: Long) : Number
    data class Float(val value: Double) : Number
    data class Bool(val value: Boolean) : Json
    object Null : Json
}

class JsonParserTest {

    @Test
    fun parseJson() {
        assertEquals(Result.Ok(Json.Bool(value = true), StringSlice.of("")), parseJson("true"))
        assertEquals(Result.Ok(Json.Bool(value = false), StringSlice.of("")), parseJson("false"))
        assertEquals(Result.Ok(Json.Null, StringSlice.of("")), parseJson("null"))
        assertEquals(Result.Ok(Json.Integer(0), StringSlice.of("")), parseJson("0"))
        assertEquals(Result.Ok(Json.Integer(11), StringSlice.of("")), parseJson("11"))
        assertEquals(Result.Ok(Json.Float(0.1), StringSlice.of("")), parseJson("0.1"))
        assertEquals(Result.Ok(Json.Float(0.1e1), StringSlice.of("")), parseJson("0.1e1"))
        assertEquals(Result.Ok(Json.Float(0.1e+1), StringSlice.of("")), parseJson("0.1e+1"))
        assertEquals(Result.Ok(Json.Float(0.1e-1), StringSlice.of("")), parseJson("0.1e-1"))
        assertEquals(Result.Ok(Json.Float(-0.1e1), StringSlice.of("")), parseJson("-0.1e1"))
        assertEquals(Result.Ok(Json.String("a"), StringSlice.of("")), parseJson("\"a\""))
        assertEquals(Result.Ok(Json.Array(listOf(Json.Null)), StringSlice.of("")), parseJson("[null]"))
        assertEquals(Result.Ok(Json.Array(listOf(Json.String("a"))), StringSlice.of("")), parseJson("[\"a\"]"))
        assertEquals(Result.Ok(Json.Object(mapOf(Json.String("a") to Json.String("b"))), StringSlice.of("")), parseJson("{\"a\": \"b\"}"))
    }

}