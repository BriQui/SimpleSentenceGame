@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.simplesentencegame
import android.database.sqlite.SQLiteDatabase
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// Data classes populated by SQL are in SqlStuff.kt
// e.g. FlashCard, VocabCard.
// Constants are in Constants.kt
// GuiStuff.kt contains simple composables, etc.

/*
enum class Article(val value: Int) {
    DE("de"), HET("het"), EMPTY("") }
enum class WordType(val value: Int) {
    NOUN("noun"), VERB("verb"), ADJECTIVE("adjective"), ADVERB("adverb"), PRONOUN("pronoun"), PREPOSITION("preposition"), CONJUNCTION("conjunction"), INTERJECTION("interjection") }
;
    companion object {
        // Function to get the WordType by its integer value
        fun fromValue(value: Int): WordType? {
            return entries.find { it.value == value }
        }
    }
}
// Standalone function to get the WordType as a string
fun getWordTypeAsString(value: Int): String? {
    return WordType.fromValue(value)?.name
}
*/

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var db: SQLiteDatabase

    private fun String.speak(tts: TextToSpeech) {
        tts.speak(this, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(DEBUG, "MainActivity: onCreate: ok.")

        // Initialize TextToSpeech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("nl", "NL")) // Set to Dutch
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.d(DEBUG, "Dutch not supported")
                    throw Exception("Dutch not supported")
                }
            }
        }

        // set up database
        val dbHelper = MyDatabaseHelper(this)
        val db: SQLiteDatabase = dbHelper.readableDatabase

        // Call the function to list tables
        val tables = listTables(db)
        Log.d(DEBUG, "MainActivity: db tables → $tables")

        // get previous session details
        var (currentChunkId, flashCardPosition) = getLastSessionInfo(db)

        // get sentences from database
        var records = loadChunkFromDb(db, currentChunkId)
        if (records.isEmpty()) throw Exception("MainActivity: No sentences found in db!!!")

        // get vocab from db
        val vocab = loadWords(db)
        if (vocab.isEmpty()) throw Exception("MainActivity: Words table is empty!!!")

        // main menu
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = HOME) {
                composable(HOME) {
                    BoxWithConstraints(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val buttonWidth = maxWidth * 2 / 3

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // App name header and app image
                            HeaderWithImage(stringResource(id = R.string.app_name), DeepSkyBlue, true)

                            SpacerHeight(dp = 12)

                            val buttonConfigurations = listOf(
                                ButtonConfig(REVIEW, { navController.navigate(REVIEW) }, DeepSkyBlue),
                                ButtonConfig(LEARN, { navController.navigate(LEARN) }, LightGreen),
                                ButtonConfig(PRACTICE_SOURCE, { navController.navigate(PRACTICE_SOURCE) }, LightGreen),
                                ButtonConfig(PRACTICE_TARGET, { navController.navigate(PRACTICE_TARGET) }, LightGreen),
                                ButtonConfig(TEST_CHUNK, { navController.navigate(TEST_CHUNK) }, LightGreen),
                                ButtonConfig(VOCAB, { navController.navigate(VOCAB) }, DeepSkyBlue),
                                ButtonConfig(EXTRAS, { navController.navigate(EXTRAS) }, DeepSkyBlue),
                                ButtonConfig(HOWTO, { navController.navigate(HOWTO) }, DeepSkyBlue)
                            )

                            buttonConfigurations.forEach { buttonConfig ->
                                if (buttonConfig.text != REVIEW || currentChunkId > 0) {
                                    MenuButton(
                                        onClick = buttonConfig.onClick,
                                        text = buttonConfig.text,
                                        buttonWidth = buttonWidth,
                                        buttonColor = buttonConfig.color
                                    )
                                }
                            }
                        }
                    }
                }
                composable(REVIEW) {
                    LearnSentences(REVIEW,
                        navController, records, this@MainActivity) { text -> text.speak(tts) }
                }
                composable(LEARN) {
                    LearnSentences(LEARN,
                        navController, records, this@MainActivity) { text -> text.speak(tts) }
                }
                composable(PRACTICE_SOURCE) {
                    LearnSentences(PRACTICE_SOURCE,
                        navController, records, this@MainActivity) { text -> text.speak(tts) }
                }
                composable(PRACTICE_TARGET) {
                    LearnSentences(PRACTICE_TARGET,
                        navController, records, this@MainActivity) { text -> text.speak(tts) }
                }
                composable(TEST_CHUNK) {
                    TestChunk(
                        learningOption = TEST_CHUNK,
                        navController = navController,
                        records = records,
                        chunkId = currentChunkId
                    ) { nextChunkId ->
                        currentChunkId = nextChunkId
                        records = loadChunkFromDb(db, currentChunkId)
                    }
                }
                composable(VOCAB) {
                    VocabScreen(navController, vocab)
                }
                composable(EXTRAS) {
                    ExtrasScreen(navController)
                }
                composable(HOWTO) {
                    HowToScreen(navController)
                }
            }
        }
    }

    // Clean up TextToSpeech resources when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        db.close()
    }
}

