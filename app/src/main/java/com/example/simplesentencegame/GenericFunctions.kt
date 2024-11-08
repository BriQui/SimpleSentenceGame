package com.example.simplesentencegame

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun todaySimpleFormat(): String {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

infix fun String.isGoodMatch(input: String): Boolean {
    // remove trailing spaces and period
    val trimmedInput = input.trim().trimEnd('.')

    // compare trimmed string with trimmed input
    return this.trim().trimEnd('.').equals(trimmedInput, ignoreCase = true)
}
fun jumbleWords(input: String): String {
    // Remove end spaces and period if these exist
    val sanitizedInput = input.trim().trimEnd('.')

    // Split the input string into words
    val words = sanitizedInput.split(" ").toMutableList()

    if (words.size < 2) {
        throw IllegalArgumentException("jumbleWords: Input string must contain at least 2 words.")
    } else {
        // Shuffle the words until the order is different from the original
        do {
            words.shuffle()
        } while (words.joinToString(" ") == sanitizedInput)
    }
    // Join the shuffled words into a new string
    return words.joinToString(" ")
}