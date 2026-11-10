package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppSection
import com.example.ui.GrowSubTab
import com.example.ui.NotebookViewModel
import com.example.ui.components.AppProgressBar
import com.example.ui.components.MetricTile
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@Composable
fun TodayScreen(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val today = JalaliCalendar.today()
    val weekday = JalaliCalendar.currentWeekday()
    val todayReadable = JalaliCalendar.formatJalaliReadable(today)

    // Filter tasks for today: open tasks due today or earlier, or doing status, plus tasks finished today
    val openTodayTasks = state.task.filter { t ->
        t.st != "done" && ((t.due.isNotBlank() && t.due <= today) || t.st == "doing")
    }.sortedWith(compareBy({ it.st == "done" }, { it.due.ifBlank { "9" } }))

    val doneTodayTasks = state.task.filter { t ->
        t.st == "done" && t.doneAt == today
    }

    val activeHabits = state.habit.filterNot { it.arch }
    val doneHabitsToday = activeHabits.filter { it.log.contains(today) }

    val totalItems = openTodayTasks.size + doneTodayTasks.size + activeHabits.size
    val doneItems = doneTodayTasks.size + doneHabitsToday.size
    val todayProgress = if (totalItems > 0) doneItems.toFloat() / totalItems else 0f

    // Financial calculation for current month
    val ym = JalaliCalendar.currentYearMonth()
    val ymStr = JalaliCalendar.ymString(ym)
    val monthTxs = state.tx.filter { it.d.startsWith(ymStr) }
    val monthIncome = monthTxs.filter { it.type == "in" }.sumOf { it.amt }
    val monthExpense = monthTxs.filter { it.type == "out" }.sumOf { it.amt }
    val monthNet = monthIncome - monthExpense

    // Workout last 7 days
    val last7 = JalaliCalendar.lastDays(7).toSet()
    val workouts7 = state.work.filter { last7.contains(it.d) }
    val workoutMin7 = workouts7.sumOf { it.min }

    // Reading last 7 days
    val reading7 = state.rlog.filter { last7.contains(it.d) }
    val readingPages7 = reading7.sumOf { it.pages }

    // Tasks metrics
    val allOpenTasks = state.task.filter { it.st != "done" }
    val overdueTasksCount = allOpenTasks.count { it.due.isNotBlank() && it.due < today }

    // Journal entry for today
    val todayJournal = state.jour.find { it.d == today }
    val todayMood = todayJournal?.mood ?: 0

    // Active goals
    val activeGoals = state.goal.filter { it.st == "active" }.take(4)

    // Open resolutions from sessions
    val openResolutions = state.sess.flatMap { s ->
        s.res.filter { it.st != "done" }.map { Pair(s, it) }
    }.sortedBy { it.second.due.ifBlank { "9" } }.take(4)

    var quickTaskTitle by remember { mutableStateOf("") }
    var quickTaskArea by remember { mutableStateOf("personal") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = todayReadable,
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = weekday,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${JalaliCalendar.toFa((todayProgress * 100).toInt())}٪",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (totalItems > 0)
                            "پیشرفت امروز: ${JalaliCalendar.toFa(doneItems)} از ${JalaliCalendar.toFa(totalItems)} مورد"
                        else
                            "برای امروز هنوز تسک یا عادتی تعیین نشده است",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AppProgressBar(progress = todayProgress)
                }
            }
        }

        // Summary Metric Tiles Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "خالص این ماه",
                        value = JalaliCalendar.formatToman(monthNet),
                        subValue = "درآمد ${JalaliCalendar.formatTomanWithoutUnit(monthIncome)} / هزینه ${JalaliCalendar.formatTomanWithoutUnit(monthExpense)}",
                        isPositive = monthNet >= 0,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppSection.FINANCE) }
                    )
                    MetricTile(
                        label = "ورزش ۷ روز اخیر",
                        value = "${JalaliCalendar.toFa(workoutMin7)} دقیقه",
                        subValue = "${JalaliCalendar.toFa(workouts7.size)} جلسه تمرین",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppSection.GROW, GrowSubTab.WORK) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "مطالعه ۷ روز اخیر",
                        value = "${JalaliCalendar.toFa(readingPages7)} صفحه",
                        subValue = "در بخش رشد فردی",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppSection.GROW, GrowSubTab.READ) }
                    )
                    MetricTile(
                        label = "تسک‌های باز",
                        value = JalaliCalendar.toFa(allOpenTasks.size),
                        subValue = if (overdueTasksCount > 0) "${JalaliCalendar.toFa(overdueTasksCount)} عقب‌افتاده" else "در جریان",
                        isPositive = if (overdueTasksCount > 0) false else null,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppSection.TASKS) }
                    )
                }
            }
        }

        // Today's Tasks Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "کارهای امروز",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Add Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickTaskTitle,
                            onValueChange = { quickTaskTitle = it },
                            placeholder = { Text("کار تازه برای امروز…", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (quickTaskTitle.isNotBlank()) {
                                    viewModel.addQuickTaskToday(quickTaskTitle.trim(), quickTaskArea)
                                    quickTaskTitle = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزودن")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val combinedTasks = openTodayTasks + doneTodayTasks
                    if (combinedTasks.isEmpty()) {
                        Text(
                            text = "کاری برای امروز نیست. از کادر بالا اضافه کنید یا به بخش تسک‌ها بروید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        combinedTasks.forEach { task ->
                            val isDone = task.st == "done"
                            val isLate = !isDone && task.due.isNotBlank() && task.due < today
                            val priorityColor = when (task.prio) {
                                "high" -> DangerRed
                                "low" -> SuccessGreen
                                else -> WarningAmber
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { viewModel.toggleTask(task.id) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .background(priorityColor, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Checkbox(
                                    checked = isDone,
                                    onCheckedChange = { viewModel.toggleTask(task.id) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.t,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (task.area.isNotBlank()) {
                                            val areaName = when (task.area) {
                                                "work" -> "کاری"
                                                "personal" -> "شخصی"
                                                "home" -> "خانه"
                                                "study" -> "یادگیری"
                                                else -> task.area
                                            }
                                            Text(areaName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (isLate) {
                                            Text("عقب‌افتاده", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }

        // Today's Habits Card
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
                            text = "عادت‌های امروز",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { viewModel.navigateTo(AppSection.GROW, GrowSubTab.HABIT) }) {
                            Text("مدیریت عادت‌ها")
                        }
                    }

                    if (activeHabits.isEmpty()) {
                        Text(
                            text = "هنوز عادتی ثبت نشده است. از بخش رشد فردی عادت اضافه کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        activeHabits.forEach { habit ->
                            val doneToday = habit.log.contains(today)
                            val streak = viewModel.getHabitStreak(habit)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.toggleHabitToday(habit.id) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = doneToday,
                                        onCheckedChange = { viewModel.toggleHabitToday(habit.id) }
                                    )
                                    Text(
                                        text = habit.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                StatusBadge(
                                    text = "${JalaliCalendar.toFa(streak)} روز پیاپی",
                                    color = if (streak > 0) SuccessGreen else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "امروز چطور بود؟",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val moods = listOf(
                            Pair(1, "😞 خیلی بد"),
                            Pair(2, "😕 بد"),
                            Pair(3, "😐 معمولی"),
                            Pair(4, "🙂 خوب"),
                            Pair(5, "😄 عالی")
                        )
                        moods.forEach { (m, label) ->
                            val isSelected = todayMood == m
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTodayMood(m) },
                                label = {
                                    Text(label, fontSize = 11.sp, maxLines = 1)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // Active Goals Summary
        if (activeGoals.isNotEmpty()) {
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
                                text = "اهداف فعال",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { viewModel.navigateTo(AppSection.GOALS) }) {
                                Text("مشاهده همه")
                            }
                        }
                        activeGoals.forEach { goal ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = goal.t,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${JalaliCalendar.toFa(goal.prog.toInt())}٪",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                AppProgressBar(progress = (goal.prog / 100.0).toFloat())
                            }
                        }
                    }
                }
            }
        }

        // Open Session Resolutions
        if (openResolutions.isNotEmpty()) {
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
                                text = "مصوبات باز جلسات تفکر",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { viewModel.navigateTo(AppSection.SESSIONS) }) {
                                Text("جلسات تفکر")
                            }
                        }
                        openResolutions.forEach { (sess, res) ->
                            val isLate = res.due.isNotBlank() && res.due < today
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = res.t,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sess.t}${if (res.due.isNotBlank()) " · مهلت: " + JalaliCalendar.formatJalaliReadable(res.due) else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLate) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (res.tid.isBlank()) {
                                    TextButton(onClick = { viewModel.convertResolutionToTask(sess.id, res.id) }) {
                                        Text("به تسک‌ها", fontSize = 12.sp)
                                    }
                                } else {
                                    StatusBadge(text = "در تسک‌ها هست", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}
