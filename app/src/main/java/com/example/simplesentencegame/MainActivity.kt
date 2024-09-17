package com.example.simplesentencegame

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
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

const val FILENAME = "sentenceDatabase.json"
const val MAX_SCORE = 2 // # correct answers to gen animation
const val TIME_FACTOR_PER_CHAR = 60
const val DEBUG = "BQ_DEBUG"
// const val BUTTON_HEIGHT = 35
// val tonedDownButtonColor = Color.Blue.copy(alpha = 0.7f)
const val LOTTIE_SIZE = 400
@Serializable
data class SentenceRecord(val id: Int, val completeSentence: String, val gameSentence: String, val translation: String)

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech

    // Function to speak a sentence using TextToSpeech
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

        val filePath = "${this.filesDir.path}/$FILENAME"
        val records = loadRecords(filePath)
        if (records.isEmpty()) throw Exception("Bad data file!!!")

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    BoxWithConstraints(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val buttonWidth = maxWidth / 2 // Half of the screen width

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // App name header and app image
                            AppHeaderWithImage()

                            Spacer(modifier = Modifier.height(16.dp))

                            val buttonConfigurations = listOf(
                                ButtonConfig("Learn") { navController.navigate("learn") },
                                ButtonConfig("Game1") { navController.navigate("game1") },
                                ButtonConfig("Stats") { navController.navigate("stats") },
                                ButtonConfig("Extras") { navController.navigate("extras") },
                                ButtonConfig("How to") { navController.navigate("howto") },
                                ButtonConfig("Exit") { this@MainActivity.finish() }
                            )

                            buttonConfigurations.forEach { buttonConfig ->
                                Spacer(modifier = Modifier.height(8.dp))
                                LearnButton(
                                    onClick = buttonConfig.onClick,
                                    text = buttonConfig.text,
                                    buttonWidth = buttonWidth
                                )
                            }
                        }
                    }
                }
                composable("learn") {
                    LearnSentences(navController, records, this@MainActivity) { text -> text.speak(tts) }
                }
                composable("game1") {
                    LearnSentences(navController, records, this@MainActivity) { text -> text.speak(tts) }
                }
                composable("stats") {
                    StatsScreen(navController)
                }
                composable("extras") {
                    ExtrasScreen(navController)
                }
                composable("howto") {
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
        Text(text = text, fontSize = 18.sp)
    }
}

data class ButtonConfig(val text: String, val onClick: () -> Unit)

@Composable
fun AppHeaderWithImage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // App name header
        Text(
            text = stringResource(id = R.string.app_name),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = DeepSkyBlue,
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Image below the app name
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
@ExperimentalMaterial3Api
@Composable
fun LearnSentences(
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

    val coroutineScope = rememberCoroutineScope()

    // Ensure valid currentRecordIndex within records range
    val currentRecord = if (records.isNotEmpty()) records[currentRecordIndex] else null
    val completeSentence = currentRecord!!.completeSentence
    val inCompleteSentence = currentRecord.gameSentence
    val translation = currentRecord.translation

    val timeFactorPerChar = TIME_FACTOR_PER_CHAR // how long to display sentence, i.e. delay.
    val calculatedDelay = completeSentence.length.times(timeFactorPerChar)

    val textStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = monoSpace // so sentences align on screen for user
    )

    val focusRequester = remember { FocusRequester() }

    var displayCompleteSentence by remember { mutableStateOf(true) }
    var refreshButton by remember { mutableIntStateOf(0) }

    LaunchedEffect(inCompleteSentence, refreshButton) {
        displayCompleteSentence = true
        delay(calculatedDelay.toLong())
        displayCompleteSentence = false
    }

    fun onRefresh() {
        refreshButton += 1
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                navController = navController,
                context = context
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
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (displayCompleteSentence) completeSentence else inCompleteSentence,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .padding(top = 25.dp)
                            .fillMaxWidth(),
                        textStyle = textStyle
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp)
                            .size(20.dp)
                            .background(color = DeepSkyBlue, shape = RoundedCornerShape(8.dp))
                    ) {
                        IconButton(
                            onClick = { onRefresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("Enter full sentence here…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent { event ->
                            when (event.key) {
                                Key.Tab, Key.Enter -> {
                                    if (userInput.trim().isNotEmpty()) {
                                        if (userInput.trim() == completeSentence) {
                                            showTickMark = true
                                            speak(completeSentence)
                                            coroutineScope.launch {
                                                delay(2500)
                                                score += 1
                                                if (score >= MAX_SCORE) {
                                                    showLottieAnimation = true
                                                }
                                                currentRecordIndex = (currentRecordIndex + 1) % records.size
                                                userInput = ""
                                            }
                                        } else {
                                            showToastWithBeep(context, "Try again!", isCorrect = false)
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        },
                    textStyle = textStyle
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (userInput.trim() == completeSentence) {
                                showTickMark = true
                                speak(completeSentence)
                                coroutineScope.launch {
                                    delay(2500)
                                    score += 1
                                    if (score >= MAX_SCORE) {
                                        showLottieAnimation = true
                                    }
                                    currentRecordIndex = (currentRecordIndex + 1) % records.size
                                    userInput = ""
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

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = translation,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = textStyle
                )

                Spacer(modifier = Modifier.height(16.dp))
                val progress = score / MAX_SCORE.toFloat()
                Text(text = "Progress: ${(progress * 100).toInt()}%")
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

            if (showLottieAnimation) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                        .padding(top = 24.dp)
                ) {
                    LottieAnimation(
                        composition = lottieComposition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier
                            .size(LOTTIE_SIZE.dp)
                            .align(Alignment.TopCenter)
                    )
                    Text(
                        text = "You got $MAX_SCORE correct answers… very cool!",
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
fun loadRecords(filePath: String): List<SentenceRecord> {
    // Log.d(DEBUG, "loadRecords: filePath=$filePath")
    val file = File(filePath)
    return if (file.exists()) {
        try {
            val jsonData = file.readText()
            try {
                Json.decodeFromString(jsonData)
            } catch (e: Exception) {
                throw Exception("could not decode jsonData", e)
            }
        } catch (e: Exception) {
            throw Exception("bad file.readText()", e)
        }
    } else {
        throw Exception("file does not exist")
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
        // Play a pleasant tone and show tick mark
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    } else {
        toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 200)
    }

    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed({ toneGen.release() }, 1000)
}
@Composable
fun StatsScreen(navController: NavController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Placeholder for Stats Screen")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back to Home")
        }
    }
}

@Composable
fun ExtrasScreen(navController: NavController) {

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Placeholder for Extras Screen")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back to Home")
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
            modifier = Modifier.align(Alignment.CenterHorizontally) // Align Button horizontally in Column
        ) {
            Text("Back to Home")
        }
    }
}
@ExperimentalMaterial3Api
@Composable
fun CustomTopAppBar(
    navController: NavController,
    context: MainActivity
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.app_name), overflow = TextOverflow.Ellipsis) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSkyBlue),
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { navController.navigateUp() }
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back", style = MaterialTheme.typography.bodyLarge)
            }
        },
        actions = {
            IconButton(onClick = { context.finish() }, modifier = Modifier.width(100.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Exit")
                    Text(text = "Exit")
                }
            }
        }
    )
}
