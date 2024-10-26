package com.example.simplesentencegame

const val DEBUG = "BQ_DEBUG"

// const val SENTENCES_FILENAME = "sentenceDatabase.json"
// const val VOCAB_FILENAME = "vocab.json"

const val TIME_FACTOR_PER_CHAR = 60 // time user has to ponder sentence
const val LOTTIE_SIZE = 400 // image size

const val HOME = "HOME"
const val LEARN = "LEARN"
const val REVIEW = "REVIEW"
const val PRACTICE_SOURCE = "DUTCH→ENGLISH"
const val PRACTICE_TARGET = "ENGLISH→DUTCH"
const val TEST_CHUNK = "TEST"
// const val STATS = "STATS"
const val VOCAB = "VOCABULARY LEARNED"
const val HOWTO = "HOWTO"
const val EXTRAS = "EXTRAS"

val LEARNING_CYCLE = listOf(LEARN, PRACTICE_SOURCE, PRACTICE_TARGET, TEST_CHUNK)
val NUMBER_OF_LEARNING_OPTIONS = LEARNING_CYCLE.size
