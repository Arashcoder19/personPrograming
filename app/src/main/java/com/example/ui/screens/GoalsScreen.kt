package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalItem
import com.example.ui.NotebookViewModel
import com.example.ui.components.AppProgressBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val statusFilter by viewModel.goalStatusFilter.collectAsState()
    val catFilter by viewModel.goalCatFilter.collectAsState()
    val searchQuery by viewModel.goalQuery.collectAsState()

    val activeGoals = state.goal.filter { it.st == "active" }
    val avgProgress = if (activeGoals.isNotEmpty()) activeGoals.sumOf { it.prog }.toInt() / activeGoals.size else 0

    val filteredGoals = state.goal.filter { g ->
        val matchesStatus = statusFilter.isBlank() || g.st == statusFilter
        val matchesCat = catFilter.isBlank() || g.cat == catFilter
        val matchesQuery = searchQuery.isBlank() ||
                g.t.contains(searchQuery, ignoreCase = true) ||
                g.desc.contains(searchQuery, ignoreCase = true) ||
                g.kpi.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesCat && matchesQuery
    }.sortedWith(compareBy({ it.st != "active" }, { it.due.ifBlank { "9" } }))

    var newTitle by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf("personal") }
    var newHz by remember { mutableStateOf("mid") }
    var newDue by remember { mutableStateOf("") }
    var newKpi by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newPid by remember { mutableStateOf("") }
    var isNewFormExpanded by remember { mutableStateOf(false) }

    var deletingGoalId by remember { mutableStateOf<String?>(null) }

    val categoryLabels = mapOf(
        "personal" to "فردی",
        "career" to "شغلی",
        "finance" to "مالی",
        "health" to "سلامت و ورزش",
        "learning" to "یادگیری",
        "relation" to "روابط",
        "other" to "سایر"
    )

    val horizonLabels = mapOf(
        "short" to "کوتاه‌مدت",
        "mid" to "میان‌مدت",
        "long" to "بلندمدت"
    )

    val statusLabels = mapOf(
        "active" to "فعال",
        "done" to "محقق شد",
        "hold" to "متوقف",
        "dropped" to "رها شد"
    )

    val projectsMap = remember(state.proj) { state.proj.associate { it.id to it.name } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "اهداف",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${JalaliCalendar.toFa(state.goal.size)} هدف، ${JalaliCalendar.toFa(activeGoals.size)} فعال، میانگین پیشرفت ${JalaliCalendar.toFa(avgProgress)}٪",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Add Goal Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "افزودن هدف تازه",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { isNewFormExpanded = !isNewFormExpanded }) {
                            Icon(
                                if (isNewFormExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "تنظیمات"
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("هدف تازه (مثلاً: ۱۲ کتاب تا پایان سال)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    AnimatedVisibility(visible = isNewFormExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Category chips
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("دسته:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(50.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    categoryLabels.toList().take(4).forEach { (k, v) ->
                                        FilterChip(
                                            selected = newCat == k,
                                            onClick = { newCat = k },
                                            label = { Text(v, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Horizon & Due
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = newDue,
                                    onValueChange = { newDue = it },
                                    placeholder = { Text("موعد 1405/01/01") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                var expandedHz by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(0.8f)) {
                                    OutlinedButton(
                                        onClick = { expandedHz = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(horizonLabels[newHz] ?: "افق زمانی", fontSize = 12.sp)
                                    }
                                    DropdownMenu(
                                        expanded = expandedHz,
                                        onDismissRequest = { expandedHz = false }
                                    ) {
                                        horizonLabels.forEach { (k, v) ->
                                            DropdownMenuItem(
                                                text = { Text(v) },
                                                onClick = { newHz = k; expandedHz = false }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newKpi,
                                onValueChange = { newKpi = it },
                                placeholder = { Text("معیار موفقیت (چطور بفهمم رسیدم؟)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newDesc,
                                onValueChange = { newDesc = it },
                                placeholder = { Text("توضیحات و چرایی هدف…") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                val normDue = JalaliCalendar.normalizeDate(newDue) ?: ""
                                viewModel.addGoal(
                                    title = newTitle.trim(),
                                    cat = newCat,
                                    hz = newHz,
                                    due = normDue,
                                    kpi = newKpi.trim(),
                                    desc = newDesc.trim(),
                                    pid = newPid
                                )
                                newTitle = ""
                                newDue = ""
                                newKpi = ""
                                newDesc = ""
                                isNewFormExpanded = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("افزودن هدف")
                    }
                }
            }
        }

        // Filters & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("" to "همه", "active" to "فعال", "done" to "محقق شد").forEach { (k, v) ->
                        FilterChip(
                            selected = statusFilter == k,
                            onClick = { viewModel.goalStatusFilter.value = k },
                            label = { Text(v, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.goalQuery.value = it },
                    placeholder = { Text("جستجو در اهداف و معیارها…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Goals List
        if (filteredGoals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.goal.isEmpty()) "هنوز هدفی ثبت نشده است. هدفی مشخص با موعد بنویسید و گام‌هایش را مشخص کنید." else "هدفی با این فیلتر پیدا نشد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredGoals, key = { it.id }) { goal ->
                GoalCardItem(
                    goal = goal,
                    categoryLabels = categoryLabels,
                    horizonLabels = horizonLabels,
                    statusLabels = statusLabels,
                    onAddKeyResult = { viewModel.addKeyResult(goal.id, it) },
                    onToggleKeyResult = { viewModel.toggleKeyResult(goal.id, it) },
                    onDeleteKeyResult = { viewModel.deleteKeyResult(goal.id, it) },
                    onDelete = { deletingGoalId = goal.id }
                )
            }
        }
    }

    deletingGoalId?.let { goalId ->
        ConfirmDeleteDialog(
            title = "حذف هدف",
            message = "آیا از حذف این هدف اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteGoal(goalId)
                deletingGoalId = null
            },
            onDismiss = { deletingGoalId = null }
        )
    }
}

@Composable
fun GoalCardItem(
    goal: GoalItem,
    categoryLabels: Map<String, String>,
    horizonLabels: Map<String, String>,
    statusLabels: Map<String, String>,
    onAddKeyResult: (String) -> Unit,
    onToggleKeyResult: (String) -> Unit,
    onDeleteKeyResult: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var newKrTitle by remember { mutableStateOf("") }

    val catName = categoryLabels[goal.cat] ?: goal.cat
    val hzName = horizonLabels[goal.hz] ?: goal.hz
    val stName = statusLabels[goal.st] ?: goal.st

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        StatusBadge(text = catName, color = MaterialTheme.colorScheme.primary)
                        StatusBadge(text = hzName, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = goal.t,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${JalaliCalendar.toFa(goal.prog.toInt())}٪",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                AppProgressBar(progress = (goal.prog / 100.0).toFloat())

                if (goal.due.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "موعد: ${JalaliCalendar.formatJalaliReadable(goal.due)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (goal.kpi.isNotBlank()) {
                        Text(
                            text = "معیار موفقیت: ${goal.kpi}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (goal.desc.isNotBlank()) {
                        Text(
                            text = goal.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Key Results / Steps Checklist
                    Text(
                        text = "گام‌ها و نتایج کلیدی:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newKrTitle,
                            onValueChange = { newKrTitle = it },
                            placeholder = { Text("گام یا نتیجه کلیدی تازه…", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newKrTitle.isNotBlank()) {
                                    onAddKeyResult(newKrTitle.trim())
                                    newKrTitle = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزودن")
                        }
                    }

                    goal.kr.forEach { kr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleKeyResult(kr.id) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = kr.done,
                                    onCheckedChange = { onToggleKeyResult(kr.id) }
                                )
                                Text(
                                    text = kr.t,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = if (kr.done) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (kr.done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { onDeleteKeyResult(kr.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف گام", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Delete Goal
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف هدف", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف این هدف")
                    }
                }
            }
        }
    }
}
