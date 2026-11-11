package com.ajaz.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val Background = Color(0xFF0B0F14)
private val Panel = Color(0xFF141B24)
private val NumberButton = Color(0xFF1C2633)
private val OperatorButton = Color(0xFF263A52)
private val Accent = Color(0xFF3985F7)
private val MainText = Color(0xFFF4F7FC)
private val MutedText = Color(0xFF8B9AAF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorApp()
        }
    }
}

@Composable
fun CalculatorApp() {

    val context = LocalContext.current

    val database = remember {
        CalculatorDatabase.getDatabase(context)
    }

    val dao = remember {
        database.calculationDao()
    }

    val scope = rememberCoroutineScope()

    val history by dao.getAllCalculations()
        .collectAsState(initial = emptyList())

    var expression by remember { mutableStateOf("") }
    var justEvaluated by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val result = remember(expression) {
        if (expression.isNotEmpty() &&
            expression.last() !in "+-×÷.") {
            runCatching {
                val value = ExpressionParser(expression).parse()
                if (value.isFinite()) formatNumber(value) else null
            }.getOrNull()
        } else null
    }

    val display = result ?: expression.ifEmpty { "0" }

    fun clear() {
        expression = ""
        justEvaluated = false
    }

    fun addNumber(number: String) {
        if (justEvaluated) {
            expression = ""
            justEvaluated = false
        }

        expression += number
    }

    fun addOperator(operator: String) {
        if (expression.isEmpty()) {
            if (operator == "−") expression = "−"
            return
        }

        justEvaluated = false

        if (expression.last() in "+-×÷.") {
            expression = expression.dropLast(1) + operator
        } else {
            expression += operator
        }
    }

    fun addDecimal() {
        if (justEvaluated) {
            expression = ""
            justEvaluated = false
        }

        val lastPart = expression
            .substringAfterLast('+')
            .substringAfterLast('−')
            .substringAfterLast('×')
            .substringAfterLast('÷')

        if (!lastPart.contains(".")) {
            expression += if (
                expression.isEmpty() ||
                expression.last() in "+-×÷"
            ) "0." else "."
        }
    }

    fun backspace() {
        if (justEvaluated) {
            clear()
        } else if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
        }
    }

    fun toggleSign() {
        if (expression.isEmpty()) return

        justEvaluated = false

        var start = expression.length

        while (
            start > 0 &&
            (expression[start - 1].isDigit() ||
             expression[start - 1] == '.')
        ) {
            start--
        }

        if (start == expression.length) return

        if (
            start > 0 &&
            expression[start - 1] == '−' &&
            (start - 1 == 0 ||
             expression[start - 2] in "+-×÷")
        ) {
            expression = expression.removeRange(start - 1, start)
        } else {
            expression = expression.substring(0, start) +
                "−" + expression.substring(start)
        }
    }

    fun percentage() {
        if (
            expression.isNotEmpty() &&
            (expression.last().isDigit() || expression.last() == '.')
        ) {
            justEvaluated = false
            expression += "/100"
        }
    }

    fun calculate() {
        if (expression.isEmpty()) return

        val originalExpression = expression

        runCatching {
            val value = ExpressionParser(originalExpression).parse()

            if (!value.isFinite()) {
                error("Invalid result")
            }

            val answer = formatNumber(value)

            // Save calculation in Room Database
            scope.launch(Dispatchers.IO) {
                dao.insertCalculation(
                    Calculation(
                        expression = originalExpression,
                        result = answer
                    )
                )
            }

            expression = answer
            justEvaluated = true

        }.onFailure {
            expression = "Error"
            justEvaluated = true
        }
    }

    val rows = listOf(
        listOf("AC", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("+/−", "0", ".", "=")
    )

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "CALCULATOR",
                            color = MainText,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Text(
                            if (showHistory) "CALCULATION HISTORY"
                            else "STANDARD EDITION",
                            color = MutedText,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    Text(
                        text = if (showHistory) "BACK" else "HISTORY",
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Panel)
                            .clickable {
                                showHistory = !showHistory
                            }
                            .padding(14.dp)
                    )
                }

                if (showHistory) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Saved calculations",
                            color = MainText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "CLEAR ALL",
                            color = if (history.isEmpty()) MutedText else Accent,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable(enabled = history.isNotEmpty()) {
                                    scope.launch(Dispatchers.IO) {
                                        dao.deleteAllCalculations()
                                    }
                                }
                                .padding(8.dp)
                        )
                    }

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No calculations yet",
                                color = MutedText,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = history,
                                key = { it.id }
                            ) { item ->

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Panel)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            item.expression,
                                            color = MutedText,
                                            fontSize = 15.sp
                                        )

                                        Spacer(
                                            modifier = Modifier.height(5.dp)
                                        )

                                        Text(
                                            "= ${item.result}",
                                            color = MainText,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        "DELETE",
                                        color = Accent,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .clickable {
                                                scope.launch(Dispatchers.IO) {
                                                    dao.deleteCalculation(item.id)
                                                }
                                            }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                } else {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(Panel)
                            .padding(20.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                expression.ifEmpty { "Ready to calculate" },
                                color = MutedText,
                                fontSize = 19.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )

                            AnimatedContent(
                                targetState = display,
                                transitionSpec = {
                                    (
                                        fadeIn() + slideInVertically { it / 5 }
                                    ) togetherWith (
                                        fadeOut() + slideOutVertically { -it / 5 }
                                    )
                                },
                                label = "CalculatorDisplay"
                            ) { value ->
                                Text(
                                    value,
                                    color = MainText,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.Light,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.End
                                )
                            }

                            Text(
                                if (result != null) "LIVE RESULT" else " ",
                                color = Accent,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { label ->

                                val isEquals = label == "="
                                val isOperator = label in listOf("÷", "×", "−", "+")
                                val isUtility = label in listOf("AC", "⌫", "%", "+/−")

                                val buttonColor = when {
                                    isEquals -> Accent
                                    isOperator -> OperatorButton
                                    isUtility -> Panel
                                    else -> NumberButton
                                }

                                val textColor = when {
                                    isEquals -> Color.White
                                    isOperator -> Accent
                                    isUtility -> MutedText
                                    else -> MainText
                                }

                                CalculatorKey(
                                    label = label,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(66.dp),
                                    background = buttonColor,
                                    foreground = textColor
                                ) {
                                    when (label) {
                                        "AC" -> clear()
                                        "⌫" -> backspace()
                                        "%" -> percentage()
                                        "+/−" -> toggleSign()
                                        "." -> addDecimal()
                                        "=" -> calculate()
                                        "+", "−", "×", "÷" -> addOperator(label)
                                        else -> addNumber(label)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    "DESIGNED BY AJAZ",
                    color = MutedText.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CalculatorKey(
    label: String,
    modifier: Modifier,
    background: Color,
    foreground: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 500f
        ),
        label = "ButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = foreground,
            fontSize = if (label.length > 2) 17.sp else 23.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatNumber(value: Double): String {
    return BigDecimal.valueOf(value)
        .stripTrailingZeros()
        .toPlainString()
}

private class ExpressionParser(
    private val input: String
) {
    private var position = 0

    fun parse(): Double {
        val value = expression()

        if (position != input.length) {
            error("Invalid expression")
        }

        return value
    }

    private fun expression(): Double {
        var value = term()

        while (position < input.length) {
            when (input[position]) {
                '+' -> {
                    position++
                    value += term()
                }
                '−', '-' -> {
                    position++
                    value -= term()
                }
                else -> return value
            }
        }

        return value
    }

    private fun term(): Double {
        var value = factor()

        while (position < input.length) {
            when (input[position]) {
                '×', '*' -> {
                    position++
                    value *= factor()
                }
                '÷', '/' -> {
                    position++
                    val divisor = factor()

                    if (divisor == 0.0) {
                        error("Division by zero")
                    }

                    value /= divisor
                }
                else -> return value
            }
        }

        return value
    }

    private fun factor(): Double {
        if (position >= input.length) {
            error("Missing number")
        }

        when (input[position]) {
            '−', '-' -> {
                position++
                return -factor()
            }
            '+' -> {
                position++
                return factor()
            }
        }

        val start = position

        while (
            position < input.length &&
            (input[position].isDigit() || input[position] == '.')
        ) {
            position++
        }

        if (start == position) {
            error("Invalid number")
        }

        return input.substring(start, position).toDouble()
    }
}

                
