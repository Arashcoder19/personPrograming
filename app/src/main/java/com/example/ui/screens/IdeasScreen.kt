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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IdeaItem
import com.example.ui.NotebookViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasScreen(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val statusFilter by viewModel.ideaStatusFilter.collectAsState()
    val typeFilter by viewModel.ideaTypeFilter.collectAsState()
    val searchQuery by viewModel.ideaQuery.collectAsState()

    val countNew = state.idea.count { it.st == "new" }
    val countReview = state.idea.count { it.st == "review" }
    val countApproved = state.idea.count { it.st == "approved" }
    val countDoing = state.idea.count { it.st == "doing" }
    val countDone = state.idea.count { it.st == "done" }

    val filteredIdeas = state.idea.filter { e ->
        val matchesStatus = statusFilter.isBlank() || e.st == statusFilter
        val matchesType = typeFilter.isBlank() || e.type == typeFilter
        val matchesQuery = searchQuery.isBlank() ||
                e.t.contains(searchQuery, ignoreCase = true) ||
                e.desc.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesType && matchesQuery
    }

    var newTitle by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("idea") }
    var newPrio by remember { mutableStateOf("mid") }
    var newDesc by remember { mutableStateOf("") }
    var isNewFormExpanded by remember { mutableStateOf(false) }

    var deletingIdeaId by remember { mutableStateOf<String?>(null) }

    val statusLabels = mapOf(
        "new" to "جدید",
        "review" to "در حال بررسی",
        "approved" to "تصویب‌شده",
        "doing" to "در حال اجرا",
        "done" to "انجام‌شده",
        "hold" to "متوقف",
        "rejected" to "ردشده"
    )

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
                    text = "ایده‌ها و طرح‌ها",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${JalaliCalendar.toFa(state.idea.size)} مورد · جدید: ${JalaliCalendar.toFa(countNew)} · بررسی: ${JalaliCalendar.toFa(countReview)} · تصویب: ${JalaliCalendar.toFa(countApproved)} · اجرا: ${JalaliCalendar.toFa(countDoing)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Add Idea Card
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
                            text = "ثبت ایده یا طرح تازه",
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
                        placeholder = { Text("ایده یا طرح تازه…") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row {
                        FilterChip(
                            selected = newType == "idea",
                            onClick = { newType = "idea" },
                            label = { Text("ایده") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = newType == "plan",
                            onClick = { newType = "plan" },
                            label = { Text("طرح") }
                        )
                    }

                    AnimatedVisibility(visible = isNewFormExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("اولویت:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                                listOf("high" to "زیاد", "mid" to "متوسط", "low" to "کم").forEach { (k, v) ->
                                    FilterChip(
                                        selected = newPrio == k,
                                        onClick = { newPrio = k },
                                        label = { Text(v, fontSize = 12.sp) }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                            OutlinedTextField(
                                value = newDesc,
                                onValueChange = { newDesc = it },
                                placeholder = { Text("توضیحات و جزئیات بیشتر…") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                viewModel.addIdea(
                                    title = newTitle.trim(),
                                    type = newType,
                                    prio = newPrio,
                                    pid = "",
                                    desc = newDesc.trim()
                                )
                                newTitle = ""
                                newDesc = ""
                                isNewFormExpanded = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("ثبت ایده")
                    }
                }
            }
        }

        // Filters & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("" to "همه وضعیت‌ها", "new" to "جدید", "review" to "بررسی", "approved" to "تصویب‌شده", "doing" to "در حال اجرا").forEach { (k, v) ->
                        FilterChip(
                            selected = statusFilter == k,
                            onClick = { viewModel.ideaStatusFilter.value = k },
                            label = { Text(v, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.ideaQuery.value = it },
                    placeholder = { Text("جستجو در ایده‌ها و طرح‌ها…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Ideas List
        if (filteredIdeas.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.idea.isEmpty()) "ایده‌هایتان را بنویسید تا فراموش نشوند؛ بعد در جلسهٔ تفکر تصمیم بگیرید." else "ایده‌ای با این فیلتر پیدا نشد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredIdeas, key = { it.id }) { idea ->
                IdeaCardItem(
                    idea = idea,
                    statusLabels = statusLabels,
                    onAddLog = { text, date -> viewModel.addIdeaLog(idea.id, text, date) },
                    onDeleteLog = { logId -> viewModel.deleteIdeaLog(idea.id, logId) },
                    onUpdateStatus = { st -> viewModel.updateIdea(idea.copy(st = st)) },
                    onDelete = { deletingIdeaId = idea.id }
                )
            }
        }
    }

    deletingIdeaId?.let { ideaId ->
        ConfirmDeleteDialog(
            title = "حذف ایده",
            message = "آیا از حذف این ایده اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteIdea(ideaId)
                deletingIdeaId = null
            },
            onDismiss = { deletingIdeaId = null }
        )
    }
}

@Composable
fun IdeaCardItem(
    idea: IdeaItem,
    statusLabels: Map<String, String>,
    onAddLog: (String, String) -> Unit,
    onDeleteLog: (String) -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var newLogText by remember { mutableStateOf("") }

    val stName = statusLabels[idea.st] ?: idea.st
    val isPlan = idea.type == "plan"

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
                        StatusBadge(
                            text = if (isPlan) "طرح" else "ایده",
                            color = if (isPlan) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = idea.t,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    StatusBadge(
                        text = stName,
                        color = when (idea.st) {
                            "approved", "done" -> SuccessGreen
                            "doing" -> MaterialTheme.colorScheme.primary
                            "rejected" -> DangerRed
                            else -> WarningAmber
                        }
                    )
                }

                if (idea.desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = idea.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2
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
                    // Status selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تغییر وضعیت:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
                        var expandedStatus by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedStatus = true }) {
                                Text(stName, fontSize = 12.sp)
                            }
                            DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                statusLabels.forEach { (k, v) ->
                                    DropdownMenuItem(
                                        text = { Text(v) },
                                        onClick = { onUpdateStatus(k); expandedStatus = false }
                                    )
                                }
                            }
                        }
                    }

                    // Action Log (پیگیری) Section
                    Text("پیگیری و اقدامات انجام‌شده:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newLogText,
                            onValueChange = { newLogText = it },
                            placeholder = { Text("اقدام یا نکته تازه…", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newLogText.isNotBlank()) {
                                    onAddLog(newLogText.trim(), JalaliCalendar.today())
                                    newLogText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "ثبت")
                        }
                    }

                    idea.log.reversed().forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.txt, style = MaterialTheme.typography.bodySmall)
                                Text(JalaliCalendar.formatJalaliReadable(log.d), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDeleteLog(log.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف پیگیری", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Delete Idea
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف ایده", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف این ایده")
                    }
                }
            }
        }
    }
}