@ExperimentalMaterial3Api
@Composable
fun LearnSentences(
    learningOption: String,
    navController: NavController,
    records: List<FlashCard>,
    context: MainActivity,
    speak: (String) -> Unit
) {
    // State variables
    var currentRecordIndex by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var showTickMark by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var showLottieAnimation by remember { mutableStateOf(false) }
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.Asset("well_done.json"))
    var spoken by remember { mutableStateOf(false) }
    var flashAnswerSentence by remember { mutableStateOf(true) }
    var refreshButton by remember { mutableIntStateOf(0) }
    var showNextSentenceButton by remember { mutableStateOf(false) }
    var showGoToNextOptionButton by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Ensure valid currentRecordIndex within records range
    val currentRecord = if (records.isNotEmpty()) records[currentRecordIndex] else null
    val sourceSentence = currentRecord!!.sourceSentence // should never throw exception
    val gameSentence = currentRecord.gameSentence
    val translation = currentRecord.translation
    var answerSentence = ""

    // Define the next learning option based on the current one
    val nextLearningOptionIndex = (LEARNING_CYCLE.indexOf(learningOption) + 1) % NUMBER_OF_LEARNING_OPTIONS
    val nextLearningOption = LEARNING_CYCLE[nextLearningOptionIndex]

    val timeFactorPerChar = TIME_FACTOR_PER_CHAR // how long to display sentence, i.e. delay.
    val calculatedDelay = sourceSentence.length.times(timeFactorPerChar)

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(gameSentence, refreshButton) {
        flashAnswerSentence = true
        delay(calculatedDelay.toLong())
        flashAnswerSentence = false
    }

    // cause recompose
    fun onRefresh() {
        refreshButton += 1
        speak(answerSentence)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                navController = navController,
                learningOption = learningOption
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // display the appropriate learning option
                when (learningOption) {
                    LEARN -> {
                        HeaderWithImage(headerText = LEARN, showSecondaryInfo = false)
                        answerSentence = sourceSentence
                        if (!spoken) {
                            speak(sourceSentence)
                            spoken = true
                        }
                        SentenceDisplay(
                            testSentence = gameSentence,
                            answerSentence = answerSentence,
                            flashAnswerSentence = flashAnswerSentence, // flash answer briefly
                            showRefreshButton = true,
                            onRefresh = { onRefresh() },
                            textStyle = monoTextStyle
                        )
                    }
                    REVIEW -> {
                        HeaderWithImage(headerText = REVIEW, showSecondaryInfo = false)
                        answerSentence = sourceSentence
                        if (!spoken) {
                            speak(sourceSentence)
                            spoken = true
                        }
                        SentenceDisplay(
                            testSentence = gameSentence,
                            answerSentence = answerSentence,
                            flashAnswerSentence = false,
                            showRefreshButton = true,
                            onRefresh = { onRefresh() },
                            textStyle = monoTextStyle
                        )
                    }
                    PRACTICE_SOURCE -> {
                        HeaderWithImage(headerText = PRACTICE_SOURCE, showSecondaryInfo = false)
                        answerSentence = translation
                        if (!spoken) {
                            speak(sourceSentence)
                            spoken = true
                        }
                        SentenceDisplay(
                            testSentence = sourceSentence,
                            answerSentence = answerSentence,
                            flashAnswerSentence = false,
                            onRefresh = { onRefresh() },
                            showRefreshButton = false,
                            textStyle = monoTextStyle
                        )
                    }
                    PRACTICE_TARGET -> {
                        HeaderWithImage(headerText = PRACTICE_TARGET, showSecondaryInfo = false)
                        answerSentence = sourceSentence
                        if (!spoken) {
                            speak(sourceSentence)
                            spoken = true
                        }
                        SentenceDisplay(
                            testSentence = translation,
                            answerSentence = answerSentence,
                            flashAnswerSentence = false,
                            showRefreshButton = false,
                            onRefresh = { onRefresh() },
                            textStyle = monoTextStyle
                        )
                    }
                    else -> {
                        Log.d(DEBUG,"bad learning option")
                        throw Exception("bad learning option")
                    }
                }

                // user input
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("Enter full sentence here…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = monoTextStyle
                )

                // check user input, if good→reward + Next sentence button else beep
                SpacerHeight(dp = 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (showNextSentenceButton) {
                        StandardButton(
                            onClick = {
                                // Move to the next sentence
                                currentRecordIndex = (currentRecordIndex + 1) % records.size
                                userInput = ""
                                spoken = false
                                showTickMark = false
                                showNextSentenceButton = false // Hide "Next sentence" button
                            },
                            text = "Next sentence"
                        )
                        if (showGoToNextOptionButton) {
                            SpacerHeight(dp = 8) // Spacing between buttons
                            StandardButton(
                                onClick = {
                                    navController.navigate(nextLearningOption) // Navigate to the next learning option
                                },
                                text = "Go to $nextLearningOption"
                            )
                        }

                    } else {
                        StandardButton(
                            onClick = {
                                // if (userInput.trim() == answerSentence) {
                                // ignore whitespace and ending period
                                if (userInput.isGoodMatch(answerSentence)) {
                                    showTickMark = true
                                    speak(sourceSentence)
                                    coroutineScope.launch {
                                        score += 1
//                                        if (score >= MAX_SCORE) {
                                        if (score >= records.size) {
                                            showLottieAnimation = true
                                            showGoToNextOptionButton = true
                                        }
                                        showNextSentenceButton = true
                                    }
                                } else {
                                    showToastWithBeep(context, "Try again!", playNiceBeep = false)
                                }
                            },
                            text = "Submit"
                        )
                    }
                }
                // show translation if applicable for learning option
                if (learningOption == LEARN
                    || learningOption == REVIEW) {
                    SpacerHeight()
                    OutlinedTextField(
                        value = translation,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = monoTextStyle
                    )
                }

                SpacerHeight()
//                val progress = score / MAX_SCORE.toFloat()
                val progress = score / records.size.toFloat()
                // Text(text = "${(progress * 100).toInt()}%")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(2.dp, Color.Black)
                        .fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(25.dp),
                        color = LightGreen,
                    )
                    Text(
                        // text = if (score > 0) "$score correct" else "",
                        text = if (score > 0) "${(progress * 100).toInt()}%" else "",
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Center))
                }

                SpacerHeight()
                if (showTickMark) {
                    Image(
                        painter = painterResource(id = R.drawable.tick_mark),
                        contentDescription = "Correct Answer",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(top = 16.dp)
                    )
                }
            }

            // for N correct answers, reward with animation
            if (showLottieAnimation) {
                LaunchedEffect(Unit) {
                    delay(1500) // show animation briefly
                    showLottieAnimation = false
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                        .padding(top = 32.dp)
                ) {
                    LottieAnimation(
                        composition = lottieComposition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier
                            .size(LOTTIE_SIZE.dp)
                            .align(Alignment.TopCenter)
                    )
                    Text(
//                        text = "You got $MAX_SCORE correct… very cool!",
                        text = "All correct!",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun TestChunk(
    learningOption: String,
    navController: NavController,
    records: List<FlashCard>,
    chunkId: Int,
    loadNextChunk: (Int) -> Unit // Callback for loading the next chunk
) {
    // State variables
    val focusRequesterJumbled = remember { FocusRequester() }
    val focusRequesterTranslation = remember { FocusRequester() }

    var currentRecordIndex by remember { mutableStateOf(0) }
    var currentQuestion by remember { mutableStateOf(0) } // 0 = jumbled sentence, 1 = source sentence
    var userInputJumbled by remember { mutableStateOf("") }
    var userInputTranslation by remember { mutableStateOf("") }
    var totalScore by remember { mutableStateOf(0f) } // Total score for the test
    var showResults by remember { mutableStateOf(false) }

    // To track questions and user answers
    val questionAnswerList = remember { mutableStateListOf<Pair<String, String>>() }

    val currentRecord = records.getOrNull(currentRecordIndex)
    val sourceSentence = currentRecord?.sourceSentence ?: ""
    val translation = currentRecord?.translation ?: ""
    val jumbledSentence = jumbleWords(sourceSentence).lowercase()

    // Focus request based on the current question
    LaunchedEffect(currentQuestion) {
        when (currentQuestion) {
            0 -> focusRequesterJumbled.requestFocus()
            1 -> focusRequesterTranslation.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(navController = navController, learningOption = learningOption)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            HeaderWithImage(headerText = TEST_CHUNK, showSecondaryInfo = false)

            if (showResults) {
                // Calculate the total possible score correctly
                val maxScore = records.size * 2 // Each record has 2 questions
                val percentage = (totalScore / maxScore) * 100
                val resultText = if (percentage >= 80) {
                    "Well done, you scored $totalScore out of $maxScore (${percentage.toInt()}%)"
                } else {
                    "More practice needed! You scored $totalScore out of $maxScore (${percentage.toInt()}%)"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(resultText)

                    // Display questions and user answers side by side
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        items(questionAnswerList) { (question, answer) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = "Q: $question",
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = "A: $answer",
                                    modifier = Modifier.weight(0.5f)
                                )
                            }
                        }
                    }

                    SpacerHeight()
                    // Action buttons
                    StandardButton(
                        onClick = {
                            loadNextChunk(chunkId + 1) // Load the next chunk
                            navController.navigate(LEARN)
                        },
                        text = "Learn new vocabulary"
                    )
                    SpacerHeight()
                    StandardButton(
                        onClick = {
                            currentRecordIndex = 0
                            currentQuestion = 0
                            questionAnswerList.clear()
                            totalScore = 0f
                            showResults = false
                            navController.navigate(LEARN)
                        },
                        text = "Keep practicing current vocabulary"
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {

                    SpacerHeight(dp = 20)
                    // First question: jumbled sentence
                    if (currentQuestion == 0) {
                        OutlinedTextField(
                            value = jumbledSentence,
                            onValueChange = {},
                            label = { Text("Jumbled Sentence") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = monoTextStyle
                        )

                        SpacerHeight(dp = 8)

                        OutlinedTextField(
                            value = userInputJumbled,
                            onValueChange = { userInputJumbled = it },
                            label = { Text("Enter correct sentence") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesterJumbled),
                            textStyle = monoTextStyle
                        )

                        SpacerHeight(dp = 8)
                        StandardButton(
                            onClick = {
                                val isCorrect = userInputJumbled.isGoodMatch(sourceSentence)
                                if (isCorrect) totalScore += 1f

                                // Add to question/answer list
                                questionAnswerList.add(Pair("Jumbled: $jumbledSentence", userInputJumbled))

                                userInputJumbled = ""
                                currentQuestion = 1 // Move to the next question
                            },
                            text = "Submit"
                        )
                    }

                    // Second question: source sentence
                    if (currentQuestion == 1) {
                        OutlinedTextField(
                            value = sourceSentence,
                            onValueChange = {},
                            label = { Text("Source Sentence") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        SpacerHeight()

                        OutlinedTextField(
                            value = userInputTranslation,
                            onValueChange = { userInputTranslation = it },
                            label = { Text("Translate the sentence") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesterTranslation)
                        )

                        SpacerHeight(dp = 8)
                        StandardButton(
                            onClick = {
                                val isCorrect = userInputTranslation.isGoodMatch(translation)
                                if (isCorrect) totalScore += 1f

                                // Add to question/answer list
                                questionAnswerList.add(Pair("Source: $sourceSentence", userInputTranslation))

                                if (currentRecordIndex + 1 < records.size) {
                                    // Move to the next record
                                    currentRecordIndex++
                                    currentQuestion = 0
                                    userInputTranslation = ""
                                } else {
                                    // Show results if it's the last record
                                    showResults = true
                                }
                            },
                            text = "Submit"
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
fun showToastWithBeep(context: MainActivity, message: String, playNiceBeep: Boolean) {
    // Create and show the Toast
    Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
        setGravity(Gravity.CENTER, 0, 0) // Set the gravity to center
        show()
    }

    // Create the ToneGenerator instance
    val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    if (playNiceBeep) {
        // Play a pleasant tone
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    } else {
        // Play an unpleasant tone
        toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 200)
    }

    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed({ toneGen.release() }, 1000)
}

@Composable
fun VocabScreen(navController: NavController, vocabCards: List<VocabCard>) {
    // sort list alphabetically
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Vocabulary Records", style = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic
        ))

        SpacerHeight()

        vocabCards.forEach { record ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(text = record.word)
                if (record.article.isNullOrEmpty()) {
                    Text(text = ", ${record.article}")
                }
/*
                Text(text = record.wordType, modifier = Modifier.weight(1f))
                Text(text = record.translation, modifier = Modifier.weight(1f))
*/
            }
            // Divider() // Optional divider between records
        }

        SpacerHeight()
        StandardButton(
            onClick = { navController.popBackStack() },
            text = "Back to Home"
        )
    }
}

@Composable
fun ExtrasScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        // The rest of your screen content
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Placeholder for Extras Screen")
            SpacerHeight()
            StandardButton(
                onClick = { navController.popBackStack() },
                text = "Back to Home"
            )
        }
    }
}

@Composable
fun HowToScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val boxHeight = screenHeight * 0.85f
    val boxWidth = screenWidth * 0.9f

    // Use a Column to provide a vertical layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .height(boxHeight)
                .width(boxWidth)
                .border(1.dp, Color.Gray) // Optional: border to visualize the box
        ) {
            // Column inside Box to enable vertical scrolling
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.game_instructions),
                    fontSize = 16.sp
                )
            }
        }

        // Spacer to provide space between Box and Button
        SpacerHeight()

        // Button aligned at the bottom of the screen
        StandardButton(
            onClick = { navController.popBackStack() },
            text = "Back to Home"
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun CustomTopAppBar(
    navController: NavController,
    learningOption: String
) {
    val currentIndex = LEARNING_CYCLE.indexOf(learningOption)
    val previousIndex = (currentIndex - 1 + NUMBER_OF_LEARNING_OPTIONS) % NUMBER_OF_LEARNING_OPTIONS
    val nextIndex = (currentIndex + 1) % NUMBER_OF_LEARNING_OPTIONS

    TopAppBar(
        title = { }, // Leave empty so no title
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSkyBlue),
        actions = {
            // Back arrow - go to previous learning option
            IconButton(onClick = { navController.navigate(LEARNING_CYCLE[previousIndex]) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = LEARNING_CYCLE[previousIndex],
                modifier = Modifier.clickable { navController.navigate(LEARNING_CYCLE[previousIndex]) }
            )
            Spacer(modifier = Modifier.weight(1f))
            // Home Icon
            IconButton(onClick = { navController.navigate(HOME) }) {
                Icon(Icons.Filled.Home, contentDescription = "Home")
            }
            Spacer(modifier = Modifier.weight(1f))
            // Next arrow - go to following learning option
            Text(
                text = LEARNING_CYCLE[nextIndex],
                modifier = Modifier.clickable { navController.navigate(LEARNING_CYCLE[nextIndex]) }
            )
            IconButton(onClick = { navController.navigate(LEARNING_CYCLE[nextIndex]) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }
    )
}
