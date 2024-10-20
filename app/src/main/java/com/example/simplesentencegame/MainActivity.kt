@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.simplesentencegame
import android.content.Context
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

val monoSpace = FontFamily.Monospace
val DeepSkyBlue = Color(0xFF00BFFF)
val LightGreen = Color(0xFF90EE90)
val customTypography = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 24.sp,
    letterSpacing = 0.15.sp
)

const val DEBUG = "BQ_DEBUG"

// const val SENTENCES_FILENAME = "sentenceDatabase.json"
const val VOCAB_FILENAME = "vocab.json"
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
// const val STATS = "STATS"
const val VOCAB = "VOCABULARY"
const val HOWTO = "HOWTO"
const val EXTRAS = "EXTRAS"
const val EXIT = "EXIT"
val LEARNING_CYCLE = listOf(LEARN, PRACTICE_RECALL, PRACTICE_SOURCE, PRACTICE_TARGET)
val NUMBER_OF_LEARNING_OPTIONS = LEARNING_CYCLE.size

data class ButtonConfig(val text: String, val onClick: () -> Unit)

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

@Serializable
data class SentenceRecord(
    val id: Int,
    val sourceSentence: String,
    val gameSentence: String,
    val translation: String
)

@Serializable
data class VocabRecord(
    val word: String,
    val wordType: String,
    val translation: String,
    val article: String = "",
    val wordsChunkId: Int,
    val sentencesChunkID: Int
)

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech

    private fun String.speak(tts: TextToSpeech) {
        tts.speak(this, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(DEBUG, "MainActivity: ok.")

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

        /*// get sentences from file
        val sentencesFile = "${this.filesDir.path}/$SENTENCES_FILENAME"
        val records = loadSentencesFromFile(sentencesFile)
        if (records.isEmpty()) throw Exception("MainActivity: Bad Sentences file!!!")
        */
        // get sentences from database
        val records = loadSentencesFromDb(this)

        // get vocab from file
        val vocabFile = "${this.filesDir.path}/$VOCAB_FILENAME"
        val vocab = loadVocab(vocabFile).sortedBy { it.word }

        if (vocab.isEmpty()) throw Exception("MainActivity: Bad Vocab file!!!")

        // main menu
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = HOME) {
                composable(HOME) {
                    BoxWithConstraints(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val buttonWidth = maxWidth * 2 / 3 // Half of the screen width

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // App name header and app image
                            HeaderWithImage(stringResource(id = R.string.app_name), true)

                            Spacer(modifier = Modifier.height(16.dp))

                            val buttonConfigurations = listOf(
                                ButtonConfig(LEARN) { navController.navigate(LEARN) },
                                ButtonConfig(PRACTICE_RECALL) { navController.navigate(PRACTICE_RECALL) },
                                ButtonConfig(PRACTICE_SOURCE) { navController.navigate(PRACTICE_SOURCE) },
                                ButtonConfig(PRACTICE_TARGET) { navController.navigate(PRACTICE_TARGET) },
                                ButtonConfig(VOCAB) { navController.navigate(VOCAB) },
                                ButtonConfig(EXTRAS) { navController.navigate(EXTRAS) },
                                ButtonConfig(HOWTO) { navController.navigate(HOWTO) },
                                ButtonConfig(EXIT) { this@MainActivity.finish() }
                            )

                            buttonConfigurations.forEach { buttonConfig ->
                                //Spacer(modifier = Modifier.height(6.dp))
                                LearnButton(
                                    onClick = buttonConfig.onClick,
                                    text = buttonConfig.text,
                                    buttonWidth = buttonWidth
                                )
                            }
                        }
                    }
                }
                composable(LEARN) {
                    LearnSentences(LEARN,
                        navController, records, this@MainActivity) { text -> text.speak(tts) }
                }
                composable(PRACTICE_RECALL) {
                    LearnSentences(PRACTICE_RECALL,
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
    }
}

@ExperimentalMaterial3Api
@Composable
fun LearnSentences(
    learningOption: String,
    navController: NavController,
    records: List<SentenceRecord>,
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

    val textStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = monoSpace // so sentences align on screen for user
    )

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
                            textStyle = textStyle
                        )
                    }
                    PRACTICE_RECALL -> {
                        HeaderWithImage(headerText = PRACTICE_RECALL, showSecondaryInfo = false)
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
                            textStyle = textStyle
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
                            textStyle = textStyle
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
                            textStyle = textStyle
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
                    textStyle = textStyle
                )

                // check user input, if good→reward + Next sentence button else beep
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (showNextSentenceButton) {
                        Button(
                            onClick = {
                                // Move to the next sentence
                                currentRecordIndex = (currentRecordIndex + 1) % records.size
                                userInput = ""
                                spoken = false
                                showTickMark = false
                                showNextSentenceButton = false // Hide "Next sentence" button
                            },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Green,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                text = "Next sentence",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (showGoToNextOptionButton) {
                            Spacer(modifier = Modifier.width(8.dp)) // Optional spacing between buttons
                            Button(
                                onClick = {
                                    navController.navigate(nextLearningOption) // Navigate to the next learning option
                                },
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepSkyBlue,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(
                                    text = "Go to $nextLearningOption",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                    } else {
                        Button(
                            onClick = {
                                if (userInput.trim() == answerSentence) {
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
                                    showToastWithBeep(context, "Try again!", isCorrect = false)
                                }
                            },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepSkyBlue,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                text = "Submit",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                // show translation if applicable for learning option
                if (learningOption == LEARN
                    || learningOption == PRACTICE_RECALL) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = translation,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = textStyle
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
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

                Spacer(modifier = Modifier.height(16.dp))
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

@Composable
fun LearnButton(
    onClick: () -> Unit,
    text: String,
    buttonWidth: Dp = 100.dp, // Default button width
    buttonColor: Color = DeepSkyBlue,
    textColor: Color = Color.Black
) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(buttonWidth),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = textColor
        )
    ) {
        Text(text = text, fontSize = 16.sp)
    }
}

@Composable
fun SentenceDisplay(
    testSentence: String,
    answerSentence: String,
    flashAnswerSentence: Boolean,
    showRefreshButton: Boolean,
    onRefresh: () -> Unit,
    textStyle: TextStyle
) {
    // show test sentence
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            // Learn option only, briefly show answer sentence
            value = if (flashAnswerSentence) answerSentence else testSentence,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .padding(top = 25.dp)
                .fillMaxWidth(),
            textStyle = textStyle,
            trailingIcon = {
                // Show refresh icon only if the refresh button is enabled
                if (showRefreshButton) {
                    IconButton(onClick = { onRefresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.Black
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun HeaderWithImage(headerText: String, showSecondaryInfo: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // App name header
        Text(
            text = headerText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = DeepSkyBlue,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
        // Image below the header
        if (showSecondaryInfo) {
            Text(
                text = "Goal → 1,000 words",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = Color.Green,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.netherlands),
                    contentDescription = "App Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
fun showToastWithBeep(context: MainActivity, message: String, isCorrect: Boolean) {
    // Create and show the Toast
    val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
    toast.show()

    // Create the ToneGenerator instance
    val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    if (isCorrect) {
        // Play a pleasant tone
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    } else {
        // Play an unpleasant tone
        toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 200)
    }

    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed({ toneGen.release() }, 1000)
}

// load sentences
/*
fun loadSentencesFromFile(filePath: String): List<SentenceRecord> {
    Log.d(DEBUG, "loadSentencesFromFile: filePath=$filePath")
    val file = File(filePath)
    return if (file.exists()) {
        try {
            val jsonData = file.readText()
            try {
                Json.decodeFromString(jsonData)
            } catch (e: Exception) {
                Log.d(DEBUG,"loadSentencesFromFile: could not decode jsonData")
                throw Exception("loadSentencesFromFile: could not decode jsonData", e)
            }
        } catch (e: Exception) {
            Log.d(DEBUG,"loadSentencesFromFile: bad file.readText()")
            throw Exception("loadSentencesFromFile: bad file.readText()", e)
        }
    } else {
        throw Exception("loadSentencesFromFile: file does not exist")
    }
}
*/

// load vocab
fun loadVocab(filePath: String): List<VocabRecord> {
    Log.d(DEBUG, "loadVocab: filePath=$filePath")
    val file = File(filePath)
    return if (file.exists()) {
        try {
            val jsonData = file.readText()
            try {
                Json.decodeFromString<List<VocabRecord>>(jsonData)
            } catch (e: Exception) {
                Log.d(DEBUG,"loadVocab: could not decode jsonData")
                throw Exception("loadVocab: could not decode jsonData", e)
            }
        } catch (e: Exception) {
            Log.d(DEBUG,"loadVocab: bad file.readText()")
            throw Exception("loadVocab: bad file.readText()", e)
        }
    } else {
        throw Exception("loadVocab: file does not exist")
    }
}

@Composable
fun VocabScreen(navController: NavController, vocabRecords: List<VocabRecord>) {
    // sort list alphabetically
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Vocabulary Records", style = customTypography) // Use custom typography

        Spacer(modifier = Modifier.height(16.dp))

        vocabRecords.forEach { record ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(text = record.word)
                if (record.article.isNotEmpty()) {
                    Text(text = ", ${record.article}")
                }
/*
                Text(text = record.wordType, modifier = Modifier.weight(1f))
                Text(text = record.translation, modifier = Modifier.weight(1f))
*/
            }
            // Divider() // Optional divider between records
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back to Home")
        }
    }
}

@Composable
fun ExtrasScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        // The rest of your screen content
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Placeholder for Extras Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Back to Home")
            }
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
        Spacer(modifier = Modifier.height(16.dp))

        // Button aligned at the bottom of the screen
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back to Home")
        }
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

fun loadSentencesFromDb(context: Context): List<SentenceRecord> {
    val dbHelper = MyDatabaseHelper(context)
    val db: SQLiteDatabase = dbHelper.readableDatabase

    val sentenceRecords = mutableListOf<SentenceRecord>()

    try {
        sentenceRecords.addAll(loadSentencesFromDB(db))
    } catch (e: Exception) {
        Log.e(DEBUG, "Error loading sentences from database: ${e.message}")
        throw Exception("Error loading sentences from database: ${e.message}")
    } finally {
        db.close() // Always close the database
    }

    return sentenceRecords
}

fun loadSentencesFromDB(db: SQLiteDatabase): List<SentenceRecord> {
    Log.d(DEBUG, "loadSentencesFromDB: loading records from database")

    val sentenceRecords = mutableListOf<SentenceRecord>()

    // Query to select all rows from the 'sentences' table
    val cursor = db.rawQuery("SELECT id, sourceSentence, gameSentence, translation FROM sentences", null)

    // Move the cursor to the first row
    if (cursor.moveToFirst()) {
        do {
            // Extract the values from the cursor
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val sourceSentence = cursor.getString(cursor.getColumnIndexOrThrow("sourceSentence"))
            val gameSentence = cursor.getString(cursor.getColumnIndexOrThrow("gameSentence"))
            val translation = cursor.getString(cursor.getColumnIndexOrThrow("translation"))

            // Add a new SentenceRecord to the list
            sentenceRecords.add(SentenceRecord(id, sourceSentence, gameSentence, translation))
        } while (cursor.moveToNext()) // Move to the next row
    } else {
        Log.e(DEBUG, "loadSentencesFromDB: No rows found in database.")
    }

    // Close the cursor after the query
    cursor.close()

    Log.d(DEBUG, "loadSentencesFromDB: loaded ${sentenceRecords.size} rows")
    if(sentenceRecords.size < 1) throw Exception("loadSentencesFromDB: NO rows loaded")
    return sentenceRecords
}
