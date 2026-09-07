package com.example.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.ActionCoralRed
import com.example.ui.theme.ActionEmeraldGreen
import java.util.Calendar

enum class TimeFilter { ALL, THIS_MONTH, LAST_MONTH, THIS_YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTab(transactions: List<Transaction>, currency: String) {
    var timeFilter by remember { mutableStateOf(TimeFilter.ALL) }

    val filteredTransactions = remember(transactions, timeFilter) {
        when (timeFilter) {
            TimeFilter.ALL -> transactions
            TimeFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)
                transactions.filter {
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
            }
            TimeFilter.LAST_MONTH -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -1)
                val lastMonth = cal.get(Calendar.MONTH)
                val yearOfLastMonth = cal.get(Calendar.YEAR)
                transactions.filter {
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == yearOfLastMonth
                }
            }
            TimeFilter.THIS_YEAR -> {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                transactions.filter {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.YEAR) == currentYear
                }
            }
        }
    }

    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune donnée pour les statistiques.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val expenses = filteredTransactions.filter { it.type == TransactionType.CASH_OUT }
    val incomes = filteredTransactions.filter { it.type == TransactionType.CASH_IN }
    
    val totalExpense = expenses.sumOf { it.amount }
    val totalIncome = incomes.sumOf { it.amount }
    val maxBarValue = maxOf(totalExpense, totalIncome)

    val expenseByCategory = expenses.groupBy { it.source.takeIf { s -> s.isNotBlank() } ?: "Autre" }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .toList()
        .sortedByDescending { it.second }

    val colors = listOf(
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0),
        Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF2196F3),
        Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Statistiques et Rapports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = when(timeFilter) {
                        TimeFilter.ALL -> "Toute la période"
                        TimeFilter.THIS_MONTH -> "Ce mois"
                        TimeFilter.LAST_MONTH -> "Mois dernier"
                        TimeFilter.THIS_YEAR -> "Cette année"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Toute la période") }, onClick = { timeFilter = TimeFilter.ALL; expanded = false })
                    DropdownMenuItem(text = { Text("Ce mois") }, onClick = { timeFilter = TimeFilter.THIS_MONTH; expanded = false })
                    DropdownMenuItem(text = { Text("Mois dernier") }, onClick = { timeFilter = TimeFilter.LAST_MONTH; expanded = false })
                    DropdownMenuItem(text = { Text("Cette année") }, onClick = { timeFilter = TimeFilter.THIS_YEAR; expanded = false })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("Comparaison Revenus vs Dépenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (maxBarValue > 0) {
                        // Incomes Bar
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Revenus", modifier = Modifier.width(80.dp), fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val fraction = (totalIncome / maxBarValue).toFloat()
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(ActionEmeraldGreen))
                            }
                            Text(formatCurrency(totalIncome), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Expenses Bar
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Dépenses", modifier = Modifier.width(80.dp), fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val fraction = (totalExpense / maxBarValue).toFloat()
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(ActionCoralRed))
                            }
                            Text(formatCurrency(totalExpense), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("Aucune donnée pour générer le graphique.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text("Répartition des Dépenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (totalExpense > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(150.dp)) {
                        var startAngle = -90f
                        expenseByCategory.forEachIndexed { index, pair ->
                            val sweepAngle = ((pair.second / totalExpense) * 360f).toFloat()
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Butt),
                                size = Size(size.width, size.height)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }
            } else {
                Text("Aucune dépense enregistrée sur cette période.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        items(expenseByCategory.size) { index ->
            val pair = expenseByCategory[index]
            val percentage = if (totalExpense > 0) (pair.second / totalExpense) * 100 else 0.0
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = colors[index % colors.size], shape = MaterialTheme.shapes.small, modifier = Modifier.size(16.dp)) {}
                Spacer(modifier = Modifier.width(16.dp))
                Text(pair.first, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(formatCurrency(pair.second) + " " + currency, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("${String.format("%.1f", percentage)}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
