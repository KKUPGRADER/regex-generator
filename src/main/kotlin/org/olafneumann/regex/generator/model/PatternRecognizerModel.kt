package org.olafneumann.regex.generator.model

import dev.andrewbailey.diff.differenceOf
import org.olafneumann.regex.generator.regex.RecognizerMatchCombiner
import org.olafneumann.regex.generator.recognizer.RecognizerMatch
import org.olafneumann.regex.generator.recognizer.RecognizerRegistry
import org.olafneumann.regex.generator.regex.RecognizerMatchCombinerOptions
import org.olafneumann.regex.generator.regex.RegularExpression
import org.olafneumann.regex.generator.util.hasIntersectionWith

data class PatternRecognizerModel(
    val input: String,
    val recognizerMatches: List<RecognizerMatch> = RecognizerRegistry.findMatches(input),
    val selectedRecognizerMatches: Collection<RecognizerMatch> = emptySet(),
    val recognizerMatchCombinerOptions: RecognizerMatchCombinerOptions,
    val regularExpression: RegularExpression = RecognizerMatchCombiner
        .combineMatches(
            inputText = input,
            selectedMatches = selectedRecognizerMatches,
            options = recognizerMatchCombinerOptions
        )
) {
    fun setUserInput(newInput: String): PatternRecognizerModel {
        val newMatches = RecognizerRegistry.findMatches(newInput)

        // check how the input string has changed in regard to the old string
        val inputDiffs = differenceOf(
            original = this.input.toCharArray().toList(),
            updated = newInput.toCharArray().toList(),
            detectMoves = false
        )

        // generate pseudo matches, that resemble possible matches after applying the changes
        val newSelectedMatches = this.selectedRecognizerMatches
            .map { AugmentedRecognizerMatch(original = it) }
            .flatMap { it.applyAll(inputDiffs.operations) }
            // see if the augmented matches are still present in the new list of matches -> the new selection
            .mapNotNull { augmentedMatch -> newMatches.firstOrNull { newMatch -> augmentedMatch.equals(newMatch) } }
            .filter { newMatches.contains(it) }
            .toSet()

        val newRegex = RecognizerMatchCombiner
            .combineMatches(
                inputText = newInput,
                selectedMatches = selectedRecognizerMatches,
                options = recognizerMatchCombinerOptions
            )

        return copy(
            input = newInput,
            recognizerMatches = newMatches,
            selectedRecognizerMatches = newSelectedMatches,
            regularExpression = newRegex
        )
    }

    fun select(match: RecognizerMatch): PatternRecognizerModel {
        // make sure, the selection is valid
        if (!recognizerMatches.contains(match)) {
            return this
        }

        // make sure, the selection is valid
        val alreadySelectedRanges = selectedRecognizerMatches.flatMap { it.ranges }
        match.ranges.forEach { rangeOfNewMatch ->
            val hasIntersection = alreadySelectedRanges.firstOrNull { it.hasIntersectionWith(rangeOfNewMatch) } != null
            if (hasIntersection) {
                return this
            }
        }

        val newSelection = selectedRecognizerMatches + match
        val newRegex = RecognizerMatchCombiner
            .combineMatches(inputText = input, selectedMatches = newSelection, options = recognizerMatchCombinerOptions)

        return copy(
            selectedRecognizerMatches = newSelection,
            regularExpression = newRegex
        )
    }

    fun deselect(match: RecognizerMatch): PatternRecognizerModel {
        val newSelection = selectedRecognizerMatches - match
        val newRegex = RecognizerMatchCombiner
            .combineMatches(inputText = input, selectedMatches = newSelection, options = recognizerMatchCombinerOptions)

        return copy(
            selectedRecognizerMatches = newSelection,
            regularExpression = newRegex
        )
    }

    fun setRecognizerMatchCombinerOptions(options: RecognizerMatchCombinerOptions): PatternRecognizerModel {
        return copy(
            recognizerMatchCombinerOptions = options,
            regularExpression = RecognizerMatchCombiner
                .combineMatches(inputText = input, selectedMatches = selectedRecognizerMatches, options = options)
        )
    }
}
