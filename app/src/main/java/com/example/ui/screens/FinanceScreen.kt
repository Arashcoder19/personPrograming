package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TxItem
import com.example.ui.NotebookViewModel
import com.example.ui.components.AppProgressBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val currentMonth by viewModel.financeMonth.collectAsState()
    val showAll by viewModel.financeShowAll.collectAsState()
    val typeFilter by viewModel.financeTypeFilter.collectAsState()
    val searchQuery by viewModel.financeQuery.collectAsState()

    val monthStr = JalaliCalendar.ymString(currentMonth)

    // Filter transactions
    val matchingTxs = state.tx.filter { x ->
        val matchesPeriod = showAll || x.d.startsWith(monthStr)
        val matchesType = typeFilter.isBlank() || x.type == typeFilter
        val matchesQuery = searchQuery.isBlank() ||
                x.t.contains(searchQuery, ignoreCase = true) ||
                x.cat.contains(searchQuery, ignoreCase = true)
        matchesPeriod && matchesType && matchesQuery
    }.sortedByDescending { it.d }

    // Summary calculations for period
    val periodTxs = state.tx.filter { showAll || it.d.startsWith(monthStr) }
    val totalIncome = periodTxs.filter { it.type == "in" }.sumOf { it.amt }
    val totalExpense = periodTxs.filter { it.type == "out" }.sumOf { it.amt }
    val netBalance = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netBalance / totalIncome) * 100).toInt() else null

    // Category breakdown
    val categoryExpenses = remember(periodTxs) {
        val map = mutableMapOf<String, Double>()
        periodTxs.filter { it.type == "out" }.forEach {
            val c = it.cat.ifBlank { "بدون دسته" }
            map[c] = (map[c] ?: 0.0) + it.amt
        }
        map.toList().sortedByDescending { it.second }
    }
    val maxCatExpense = categoryExpenses.maxOfOrNull { it.second } ?: 1.0

    // 6-Month Trend Data
    val past6Months = remember(currentMonth, state.tx) {
        val base = currentMonth
        (-5..0).map { offset ->
            val ym = JalaliCalendar.addMonths(base, offset)
            val ymS = JalaliCalendar.ymString(ym)
            val txs = state.tx.filter { it.d.startsWith(ymS) }
            val inc = txs.filter { it.type == "in" }.sumOf { it.amt }
            val exp = txs.filter { it.type == "out" }.sumOf { it.amt }
            Triple(ym, inc, exp)
        }
    }

    var newType by remember { mutableStateOf("out") }
    var newTitle by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf("") }
    var newAmt by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf(JalaliCalendar.today()) }
    var newPid by remember { mutableStateOf("") }

    var isBudgetsPanelOpen by remember { mutableStateOf(false) }
    var deletingTxId by remember { mutableStateOf<String?>(null) }

    val projectsMap = remember(state.proj) { state.proj.associate { it.id to it.name } }

    val presetCategories = listOf(
        "خوراک", "حمل‌ونقل", "مسکن", "قبوض", "سلامت", "آموزش",
        "تفریح", "پوشاک", "خرید", "اقساط", "هدیه و کمک", "سایر"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "مالی",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Month Selector Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { viewModel.prevFinanceMonth() },
                        enabled = !showAll,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("ماه قبل", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showAll) "همهٔ زمان‌ها" else JalaliCalendar.getMonthLabel(currentMonth),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.nextFinanceMonth() },
                        enabled = !showAll,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("ماه بعد", fontSize = 12.sp)
                    }
                }
                FilterChip(
                    selected = showAll,
                    onClick = { viewModel.toggleFinanceShowAll() },
                    label = { Text("همه", fontSize = 12.sp) }
                )
            }
        }

        // Summary Tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "درآمد",
                        value = JalaliCalendar.formatToman(totalIncome),
                        isPositive = true,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "هزینه",
                        value = JalaliCalendar.formatToman(totalExpense),
                        isPositive = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricTile(
                    label = if (netBalance >= 0) "مانده (پس‌انداز)" else "کسری",
                    value = JalaliCalendar.formatToman(Math.abs(netBalance)),
                    subValue = if (savingsRate != null) "نرخ پس‌انداز: ${JalaliCalendar.toFa(savingsRate)}٪" else "",
                    isPositive = netBalance >= 0
                )
            }
        }

        // Category Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "هزینه به تفکیک دسته",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { isBudgetsPanelOpen = !isBudgetsPanelOpen }) {
                            Text("تنظیم سقف بودجه")
                        }
                    }

                    if (categoryExpenses.isEmpty()) {
                        Text(
                            text = "هزینه‌ای در این بازه ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        categoryExpenses.forEach { (cat, amt) ->
                            val budgetLimit = state.set.budgets[cat] ?: 0.0
                            val pct = if (budgetLimit > 0) (amt / budgetLimit).toFloat() else (amt / maxCatExpense).toFloat()
                            val isOverBudget = budgetLimit > 0 && amt > budgetLimit

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        text = "${JalaliCalendar.formatToman(amt)}${if (budgetLimit > 0) " از " + JalaliCalendar.formatToman(budgetLimit) else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverBudget) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                AppProgressBar(
                                    progress = pct,
                                    color = if (isOverBudget) DangerRed else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Budget limits editor
                    AnimatedVisibility(visible = isBudgetsPanelOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text("تعیین سقف ماهانه برای هر دسته (تومان):", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            presetCategories.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { catName ->
                                        var limitInput by remember(state.set.budgets[catName]) {
                                            mutableStateOf(state.set.budgets[catName]?.let { Math.round(it).toString() } ?: "")
                                        }
                                        OutlinedTextField(
                                            value = limitInput,
                                            onValueChange = {
                                                limitInput = it
                                                val parsed = JalaliCalendar.parseFaDigits(it).toDoubleOrNull() ?: 0.0
                                                viewModel.setBudgetLimit(catName, parsed)
                                            },
                                            label = { Text(catName, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        // 6-Month Visual Trend Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "روند ۶ ماه اخیر",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val maxVal = past6Months.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1.0) ?: 1.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        past6Months.forEach { (ym, inc, exp) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.height(84.dp)
                                ) {
                                    val incHeight = ((inc / maxVal) * 80).dp.coerceAtLeast(2.dp)
                                    val expHeight = ((exp / maxVal) * 80).dp.coerceAtLeast(2.dp)
                                    Box(
                                        modifier = Modifier
                                            .width(12.dp)
                                            .height(incHeight)
                                            .background(SuccessGreen, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(12.dp)
                                            .height(expHeight)
                                            .background(DangerRed, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val mName = JalaliCalendar.formatJalaliReadable(JalaliCalendar.ymString(ym) + "/01").split(" ").getOrNull(1) ?: ""
                                Text(
                                    text = mName.take(6),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(SuccessGreen, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("درآمد", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(DangerRed, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("هزینه", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Add Transaction Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ثبت تراکنش جدید",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row {
                        FilterChip(
                            selected = newType == "out",
                            onClick = { newType = "out" },
                            label = { Text("هزینه") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = newType == "in",
                            onClick = { newType = "in" },
                            label = { Text("درآمد") }
                        )
                    }

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("شرح تراکنش (مثلاً: اجاره، خرید کتاب)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newAmt,
                            onValueChange = { newAmt = it },
                            placeholder = { Text("مبلغ (تومان)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newCat,
                            onValueChange = { newCat = it },
                            placeholder = { Text("دسته") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Category Quick Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presetCategories.take(4).forEach { cat ->
                            SuggestionChip(
                                onClick = { newCat = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newDate,
                            onValueChange = { newDate = it },
                            placeholder = { Text("تاریخ 1405/01/01") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        if (state.proj.isNotEmpty()) {
                            var expandedProj by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { expandedProj = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(projectsMap[newPid] ?: "پروژه", fontSize = 12.sp, maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = expandedProj,
                                    onDismissRequest = { expandedProj = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("بدون پروژه") },
                                        onClick = { newPid = ""; expandedProj = false }
                                    )
                                    state.proj.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p.name) },
                                            onClick = { newPid = p.id; expandedProj = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val amtNum = JalaliCalendar.parseFaDigits(newAmt).toDoubleOrNull() ?: 0.0
                            val normDate = JalaliCalendar.normalizeDate(newDate) ?: JalaliCalendar.today()
                            if (newTitle.isNotBlank() && amtNum > 0) {
                                viewModel.addTransaction(
                                    type = newType,
                                    title = newTitle.trim(),
                                    cat = newCat.trim(),
                                    amt = amtNum,
                                    date = normDate,
                                    pid = newPid
                                )
                                newTitle = ""
                                newAmt = ""
                                newCat = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("ثبت تراکنش")
                    }
                }
            }
        }

        // Filters & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to "همه", "out" to "هزینه‌ها", "in" to "درآمدها").forEach { (k, v) ->
                        FilterChip(
                            selected = typeFilter == k,
                            onClick = { viewModel.financeTypeFilter.value = k },
                            label = { Text(v) }
                        )
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.financeQuery.value = it },
                    placeholder = { Text("جستجو در شرح و دستهٔ تراکنش‌ها…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Transactions List
        if (matchingTxs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("تراکنشی در این بازه پیدا نشد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(matchingTxs, key = { it.id }) { tx ->
                val isIncome = tx.type == "in"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(38.dp)
                                .background(if (isIncome) SuccessGreen else DangerRed, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.t.ifBlank { "(بدون شرح)" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (tx.cat.isNotBlank()) {
                                    StatusBadge(text = tx.cat, color = MaterialTheme.colorScheme.primary)
                                }
                                val pName = projectsMap[tx.pid]
                                if (!pName.isNullOrBlank()) {
                                    StatusBadge(text = pName, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text(
                                    text = JalaliCalendar.formatJalaliReadable(tx.d),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = (if (isIncome) "+ " else "- ") + JalaliCalendar.formatToman(tx.amt),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) SuccessGreen else DangerRed
                            )
                        )

                        IconButton(onClick = { deletingTxId = tx.id }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    deletingTxId?.let { txId ->
        ConfirmDeleteDialog(
            title = "حذف تراکنش",
            message = "آیا از حذف این تراکنش اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteTransaction(txId)
                deletingTxId = null
            },
            onDismiss = { deletingTxId = null }
        )
    }
}
