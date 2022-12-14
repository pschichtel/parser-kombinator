package tel.schich.parserkombinator

import kotlin.test.Test
import kotlin.test.assertEquals
import tel.schich.parserkombinator.invoke
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class ParserKtTest {

    @Test
    fun flatMap() {
        val parseDigit = take { it.isDigit() }.map { it.toString().toInt() }
        val result = parseSeparated(parseDigit, take('+'), parseDigit).map { (a, b) -> a + b }("1+1")
        assertIs<ParserResult.Ok<Int>>(result)
        assertEquals(2, result.value)
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun map() {
        val result = take { it.isDigit() }.map { it.toString().toInt() }("1")
        assertIs<ParserResult.Ok<Int>>(result)
        assertEquals(1, result.value)
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun andThenIgnore() {
        val parser = take { it.isDigit() }.map { it.toString().toInt() } then takeWhile { true }
        val result = parser("1++")
        assertIs<ParserResult.Ok<Int>>(result)
        assertEquals(1, result.value)
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun concat() {
        val parser = (take { it.isDigit() } concat take { it.isDigit() })
        val result = parser("11")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("11", result.value.toString())
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun andThenTake() {
        val parser = take { it.isDigit() }.map { it.toString().toInt() }.andThenTake(takeWhile { true })
        val result = parser("1++")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("++", result.value.toString())
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun surroundedBy() {
        val parser = take('+').surroundedBy(take('<'), take('>'))
        val result = parser("<+>")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("+", result.value.toString())
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun or() {
        val parser = (take('+') or take('-')) concat (take('+') or take('-'))
        val result = parser("+-")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("+-", result.value.toString())
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun optional() {
        val parser = take('+') concat take('-').optional()
        run {
            val result = parser("+")
            assertIs<ParserResult.Ok<StringSlice>>(result)
            assertEquals("+", result.value.toString())
            assertTrue(result.rest.isEmpty())
        }
        run {
            val result = parser("+-")
            assertIs<ParserResult.Ok<StringSlice>>(result)
            assertEquals("+-", result.value.toString())
            assertTrue(result.rest.isEmpty())
        }
    }

    @Test
    fun parseSeparated() {
        val parser = parseSeparated(take { it.isDigit() }, take('*'), take { it.isDigit() })
            .map { (a, b) -> a.toString().toInt() * b.toString().toInt() }
        val result = parser("3*5")
        assertIs<ParserResult.Ok<Int>>(result)
        assertEquals(15, result.value)
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun parseSeparatedList() {
        val parser = parseSeparatedList(take { it.isDigit() }.map { it.toString().toInt() }, take('+'))
            .map { it.sum() }
        val result = parser("1+2+3+4+5")
        assertIs<ParserResult.Ok<Int>>(result)
        assertEquals(1+2+3+4+5, result.value)
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun takeFirstLaziness() {
        val parserA = take('a').map { it.toString() }
        val parserB: Parser<String> = { throw Exception() }

        assertWorks {
            val result = takeFirst(parserA, parserB)("a")
            assertIs<ParserResult.Ok<String>>(result)
            assertEquals("a", result.value)
            assertTrue(result.rest.isEmpty())
        }

        assertFailsWith<Exception> {
            takeFirst(parserB, parserA)("a")
        }
    }

    @Test
    fun parseRepeatedly() {
        val result = parseRepeatedly((take('1') concat take('a')).map { it.toString() })("1a1aa")
        assertIs<ParserResult.Ok<List<String>>>(result)
        assertEquals(listOf("1a", "1a"), result.value)
        assertEquals("a", result.rest.toString())
    }

    @Test
    fun takeWhile() {
        run {
            val result = takeWhile { it.isDigit() }("11aa")
            assertIs<ParserResult.Ok<StringSlice>>(result)
            assertEquals("11", result.value.toString())
            assertEquals("aa", result.rest.toString())
        }
        run {
            val result = takeWhile(max = 2) { it.isDigit() }("111aa")
            assertIs<ParserResult.Ok<StringSlice>>(result)
            assertEquals("11", result.value.toString())
            assertEquals("1aa", result.rest.toString())
        }
        run {
            val result = takeWhile(min = 1) { it.isDigit() }("aa")
            assertIs<ParserResult.Error>(result)
            assertEquals("aa", result.rest.toString())
        }
    }

    @Test
    fun takeUntil() {
        val result = takeUntil { !it.isDigit() }("11aa")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("11", result.value.toString())
        assertEquals("aa", result.rest.toString())
    }

    @Test
    fun takePredicate() {
        val result = take { it == '1' }("1a")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("1", result.value.toString())
        assertEquals("a", result.rest.toString())
    }

    @Test
    fun takeChar() {
        val result = take('1')("1a")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("1", result.value.toString())
        assertEquals("a", result.rest.toString())
    }

    @Test
    fun takeString() {
        val result = takeString("1a")("1ab")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("1a", result.value.toString())
        assertEquals("b", result.rest.toString())
    }

    @Test
    fun entireSliceOf() {
        val result = entireSliceOf(take('1') then take('a'))("1a")
        assertIs<ParserResult.Ok<StringSlice>>(result)
        assertEquals("1a", result.value.toString())
        assertTrue(result.rest.isEmpty())
    }
}