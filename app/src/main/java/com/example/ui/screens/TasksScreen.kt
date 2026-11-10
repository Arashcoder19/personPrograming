package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.example.ui.NotebookViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val statusFilter by viewModel.taskStatusFilter.collectAsState()
    val areaFilter by viewModel.taskAreaFilter.collectAsState()
    val projectFilter by viewModel.taskProjectFilter.collectAsState()
    val searchQuery by viewModel.taskQuery.collectAsState()

    val today = JalaliCalendar.today()

    var newTitle by remember { mutableStateOf("") }
    var newArea by remember { mutableStateOf("work") }
    var newPid by remember { mutableStateOf("") }
    var newDue by remember { mutableStateOf("") }
    var newPrio by remember { mutableStateOf("mid") }
    var isNewFormExpanded by remember { mutableStateOf(false) }

    var deletingTaskId by remember { mutableStateOf<String?>(null) }

    val filteredTasks = state.task.filter { t ->
        val matchesStatus = when (statusFilter) {
            "open" -> t.st != "done"
            "done" -> t.st == "done"
            else -> true
        }
        val matchesArea = areaFilter.isBlank() || t.area == areaFilter
        val matchesProj = projectFilter.isBlank() || t.pid == projectFilter
        val matchesQuery = searchQuery.isBlank() ||
                t.t.contains(searchQuery, ignoreCase = true) ||
                t.desc.contains(searchQuery, ignoreCase = true)

        matchesStatus && matchesArea && matchesProj && matchesQuery
    }.sortedWith(compareBy({ it.st == "done" }, { it.due.ifBlank { "9" } }, {
        when (it.prio) {
            "high" -> 0
            "mid" -> 1
            else -> 2
        }
    }))

    val projectsMap = remember(state.proj) { state.proj.associate { it.id to it.name } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "تسک‌ها",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Add Task Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "افزودن تسک جدید",
                            style = MaterialTheme.typography.titleMedium,
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
                        placeholder = { Text("عنوان تسک جدید…") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    AnimatedVisibility(visible = isNewFormExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Area Selector
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("حوزه:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                                val areas = listOf("work" to "کاری", "personal" to "شخصی", "home" to "خانه", "study" to "یادگیری")
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    areas.forEach { (k, v) ->
                                        FilterChip(
                                            selected = newArea == k,
                                            onClick = { newArea = k },
                                            label = { Text(v, fontSize = 12.sp) }
                                        )
                                    }
                                }
                            }

                            // Project Selector
                            if (state.proj.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("پروژه:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                                    var expandedProjDropdown by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(onClick = { expandedProjDropdown = true }) {
                                            Text(projectsMap[newPid] ?: "بدون پروژه", fontSize = 12.sp)
                                        }
                                        DropdownMenu(
                                            expanded = expandedProjDropdown,
                                            onDismissRequest = { expandedProjDropdown = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("بدون پروژه") },
                                                onClick = { newPid = ""; expandedProjDropdown = false }
                                            )
                                            state.proj.forEach { p ->
                                                DropdownMenuItem(
                                                    text = { Text(p.name) },
                                                    onClick = { newPid = p.id; expandedProjDropdown = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Due Date & Priority
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = newDue,
                                    onValueChange = { newDue = it },
                                    placeholder = { Text("موعد مثلاً: 1405/01/01") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                var expandedPrioDropdown by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(0.7f)) {
                                    OutlinedButton(
                                        onClick = { expandedPrioDropdown = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            when (newPrio) {
                                                "high" -> "زیاد"
                                                "low" -> "کم"
                                                else -> "متوسط"
                                            },
                                            fontSize = 12.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expandedPrioDropdown,
                                        onDismissRequest = { expandedPrioDropdown = false }
                                    ) {
                                        listOf("high" to "زیاد", "mid" to "متوسط", "low" to "کم").forEach { (k, v) ->
                                            DropdownMenuItem(
                                                text = { Text(v) },
                                                onClick = { newPrio = k; expandedPrioDropdown = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                val normDue = JalaliCalendar.normalizeDate(newDue) ?: ""
                                viewModel.addTask(newTitle.trim(), newArea, newPid, normDue, newPrio)
                                newTitle = ""
                                newDue = ""
                                isNewFormExpanded = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("افزودن تسک")
                    }
                }
            }
        }

        // Status Filter Chips & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("open" to "تسک‌های باز", "done" to "انجام‌شده", "all" to "همه").forEach { (k, v) ->
                        FilterChip(
                            selected = statusFilter == k,
                            onClick = { viewModel.taskStatusFilter.value = k },
                            label = { Text(v) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.taskQuery.value = it },
                    placeholder = { Text("جستجو در تسک‌ها…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Tasks List
        if (filteredTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.task.isEmpty()) "هنوز تسکی ثبت نشده است. اولین کار را بالا بنویسید." else "تسکی با این فیلتر پیدا نشد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                TaskCardItem(
                    task = task,
                    projectsMap = projectsMap,
                    today = today,
                    onToggle = { viewModel.toggleTask(task.id) },
                    onUpdate = { viewModel.updateTask(it) },
                    onDelete = { deletingTaskId = task.id }
                )
            }
        }
    }

    deletingTaskId?.let { taskId ->
        ConfirmDeleteDialog(
            title = "حذف تسک",
            message = "آیا از حذف این تسک اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteTask(taskId)
                deletingTaskId = null
            },
            onDismiss = { deletingTaskId = null }
        )
    }
}

@Composable
fun TaskCardItem(
    task: TaskItem,
    projectsMap: Map<String, String>,
    today: String,
    onToggle: () -> Unit,
    onUpdate: (TaskItem) -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.st == "done"
    val isLate = !isDone && task.due.isNotBlank() && task.due < today
    val priorityColor = when (task.prio) {
        "high" -> DangerRed
        "low" -> SuccessGreen
        else -> WarningAmber
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(priorityColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Checkbox(
                    checked = isDone,
                    onCheckedChange = { onToggle() }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.t,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val areaName = when (task.area) {
                            "work" -> "کاری"
                            "personal" -> "شخصی"
                            "home" -> "خانه"
                            "study" -> "یادگیری"
                            else -> task.area
                        }
                        StatusBadge(text = areaName, color = MaterialTheme.colorScheme.primary)

                        val projName = projectsMap[task.pid]
                        if (!projName.isNullOrBlank()) {
                            StatusBadge(text = projName, color = MaterialTheme.colorScheme.secondary)
                        }

                        if (task.due.isNotBlank()) {
                            Text(
                                text = "موعد: ${JalaliCalendar.formatJalaliReadable(task.due)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLate) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var descText by remember(task.desc) { mutableStateOf(task.desc) }
                    OutlinedTextField(
                        value = descText,
                        onValueChange = {
                            descText = it
                            onUpdate(task.copy(desc = it))
                        },
                        label = { Text("توضیحات و یادداشت تسک") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityBadge(prio = task.prio)
                        if (task.doneAt.isNotBlank()) {
                            Text(
                                text = "انجام شد در: ${JalaliCalendar.formatJalaliReadable(task.doneAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
