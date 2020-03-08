package org.olafneumann.regex.generator.regex

import org.olafneumann.regex.generator.util.HasRange

class RecognizerCombiner {
    companion object {
        fun combine(
            inputText: String,
            selectedMatches: Collection<RecognizerMatch>,
            options: Options
        ): RegularExpression {
            if (selectedMatches.isEmpty()) {
                return RegularExpression(options.getFrame().format(inputText.escapeForRegex()))
            }

            val rangesToMatches = selectedMatches.flatMap { match -> match
                .ranges.mapIndexed { index, range -> RangeToMatch(range, match.patterns[index]) } }
                .sortedBy { it.range.first }
                .toList()

            val first = if (rangesToMatches.first().range.first > 0) {
                if (options.onlyPatterns)
                    ".*"
                else
                    inputText.substring(0, rangesToMatches.first().range.first).escapeForRegex()
            } else {
                ""
            }
            val last = if (rangesToMatches.last().range.last < inputText.length-1) {
                if (options.onlyPatterns)
                    ".*"
                else
                    inputText.substring(rangesToMatches.last().range.last + 1).escapeForRegex()
            } else {
                ""
            }

            val pattern = buildString {
                append(first)
                for (i in rangesToMatches.indices) {
                    if (i > 0) {
                        val range = IntRange(rangesToMatches[i - 1].range.last + 1, rangesToMatches[i].range.first - 1)
                        if (options.onlyPatterns) {
                            append(if(range.isEmpty()) {""} else {".*"})
                        } else {
                            append(inputText.substring(range).escapeForRegex())
                        }
                    }
                    append(rangesToMatches[i].pattern)
                }
                append(last)
            }

            return RegularExpression(options.getFrame().format(pattern))
        }

        private fun String.escapeForRegex() = replace(Regex("([.\\\\^$\\[{}()*?+])"), "\\$1")
    }

    data class Options(
        val onlyPatterns: Boolean = false,
        val matchWholeLine: Boolean = true,
        val caseSensitive: Boolean = true,
        val dotMatchesLineBreaks: Boolean = false,
        val multiline: Boolean = false
    ) {
        internal fun getFrame() = if (matchWholeLine) {
            Frame("^", "$")
        } else {
            Frame("", "")
        }
    }

    internal data class RangeToMatch(
        val range: IntRange,
        val pattern: String
    )

    internal data class Frame(
        val start: String,
        val end: String
    ) {
        internal fun format(pattern: String) = "$start$pattern$end"
    }

    data class RegularExpression(
        val pattern: String
    ) {
        val regex: Regex by lazy { Regex(pattern) }
    }
}