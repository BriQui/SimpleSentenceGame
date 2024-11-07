package com.example.simplesentencegame

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// val tonedDownButtonColor = Color.Blue.copy(alpha = 0.7f)
val monoSpace = FontFamily.Monospace
val DeepSkyBlue = Color(0xFF00BFFF)
val LightGreen = Color(0xFF90EE90)
val LightGold = Color(0xFFFFECB3)
val LightBlue = Color(0xFFB1E5FA)

val monoTextStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = monoSpace // so sentences align on screen for user
)

@Composable
fun SpacerHeight(dp: Int = 16) {
    Spacer(modifier = Modifier.height(dp.dp))
}

data class ButtonConfig(
    val text: String,
    val onClick: () -> Unit,
    val color: Color
)

@Composable
fun MenuButton(
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
fun StandardButton(
    onClick: () -> Unit,
    text: String,
    buttonColor: Color = DeepSkyBlue,
    textColor: Color = Color.Black
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(4.dp)
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
fun HeaderWithImage(
    headerText: String,
    headerTextColor: Color = LightGreen,
    showSecondaryInfo: Boolean = false
) {
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
            color = headerTextColor,
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
