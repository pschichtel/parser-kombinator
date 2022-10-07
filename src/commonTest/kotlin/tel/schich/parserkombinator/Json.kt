package tel.schich.parserkombinator

fun parseJsonString(slice: StringSlice): ParserResult<Json.String> {
    if (slice.isEmpty()) {
        return ParserResult.Error("Expected string, but got nothing", slice)
    }
    val first = slice[0]
    if (first != '"') {
        return ParserResult.Error("Strings must start with a \", but started with $first", slice)
    }

    val string = StringBuilder()
    var verbatim = false
    var i = 1
    while (true) {
        if (i >= slice.length) {
            return ParserResult.Error("Reached end of input", slice)
        }
        val c = slice[i]
        when {
            verbatim -> {
                string.append(c)
                verbatim = false
            }
            c == '\\' -> {
                verbatim = true
            }
            c == '"' -> {
                return ParserResult.Ok(Json.String(string.toString()), slice.subSlice(i + 1))
            }
            else -> {
                string.append(c)
            }
        }
        i += 1
    }
}

val parseMinus = take('-')
val parsePlus = take('+')
fun parseDigits(min: Int) = takeWhile(min = min) { it.isDigit() }
val parseInteger = parseMinus.optional().concat(take('0').or(take { it.isDigit() && it != '0'}.concat(parseDigits(min = 0))))
val parseExponent = take { it == 'e' || it == 'E' }.concat(parseMinus.or(parsePlus).optional()).concat(parseInteger)
val parseDouble = parseInteger.concat(take('.').concat(parseDigits(min = 1)).concat(parseExponent.optional()))

val parseJsonNumber = parseDouble.map { Json.Float(it.toString().toDouble()) }.or(parseInteger.map { Json.Integer(it.toString().toLong()) })
val parseJsonNull = takeString("null").map { Json.Null }
val parseJsonBool = takeString("true").map { Json.Bool(value = true) }.or(takeString("false").map { Json.Bool(value = false) })
val parseWhitespace = takeWhile { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
val parseCommaSeparator = parseWhitespace.andThenTake(take(',')).then(parseWhitespace)

fun parseJsonObjectEntry(input: StringSlice): ParserResult<Pair<Json.String, Json>> = ::parseJsonString
    .andThenIgnore(parseWhitespace)
    .andThenIgnore(take(':'))
    .andThenIgnore(parseWhitespace)
    .flatMap { key -> parseJson.map { Pair(key, it) } }
    .invoke(input)

val parseJsonObject = take('{').then(parseWhitespace)
    .andThenTake(parseSeparatedList(::parseJsonObjectEntry, parseCommaSeparator).map { Json.Object(it.toMap()) })
    .then(parseWhitespace).then(take('}'))

fun parseJsonArray(input: StringSlice): ParserResult<Json.Array> = take('[').then(parseWhitespace)
    .andThenTake(parseSeparatedList(parseJson, parseCommaSeparator).map { Json.Array(it) })
    .then(parseWhitespace).then(take(']'))
    .invoke(input)

val parseJson = parseJsonObject.or(::parseJsonArray).or(parseJsonNumber).or(parseJsonBool).or(::parseJsonString).or(parseJsonNull)