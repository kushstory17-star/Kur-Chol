package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyCatalog
import com.example.ui.ExchangeUiState
import com.example.ui.ExchangeViewModel

@Composable
fun CalculatorScreen(
    uiState: ExchangeUiState,
    onApplyToConverter: (Double) -> Unit,
    onOpenFromPicker: () -> Unit,
    onOpenToPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expression by remember { mutableStateOf("0") }
    var runningTotal by remember { mutableDoubleStateOf(0.0) }

    val fromInfo = remember(uiState.fromCurrency) {
        CurrencyCatalog.getInfo(uiState.fromCurrency)
    }
    val toInfo = remember(uiState.toCurrency) {
        CurrencyCatalog.getInfo(uiState.toCurrency)
    }

    val convertedTotal = runningTotal * (uiState.rates[uiState.toCurrency] ?: 1.0)

    fun evaluateExpression(exp: String): Double {
        return try {
            // Simple robust parser for + - * /
            val tokens = mutableListOf<String>()
            var currentNumber = StringBuilder()
            for (ch in exp) {
                if (ch in "+-×÷*/") {
                    if (currentNumber.isNotEmpty()) {
                        tokens.add(currentNumber.toString())
                        currentNumber = StringBuilder()
                    }
                    val op = when (ch) {
                        '×' -> "*"
                        '÷' -> "/"
                        else -> ch.toString()
                    }
                    tokens.add(op)
                } else {
                    currentNumber.append(ch)
                }
            }
            if (currentNumber.isNotEmpty()) {
                tokens.add(currentNumber.toString())
            }

            if (tokens.isEmpty()) return 0.0

            // Pass 1: * and /
            val intermediate = mutableListOf<String>()
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                if (token == "*" || token == "/") {
                    val prev = intermediate.removeAt(intermediate.size - 1).toDoubleOrNull() ?: 0.0
                    val next = tokens.getOrNull(i + 1)?.toDoubleOrNull() ?: 1.0
                    val res = if (token == "*") prev * next else if (next != 0.0) prev / next else 0.0
                    intermediate.add(res.toString())
                    i += 2
                } else {
                    intermediate.add(token)
                    i++
                }
            }

            // Pass 2: + and -
            var total = intermediate.firstOrNull()?.toDoubleOrNull() ?: 0.0
            var j = 1
            while (j < intermediate.size) {
                val op = intermediate[j]
                val next = intermediate.getOrNull(j + 1)?.toDoubleOrNull() ?: 0.0
                if (op == "+") total += next
                if (op == "-") total -= next
                j += 2
            }
            total
        } catch (e: Exception) {
            0.0
        }
    }

    fun onKeyClick(key: String) {
        when (key) {
            "C" -> {
                expression = "0"
                runningTotal = 0.0
            }
            "⌫" -> {
                expression = if (expression.length > 1) expression.dropLast(1) else "0"
                runningTotal = evaluateExpression(expression)
            }
            "+", "-", "×", "÷" -> {
                val last = expression.lastOrNull()
                if (last != null && last in "+-×÷") {
                    expression = expression.dropLast(1) + key
                } else {
                    expression += key
                }
            }
            "." -> {
                val parts = expression.split("+", "-", "×", "÷")
                val currentPart = parts.lastOrNull() ?: ""
                if (!currentPart.contains(".")) {
                    expression += "."
                }
            }
            "=" -> {
                runningTotal = evaluateExpression(expression)
                expression = if (runningTotal % 1.0 == 0.0) runningTotal.toLong().toString() else runningTotal.toString()
            }
            else -> {
                expression = if (expression == "0") key else expression + key
                runningTotal = evaluateExpression(expression)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Calculator Result Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calculator_result_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Currency Pickers Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CurrencySelectorBadge(
                        flag = fromInfo.flagEmoji,
                        code = fromInfo.code,
                        onClick = onOpenFromPicker,
                        modifier = Modifier.testTag("calc_from_picker")
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "to",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CurrencySelectorBadge(
                        flag = toInfo.flagEmoji,
                        code = toInfo.code,
                        onClick = onOpenToPicker,
                        modifier = Modifier.testTag("calc_to_picker")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expression text
                Text(
                    text = expression,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Running Total in Source Currency
                Text(
                    text = "${fromInfo.symbol} ${ExchangeViewModel.formatAmount(runningTotal)}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                // Converted Total in Target Currency
                Text(
                    text = "≈ ${toInfo.symbol} ${ExchangeViewModel.formatAmount(convertedTotal)} ${toInfo.code}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Apply to Converter Button
                Surface(
                    onClick = { onApplyToConverter(runningTotal) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_apply_button")
                ) {
                    Text(
                        text = "Use in Converter (${fromInfo.symbol}${ExchangeViewModel.formatAmount(runningTotal)})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        // Full Keypad Grid (4x5)
        val calcRows = listOf(
            listOf("C", "÷", "×", "⌫"),
            listOf("7", "8", "9", "-"),
            listOf("4", "5", "6", "+"),
            listOf("1", "2", "3", "="),
            listOf("0", "00", ".", "")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            calcRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        if (key.isNotEmpty()) {
                            val isOp = key in "+-×÷="
                            val isSpecial = key in listOf("C", "⌫")

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when {
                                            isOp -> MaterialTheme.colorScheme.primary
                                            isSpecial -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable { onKeyClick(key) }
                                    .testTag("calc_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = when {
                                        isOp -> MaterialTheme.colorScheme.onPrimary
                                        isSpecial -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
