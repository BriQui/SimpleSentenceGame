package com.example.simplesentencegame

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val monoSpace = FontFamily.Monospace
val DeepSkyBlue = Color(0xFF00BFFF)
val LightGreen = Color(0xFF90EE90)
val monoTextStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = monoSpace // so sentences align on screen for user
)

const val DEBUG = "BQ_DEBUG"

// const val SENTENCES_FILENAME = "sentenceDatabase.json"
// const val VOCAB_FILENAME = "vocab.json"
// const val MAX_SCORE = 10 // # correct answers to gen animation
const val TIME_FACTOR_PER_CHAR = 60
// const val BUTTON_HEIGHT = 35
// val tonedDownButtonColor = Color.Blue.copy(alpha = 0.7f)
const val LOTTIE_SIZE = 400
const val HOME = "HOME"
const val LEARN = "LEARN"
const val PRACTICE_RECALL = "PRACTICE RECALL"
const val PRACTICE_SOURCE = "DUTCH→ENGLISH"
const val PRACTICE_TARGET = "ENGLISH→DUTCH"
const val TEST_CHUNK = "TEST"
// const val STATS = "STATS"
const val VOCAB = "VOCABULARY LEARNED"
const val HOWTO = "HOWTO"
const val EXTRAS = "EXTRAS"
val LEARNING_CYCLE = listOf(LEARN, PRACTICE_RECALL, PRACTICE_SOURCE, PRACTICE_TARGET, TEST_CHUNK)
val NUMBER_OF_LEARNING_OPTIONS = LEARNING_CYCLE.size