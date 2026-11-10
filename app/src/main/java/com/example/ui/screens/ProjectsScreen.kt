package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectItem
import com.example.ui.NotebookViewModel
import com.example.ui.components.AppProgressBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val statusFilter by viewModel.projectStatusFilter.collectAsState()
    val searchQuery by viewModel.projectQuery.collectAsState()
    val context = LocalContext.current

    var newName by remember { mutableStateOf("") }
    var newBudget by remember { mutableStateOf("") }
    var newDue by remember { mutableStateOf("") }
    var newLink by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var isNewFormExpanded by remember { mutableStateOf(false) }

    var deletingProjId by remember { mutableStateOf<String?>(null) }

    val filteredProjects = state.proj.filter { p ->
        val matchesStatus = statusFilter.isBlank() || p.status == statusFilter
        val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.desc.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesQuery
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "پروژه‌ها",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Add Project Card
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
                            text = "افزودن پروژه جدید",
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
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("نام پروژه جدید…") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    AnimatedVisibility(visible = isNewFormExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = newBudget,
                                    onValueChange = { newBudget = it },
                                    placeholder = { Text("بودجه (تومان)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = newDue,
                                    onValueChange = { newDue = it },
                                    placeholder = { Text("موعد 1405/01/01") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = newLink,
                                onValueChange = { newLink = it },
                                placeholder = { Text("لینک پروژه (اختیاری)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newDesc,
                                onValueChange = { newDesc = it },
                                placeholder = { Text("شرح و توضیحات پروژه…") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                val budgetNum = JalaliCalendar.parseFaDigits(newBudget).toDoubleOrNull() ?: 0.0
                                val normDue = JalaliCalendar.normalizeDate(newDue) ?: ""
                                viewModel.addProject(newName.trim(), budgetNum, normDue, newLink.trim(), newDesc.trim())
                                newName = ""
                                newBudget = ""
                                newDue = ""
                                newLink = ""
                                newDesc = ""
                                isNewFormExpanded = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("افزودن پروژه")
                    }
                }
            }
        }

        // Filter & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val statuses = listOf(
                    "" to "همه",
                    "plan" to "برنامه‌ریزی",
                    "doing" to "در حال انجام",
                    "hold" to "متوقف",
                    "done" to "تمام‌شده"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { (k, v) ->
                        FilterChip(
                            selected = statusFilter == k,
                            onClick = { viewModel.projectStatusFilter.value = k },
                            label = { Text(v, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.projectQuery.value = it },
                    placeholder = { Text("جستجو در نام و توضیحات پروژه‌ها…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Project Cards List
        if (filteredProjects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.proj.isEmpty()) "هنوز پروژه‌ای ثبت نشده است. اولین پروژه را بالا اضافه کنید." else "پروژه‌ای با این فیلتر پیدا نشد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredProjects, key = { it.id }) { proj ->
                ProjectCardItem(
                    proj = proj,
                    viewModel = viewModel,
                    onOpenLink = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (url.startsWith("http")) url else "https://$url"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            viewModel.showToast("خطا در باز کردن لینک")
                        }
                    },
                    onDelete = { deletingProjId = proj.id }
                )
            }
        }
    }

    deletingProjId?.let { projId ->
        ConfirmDeleteDialog(
            title = "حذف پروژه",
            message = "آیا از حذف این پروژه اطمینان دارید؟ تسک‌ها و تراکنش‌های آن بدون پروژه باقی خواهند ماند.",
            onConfirm = {
                viewModel.deleteProject(projId)
                deletingProjId = null
            },
            onDismiss = { deletingProjId = null }
        )
    }
}

@Composable
fun ProjectCardItem(
    proj: ProjectItem,
    viewModel: NotebookViewModel,
    onOpenLink: (String) -> Unit,
    onDelete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    // Project tasks
    val projectTasks = state.task.filter { it.pid == proj.id }
    val doneTasks = projectTasks.count { it.st == "done" }
    val progress = if (projectTasks.isNotEmpty()) doneTasks.toFloat() / projectTasks.size else 0f

    // Project financials
    val projectTxs = state.tx.filter { it.pid == proj.id }
    val income = projectTxs.filter { it.type == "in" }.sumOf { it.amt }
    val expense = projectTxs.filter { it.type == "out" }.sumOf { it.amt }
    val net = income - expense

    // New task in project
    var quickTaskTitle by remember { mutableStateOf("") }
    // New tx in project
    var quickTxTitle by remember { mutableStateOf("") }
    var quickTxAmt by remember { mutableStateOf("") }
    var quickTxType by remember { mutableStateOf("out") }

    val statusLabel = when (proj.status) {
        "doing" -> "در حال انجام"
        "hold" -> "متوقف"
        "done" -> "تمام‌شده"
        else -> "برنامه‌ریزی"
    }

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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        StatusBadge(
                            text = statusLabel,
                            color = when (proj.status) {
                                "done" -> SuccessGreen
                                "doing" -> MaterialTheme.colorScheme.primary
                                "hold" -> DangerRed
                                else -> WarningAmber
                            }
                        )
                        Text(
                            text = proj.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${JalaliCalendar.toFa((progress * 100).toInt())}٪",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                AppProgressBar(progress = progress)

                if (proj.due.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "موعد: ${JalaliCalendar.formatJalaliReadable(proj.due)}",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Financial Overview Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("تسک‌ها", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${JalaliCalendar.toFa(doneTasks)} از ${JalaliCalendar.toFa(projectTasks.size)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("درآمد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(JalaliCalendar.formatToman(income), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen))
                                }
                                Column {
                                    Text("هزینه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(JalaliCalendar.formatToman(expense), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = DangerRed))
                                }
                                Column {
                                    Text(if (net >= 0) "سود" else "زیان", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(JalaliCalendar.formatToman(Math.abs(net)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (net >= 0) SuccessGreen else DangerRed))
                                }
                            }
                            if (proj.budget > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val consumedPct = (expense / proj.budget * 100).toInt()
                                Text(
                                    text = "مصرف بودجه: ${JalaliCalendar.toFa(consumedPct)}٪ از ${JalaliCalendar.formatToman(proj.budget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (consumedPct > 100) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (proj.link.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onOpenLink(proj.link) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "لینک", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("باز کردن لینک پروژه", fontSize = 12.sp)
                        }
                    }

                    if (proj.desc.isNotBlank()) {
                        Text(
                            text = proj.desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Project Tasks Section
                    Text("تسک‌های این پروژه:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickTaskTitle,
                            onValueChange = { quickTaskTitle = it },
                            placeholder = { Text("تسک جدید برای پروژه…", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (quickTaskTitle.isNotBlank()) {
                                    viewModel.addTask(quickTaskTitle.trim(), "work", proj.id, "", "mid")
                                    quickTaskTitle = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزودن")
                        }
                    }

                    projectTasks.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleTask(t.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = t.st == "done", onCheckedChange = { viewModel.toggleTask(t.id) })
                            Text(
                                text = t.t,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Project Quick Finance Entry
                    Text("ثبت سریع مالی برای پروژه:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            FilterChip(
                                selected = quickTxType == "out",
                                onClick = { quickTxType = "out" },
                                label = { Text("هزینه", fontSize = 11.sp) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            FilterChip(
                                selected = quickTxType == "in",
                                onClick = { quickTxType = "in" },
                                label = { Text("درآمد", fontSize = 11.sp) }
                            )
                        }
                        OutlinedTextField(
                            value = quickTxTitle,
                            onValueChange = { quickTxTitle = it },
                            placeholder = { Text("شرح", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = quickTxAmt,
                            onValueChange = { quickTxAmt = it },
                            placeholder = { Text("مبلغ", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val amtNum = JalaliCalendar.parseFaDigits(quickTxAmt).toDoubleOrNull() ?: 0.0
                                if (quickTxTitle.isNotBlank() && amtNum > 0) {
                                    viewModel.addTransaction(
                                        type = quickTxType,
                                        title = quickTxTitle.trim(),
                                        cat = "پروژه",
                                        amt = amtNum,
                                        date = JalaliCalendar.today(),
                                        pid = proj.id
                                    )
                                    quickTxTitle = ""
                                    quickTxAmt = ""
                                }
                            }
                        ) {
                            Text("ثبت", fontSize = 12.sp)
                        }
                    }

                    // Delete Project
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف پروژه", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف این پروژه")
                    }
                }
            }
        }
    }
}
