package com.ajaz.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorScreen()
        }
    }
}

@Composable
fun CalculatorScreen() {
    var display by remember { mutableStateOf("0") }
    var firstNumber by remember { mutableStateOf<Double?>(null) }
    var operation by remember { mutableStateOf<String?>(null) }
    var startNewNumber by remember { mutableStateOf(false) }

    fun calculate() {
        val first = firstNumber ?: return
        val second = display.toDoubleOrNull() ?: return

        val result = when (operation) {
            "+" -> first + second
            "−" -> first - second
            "×" -> first * second
            "÷" -> if (second != 0.0) first / second else null
            else -> null
        }

        display = result?.let {
            if (it.isFinite()) {
                if (it % 1.0 == 0.0) it.toLong().toString()
                else it.toString()
            } else "Error"
        } ?: "Error"

        firstNumber = null
        operation = null
        startNewNumber = true
    }

    val buttons = listOf(
        listOf("C", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "Calculator",
                    fontSize = 30.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = display,
                    fontSize = 48.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    maxLines = 1
                )

                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { button ->
                            Button(
                                onClick = {
                                    when (button) {
                                        "C" -> {
                                            display = "0"
                                            firstNumber = null
                                            operation = null
                                            startNewNumber = false
                                        }
                                        "+", "−", "×", "÷" -> {
                                            firstNumber = display.toDoubleOrNull()
                                            operation = button
                                            startNewNumber = true
                                        }
                                        "=" -> calculate()
                                        "." -> {
                                            if (startNewNumber) {
                                                display = "0."
                                                startNewNumber = false
                                            } else if (!display.contains(".")) {
                                                display += "."
                                            }
                                        }
                                        else -> {
                                            if (startNewNumber || display == "0" || display == "Error") {
                                                display = button
                                                startNewNumber = false
                                            } else {
                                                display += button
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(button, fontSize = 23.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
