package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.GrowSubTab
import com.example.ui.NotebookViewModel
import com.example.ui.components.AppProgressBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@Composable
fun GrowScreen(viewModel: NotebookViewModel) {
    val currentSubTab by viewModel.growSubTab.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "رشد فردی",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // SubTab selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GrowSubTab.values().forEach { tab ->
                FilterChip(
                    selected = currentSubTab == tab,
                    onClick = { viewModel.setGrowSubTab(tab) },
                    label = { Text(tab.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (currentSubTab) {
            GrowSubTab.HABIT -> HabitsSection(viewModel)
            GrowSubTab.WORK -> WorkoutsSection(viewModel)
            GrowSubTab.READ -> ReadingSection(viewModel)
            GrowSubTab.SKILL -> SkillsSection(viewModel)
            GrowSubTab.JOURNAL -> JournalSection(viewModel)
        }
    }
}

// ---------------- 1. HABITS SUBTAB ----------------
@Composable
fun HabitsSection(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val today = JalaliCalendar.today()
    val days14 = remember { JalaliCalendar.lastDays(14) }

    var newHabitName by remember { mutableStateOf("") }
    var deletingHabitId by remember { mutableStateOf<String?>(null) }

    val activeHabits = state.habit.filterNot { it.arch }
    val archivedHabits = state.habit.filter { it.arch }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newHabitName,
                        onValueChange = { newHabitName = it },
                        placeholder = { Text("عادت تازه (مثلاً: ۳۰ دقیقه مطالعه)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newHabitName.isNotBlank()) {
                                viewModel.addHabit(newHabitName.trim())
                                newHabitName = ""
                            }
                        }
                    ) {
                        Text("افزودن")
                    }
                }
            }
        }

        if (activeHabits.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("هنوز عادتی تعریف نکرده‌اید. یک عادت کوچک بنویسید و هر روز آن را تیک بزنید.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(activeHabits, key = { it.id }) { habit ->
                val streak = viewModel.getHabitStreak(habit)
                val logSet = habit.log.toSet()
                val last30 = JalaliCalendar.lastDays(30)
                val rate30 = (last30.count { logSet.contains(it) } * 100) / 30

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(habit.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(text = "${JalaliCalendar.toFa(streak)} روز پیاپی", color = SuccessGreen)
                                StatusBadge(text = "${JalaliCalendar.toFa(rate30)}٪ ۳۰ روزه", color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { viewModel.toggleHabitArchive(habit.id) }) {
                                    Text("بایگانی", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { deletingHabitId = habit.id }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("۱۴ روز اخیر (برای ثبت لمس کنید):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days14.forEach { d ->
                                val isDone = logSet.contains(d)
                                val isToday = d == today
                                val dayNum = d.split("/").lastOrNull()?.toIntOrNull() ?: 1

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDone) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                        .border(
                                            width = if (isToday) 2.dp else 1.dp,
                                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.toggleHabitDay(habit.id, d) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = JalaliCalendar.toFa(dayNum),
                                        fontSize = 10.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (archivedHabits.isNotEmpty()) {
            item {
                Text("عادت‌های بایگانی‌شده (${JalaliCalendar.toFa(archivedHabits.size)}):", style = MaterialTheme.typography.titleSmall)
            }
            items(archivedHabits) { habit ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(habit.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        TextButton(onClick = { viewModel.toggleHabitArchive(habit.id) }) {
                            Text("بازگردانی")
                        }
                        IconButton(onClick = { deletingHabitId = habit.id }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    deletingHabitId?.let { habitId ->
        ConfirmDeleteDialog(
            title = "حذف عادت",
            message = "آیا از حذف این عادت اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteHabit(habitId)
                deletingHabitId = null
            },
            onDismiss = { deletingHabitId = null }
        )
    }
}

// ---------------- 2. WORKOUTS SUBTAB ----------------
@Composable
fun WorkoutsSection(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val today = JalaliCalendar.today()
    val last7 = remember { JalaliCalendar.lastDays(7).toSet() }
    val last30 = remember { JalaliCalendar.lastDays(30).toSet() }
    val last14 = remember { JalaliCalendar.lastDays(14) }

    val workouts7 = state.work.filter { last7.contains(it.d) }
    val workoutMin7 = workouts7.sumOf { it.min }
    val activeDays7 = workouts7.map { it.d }.distinct().size
    val sessions30 = state.work.count { last30.contains(it.d) }

    var newType by remember { mutableStateOf("پیاده‌روی") }
    var newMin by remember { mutableStateOf("30") }
    var newInt by remember { mutableStateOf(2) }
    var newNote by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf(today) }

    var deletingWorkId by remember { mutableStateOf<String?>(null) }

    val presetTypes = listOf("پیاده‌روی", "دویدن", "باشگاه و وزنه", "شنا", "دوچرخه", "یوگا و کشش", "ورزش گروهی", "طناب")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "دقیقه ورزش (۷ روز)",
                        value = "${JalaliCalendar.toFa(workoutMin7)} دقیقه",
                        isPositive = workoutMin7 > 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "روزهای فعال در هفته",
                        value = "${JalaliCalendar.toFa(activeDays7)} از ۷ روز",
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricTile(
                    label = "جلسات تمرین در ۳۰ روز اخیر",
                    value = "${JalaliCalendar.toFa(sessions30)} جلسه",
                    isPositive = sessions30 >= 8
                )
            }
        }

        // 14-day Sparkline Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("دقیقهٔ ورزش در ۱۴ روز اخیر:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(10.dp))

                    val dayMins = last14.map { d ->
                        state.work.filter { it.d == d }.sumOf { it.min }
                    }
                    val maxMin = (dayMins.maxOrNull() ?: 1).coerceAtLeast(30)

                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last14.forEachIndexed { i, d ->
                            val m = dayMins[i]
                            val h = ((m.toFloat() / maxMin) * 60).dp.coerceAtLeast(3.dp)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(h)
                                        .background(
                                            if (m > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val dayNum = d.split("/").lastOrNull()?.toIntOrNull() ?: 1
                                Text(JalaliCalendar.toFa(dayNum), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Log Workout Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ثبت تمرین ورزشی", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presetTypes.take(4).forEach { t ->
                            FilterChip(
                                selected = newType == t,
                                onClick = { newType = t },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newType,
                            onValueChange = { newType = it },
                            label = { Text("نوع ورزش") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newMin,
                            onValueChange = { newMin = it },
                            label = { Text("مدت (دقیقه)") },
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newDate,
                            onValueChange = { newDate = it },
                            label = { Text("تاریخ") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("شدت:", style = MaterialTheme.typography.bodySmall)
                            listOf(1 to "سبک", 2 to "متوسط", 3 to "سنگین").forEach { (v, l) ->
                                FilterChip(
                                    selected = newInt == v,
                                    onClick = { newInt = v },
                                    label = { Text(l, fontSize = 11.sp) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        placeholder = { Text("یادداشت تمرین (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val minVal = JalaliCalendar.parseFaDigits(newMin).toIntOrNull() ?: 0
                            val normD = JalaliCalendar.normalizeDate(newDate) ?: today
                            if (newType.isNotBlank() && minVal > 0) {
                                viewModel.addWorkout(newType.trim(), minVal, newInt, normD, newNote.trim())
                                newNote = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("ثبت تمرین")
                    }
                }
            }
        }

        // Workout History
        items(state.work.sortedByDescending { it.d }) { w ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(w.type, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            StatusBadge(text = "${JalaliCalendar.toFa(w.min)} دقیقه", color = MaterialTheme.colorScheme.primary)
                            val intLabel = when (w.intVal) {
                                1 -> "سبک"
                                3 -> "سنگین"
                                else -> "متوسط"
                            }
                            StatusBadge(text = intLabel, color = WarningAmber)
                        }
                        if (w.note.isNotBlank()) {
                            Text(w.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(JalaliCalendar.formatJalaliReadable(w.d), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { deletingWorkId = w.id }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    deletingWorkId?.let { workId ->
        ConfirmDeleteDialog(
            title = "حذف تمرین",
            message = "آیا از حذف این جلسه تمرین اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteWorkout(workId)
                deletingWorkId = null
            },
            onDismiss = { deletingWorkId = null }
        )
    }
}

// ---------------- 3. READING SUBTAB ----------------
@Composable
fun ReadingSection(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val today = JalaliCalendar.today()
    val last7 = remember { JalaliCalendar.lastDays(7).toSet() }

    val todayPages = state.rlog.filter { it.d == today }.sumOf { it.pages }
    val last7Pages = state.rlog.filter { last7.contains(it.d) }.sumOf { it.pages }
    val readingCount = state.book.count { it.st == "reading" }
    val doneCount = state.book.count { it.st == "done" }

    var newBookTitle by remember { mutableStateOf("") }
    var newBookAuthor by remember { mutableStateOf("") }
    var newBookPages by remember { mutableStateOf("") }

    var deletingBookId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "صفحه امروز",
                        value = "${JalaliCalendar.toFa(todayPages)} صفحه",
                        isPositive = todayPages > 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "۷ روز اخیر",
                        value = "${JalaliCalendar.toFa(last7Pages)} صفحه",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "در حال مطالعه",
                        value = "${JalaliCalendar.toFa(readingCount)} کتاب",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "تمام‌شده امسال",
                        value = "${JalaliCalendar.toFa(doneCount)} کتاب",
                        isPositive = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Add Book Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("افزودن کتاب تازه", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = newBookTitle,
                        onValueChange = { newBookTitle = it },
                        placeholder = { Text("عنوان کتاب") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newBookAuthor,
                            onValueChange = { newBookAuthor = it },
                            placeholder = { Text("نویسنده") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newBookPages,
                            onValueChange = { newBookPages = it },
                            placeholder = { Text("تعداد صفحه") },
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            val pagesNum = JalaliCalendar.parseFaDigits(newBookPages).toIntOrNull() ?: 0
                            if (newBookTitle.isNotBlank()) {
                                viewModel.addBook(newBookTitle.trim(), newBookAuthor.trim(), pagesNum, "")
                                newBookTitle = ""
                                newBookAuthor = ""
                                newBookPages = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("افزودن کتاب")
                    }
                }
            }
        }

        // Books List
        items(state.book, key = { it.id }) { book ->
            var isExpanded by remember { mutableStateOf(false) }
            var sessionPages by remember { mutableStateOf("15") }
            var sessionMin by remember { mutableStateOf("25") }

            val pct = if (book.pages > 0) (book.cur.toFloat() / book.pages).coerceIn(0f, 1f) else 0f
            val statusLabel = when (book.st) {
                "reading" -> "در حال مطالعه"
                "done" -> "تمام شد"
                "pause" -> "متوقف"
                else -> "در فهرست"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.t, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                if (book.author.isNotBlank()) {
                                    Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            StatusBadge(
                                text = statusLabel,
                                color = if (book.st == "done") SuccessGreen else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        AppProgressBar(progress = pct)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (book.pages > 0) "${JalaliCalendar.toFa(book.cur)} از ${JalaliCalendar.toFa(book.pages)} صفحه (${JalaliCalendar.toFa((pct * 100).toInt())}٪)" else "${JalaliCalendar.toFa(book.cur)} صفحه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (book.rate > 0) {
                                Row {
                                    repeat(book.rate) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
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
                            Text("ثبت مطالعه برای این کتاب:", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = sessionPages,
                                    onValueChange = { sessionPages = it },
                                    label = { Text("صفحه") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = sessionMin,
                                    onValueChange = { sessionMin = it },
                                    label = { Text("دقیقه") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val p = JalaliCalendar.parseFaDigits(sessionPages).toIntOrNull() ?: 0
                                        val m = JalaliCalendar.parseFaDigits(sessionMin).toIntOrNull() ?: 0
                                        if (p > 0 || m > 0) {
                                            viewModel.addReadingLog(book.id, p, m, JalaliCalendar.today())
                                        }
                                    }
                                ) {
                                    Text("ثبت")
                                }
                            }

                            if (book.note.isNotBlank()) {
                                Text("نکته‌ها: ${book.note}", style = MaterialTheme.typography.bodySmall)
                            }

                            TextButton(
                                onClick = { deletingBookId = book.id },
                                colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف کتاب")
                            }
                        }
                    }
                }
            }
        }
    }

    deletingBookId?.let { bookId ->
        ConfirmDeleteDialog(
            title = "حذف کتاب",
            message = "آیا از حذف این کتاب و یادداشت‌های مطالعه آن اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteBook(bookId)
                deletingBookId = null
            },
            onDismiss = { deletingBookId = null }
        )
    }
}

// ---------------- 4. SKILLS SUBTAB ----------------
@Composable
fun SkillsSection(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    var newSkillName by remember { mutableStateOf("") }
    var newSkillTarget by remember { mutableStateOf("100") }

    var deletingSkillId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSkillName,
                        onValueChange = { newSkillName = it },
                        placeholder = { Text("مهارت تازه (مثلاً: طراحی UI)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val targetNum = JalaliCalendar.parseFaDigits(newSkillTarget).toIntOrNull() ?: 100
                            if (newSkillName.isNotBlank()) {
                                viewModel.addSkill(newSkillName.trim(), targetNum, "", "")
                                newSkillName = ""
                            }
                        }
                    ) {
                        Text("افزودن")
                    }
                }
            }
        }

        items(state.skill, key = { it.id }) { skill ->
            var isExpanded by remember { mutableStateOf(false) }
            var practiceMin by remember { mutableStateOf("45") }
            var practiceNote by remember { mutableStateOf("") }

            val practiceTotalMin = state.sprac.filter { it.sid == skill.id }.sumOf { it.min }
            val practiceHours = String.format("%.1f", practiceTotalMin / 60.0)
            val pct = if (skill.target > 0) (skill.level.toFloat() / skill.target).coerceIn(0f, 1f) else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
                            Text(skill.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "سطح ${JalaliCalendar.toFa(skill.level)}٪ از ${JalaliCalendar.toFa(skill.target)}٪",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AppProgressBar(progress = pct)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${JalaliCalendar.toFa(practiceHours)} ساعت تمرین ثبت‌شده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("ثبت جلسه تمرین:", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = practiceMin,
                                    onValueChange = { practiceMin = it },
                                    label = { Text("دقیقه") },
                                    modifier = Modifier.weight(0.7f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = practiceNote,
                                    onValueChange = { practiceNote = it },
                                    label = { Text("شرح تمرین") },
                                    modifier = Modifier.weight(1.3f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val m = JalaliCalendar.parseFaDigits(practiceMin).toIntOrNull() ?: 0
                                        if (m > 0) {
                                            viewModel.addSkillPractice(skill.id, m, practiceNote.trim(), JalaliCalendar.today())
                                            practiceNote = ""
                                        }
                                    }
                                ) {
                                    Text("ثبت")
                                }
                            }

                            // Update Level slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("سطح فعلی: ${JalaliCalendar.toFa(skill.level)}٪", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(110.dp))
                                Slider(
                                    value = skill.level.toFloat(),
                                    onValueChange = { viewModel.updateSkill(skill.copy(level = it.toInt())) },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            TextButton(
                                onClick = { deletingSkillId = skill.id },
                                colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف مهارت")
                            }
                        }
                    }
                }
            }
        }
    }

    deletingSkillId?.let { skillId ->
        ConfirmDeleteDialog(
            title = "حذف مهارت",
            message = "آیا از حذف این مهارت اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteSkill(skillId)
                deletingSkillId = null
            },
            onDismiss = { deletingSkillId = null }
        )
    }
}

// ---------------- 5. JOURNAL SUBTAB ----------------
@Composable
fun JournalSection(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val selectedDate by viewModel.journalSelectedDate.collectAsState()
    val today = JalaliCalendar.today()
    val last14 = remember { JalaliCalendar.lastDays(14) }

    val currentJournal = state.jour.find { it.d == selectedDate }

    var mood by remember(currentJournal?.mood) { mutableStateOf(currentJournal?.mood ?: 0) }
    var gratitude by remember(currentJournal?.gr) { mutableStateOf(currentJournal?.gr ?: "") }
    var lesson by remember(currentJournal?.lesson) { mutableStateOf(currentJournal?.lesson ?: "") }
    var note by remember(currentJournal?.txt) { mutableStateOf(currentJournal?.txt ?: "") }

    val moodEmojis = listOf("😞", "😕", "😐", "🙂", "😄")
    val moodNames = listOf("خیلی بد", "بد", "معمولی", "خوب", "عالی")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Daily Journal Card
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
                            text = "دفتر روز: ${JalaliCalendar.formatJalaliReadable(selectedDate)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (selectedDate != today) {
                            TextButton(onClick = { viewModel.journalSelectedDate.value = today }) {
                                Text("امروز")
                            }
                        }
                    }

                    // Mood selector
                    Text("حال و هوای این روز:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..5).forEach { m ->
                            FilterChip(
                                selected = mood == m,
                                onClick = {
                                    mood = if (mood == m) 0 else m
                                    viewModel.saveJournal(selectedDate, mood, gratitude, lesson, note)
                                },
                                label = { Text("${moodEmojis[m - 1]} ${moodNames[m - 1]}", fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = gratitude,
                        onValueChange = {
                            gratitude = it
                            viewModel.saveJournal(selectedDate, mood, it, lesson, note)
                        },
                        label = { Text("سه چیزی که برایشان سپاسگزارم") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = lesson,
                        onValueChange = {
                            lesson = it
                            viewModel.saveJournal(selectedDate, mood, gratitude, it, note)
                        },
                        label = { Text("درسی که امروز گرفتم") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                            viewModel.saveJournal(selectedDate, mood, gratitude, lesson, it)
                        },
                        label = { Text("یادداشت و تأملات آزاد روز") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        }

        // 14-day Mood dots
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("حال و هوا در ۱۴ روز اخیر:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        last14.forEach { d ->
                            val entry = state.jour.find { it.d == d }
                            val m = entry?.mood ?: 0
                            val isSelected = d == selectedDate

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { viewModel.journalSelectedDate.value = d }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (m in 1..5) moodEmojis[m - 1] else "·",
                                    fontSize = 16.sp
                                )
                                val dayNum = d.split("/").lastOrNull()?.toIntOrNull() ?: 1
                                Text(JalaliCalendar.toFa(dayNum), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Recent Entries List
        val pastJournals = state.jour.filter { it.d != selectedDate && (it.lesson.isNotBlank() || it.txt.isNotBlank() || it.gr.isNotBlank()) }
            .sortedByDescending { it.d }
        if (pastJournals.isNotEmpty()) {
            item {
                Text("نوشته‌های قبلی:", style = MaterialTheme.typography.titleSmall)
            }
            items(pastJournals) { j ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.journalSelectedDate.value = j.d },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(JalaliCalendar.formatJalaliReadable(j.d), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (j.mood in 1..5) {
                                Text("${moodEmojis[j.mood - 1]} ${moodNames[j.mood - 1]}", fontSize = 12.sp)
                            }
                        }
                        if (j.lesson.isNotBlank()) {
                            Text("درس روز: ${j.lesson}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        } else if (j.txt.isNotBlank()) {
                            Text(j.txt, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
