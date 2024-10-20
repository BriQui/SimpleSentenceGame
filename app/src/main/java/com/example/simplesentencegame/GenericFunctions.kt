package com.example.simplesentencegame

infix fun String.isGoodMatch(answerSentence: String): Boolean {
    // Trim the answerSentence and remove a trailing period if it exists
    val trimmedAnswer = answerSentence.trimEnd('.').trim()

    // Compare the trimmed answer with the user input, ignoring case
    return this.trim().equals(trimmedAnswer, ignoreCase = true)
}