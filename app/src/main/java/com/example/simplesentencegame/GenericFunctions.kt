package com.example.simplesentencegame

infix fun String.isGoodMatch(answerSentence: String): Boolean {
    // Trim the answerSentence and remove a trailing period if it exists
    val trimmedAnswer = answerSentence.trimEnd('.').trim()

    // Compare the trimmed answer with the user input, ignoring case
    return this.trim().equals(trimmedAnswer, ignoreCase = true)
}
fun jumbleWords(input: String): String {
    // Remove the ending period if it exists
    val sanitizedInput = input.trimEnd('.')

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