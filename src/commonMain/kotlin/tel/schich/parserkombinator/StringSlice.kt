package tel.schich.parserkombinator

data class StringSlice(val string: String, val offset: Int, override val length: Int) : CharSequence {
    override fun get(index: Int): Char = string[offset + index]

    override fun subSequence(startIndex: Int, endIndex: Int): StringSlice {
        return subSlice(startIndex, endIndex - startIndex)
    }

    fun subSlice(index: Int, length: Int): StringSlice {
        if (index == 0 && length == this.length) {
            return this
        }
        return StringSlice(string, offset + index, length)
    }

    fun subSlice(index: Int): StringSlice {
        if (index == 0) {
            return this
        }
        val end = offset + length
        val newOffset = offset + index
        val newLength = end - newOffset
        //println("newOffset=$newOffset, newLength=$newLength, string=${string.substring(newOffset, newOffset + newLength)}")
        return StringSlice(string, newOffset, newLength)
    }

    override fun toString(): String {
        return string.substring(offset, offset + length)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as StringSlice

        if (length != other.length) return false
        if (string == other.string && offset == other.offset) return true

        for (i in indices) {
            if (this[i] != other[i]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var h = 0
        for (i in indices) {
            h = 31 * h + this[i].code
        }
        return h
    }


    companion object {
        fun of(s: String) = StringSlice(s, 0, s.length)
    }
}