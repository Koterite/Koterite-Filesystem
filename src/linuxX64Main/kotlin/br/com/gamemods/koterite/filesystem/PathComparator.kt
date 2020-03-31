package br.com.gamemods.koterite.filesystem

import br.com.gamemods.koterite.annotation.InternalApi

@InternalApi
internal object PathComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        val o1StringPart = a.replace("\\d".toRegex(), "")
        val o2StringPart = b.replace("\\d".toRegex(), "")
        return if (o1StringPart.equals(o2StringPart, ignoreCase = false)) {
            extractInt(a) - extractInt(b)
        } else a.compareTo(b)
    }

    private fun extractInt(s: String): Int {
        val num = s.replace("\\D".toRegex(), "")
        // return 0 if no digits found
        return if (num.isEmpty()) 0 else num.toInt()
    }
}
