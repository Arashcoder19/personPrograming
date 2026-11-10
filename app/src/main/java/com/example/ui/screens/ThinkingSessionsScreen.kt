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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThinkingSessionItem
import com.example.ui.NotebookViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinkingSessionsScreen(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val today = JalaliCalendar.today()

    var newTitle by remember { mutableStateOf("") }
    var newKind by remember { mutableStateOf("week") }
    var newDate by remember { mutableStateOf(today) }
    var newPlace by remember { mutableStateOf("") }
    var isNewSessionExpanded by remember { mutableStateOf(false) }

    var showCompletedResolutions by remember { mutableStateOf(false) }
    var deletingSessionId by remember { mutableStateOf<String?>(null) }

    val kindLabels = mapOf(
        "think" to "تفکر آزاد",
        "week" to "مرور هفتگی",
        "month" to "مرور ماهانه",
        "decide" to "تصمیم‌گیری",
        "idea" to "ایده‌پردازی"
    )

    val verdictLabels = mapOf(
        "pending" to "در انتظار تصمیم",
        "approved" to "تصویب شد",
        "rejected" to "رد شد",
        "postponed" to "به تعویق افتاد"
    )

    // All resolutions across sessions
    val allResolutions = state.sess.flatMap { s ->
        s.res.map { r -> Triple(s, r, r.due.isNotBlank() && r.due < today && r.st != "done") }
    }
    val openResolutions = allResolutions.filter { showCompletedResolutions || it.second.st != "done" }
    val overdueCount = allResolutions.count { it.third }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "جلسات تفکر",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Start Session Card
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
                            text = "شروع جلسه تفکر جدید",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { isNewSessionExpanded = !isNewSessionExpanded }) {
                            Icon(
                                if (isNewSessionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "بیشتر"
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("عنوان جلسه (مثلاً: مرور هفتگی هفتهٔ ۱۷ مهر)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Session Kind chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        kindLabels.forEach { (k, v) ->
                            FilterChip(
                                selected = newKind == k,
                                onClick = { newKind = k },
                                label = { Text(v, fontSize = 11.sp) }
                            )
                        }
                    }

                    AnimatedVisibility(visible = isNewSessionExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = newDate,
                                    onValueChange = { newDate = it },
                                    label = { Text("تاریخ") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = newPlace,
                                    onValueChange = { newPlace = it },
                                    label = { Text("مکان / حال و هوا") },
                                    placeholder = { Text("مثلاً: پارک، کافه") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val normD = JalaliCalendar.normalizeDate(newDate) ?: today
                            val title = newTitle.ifBlank { "${kindLabels[newKind]} ${JalaliCalendar.formatJalaliReadable(normD)}" }
                            viewModel.addSession(title.trim(), newKind, normD, newPlace.trim(), "")
                            newTitle = ""
                            newPlace = ""
                            isNewSessionExpanded = false
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("شروع جلسه")
                    }
                }
            }
        }

        // Followup Resolutions Card
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "پیگیری مصوبات جلسات",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusBadge(text = "${JalaliCalendar.toFa(allResolutions.count { it.second.st != "done" })} باز", color = MaterialTheme.colorScheme.primary)
                            if (overdueCount > 0) {
                                StatusBadge(text = "${JalaliCalendar.toFa(overdueCount)} عقب‌افتاده", color = DangerRed)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showCompletedResolutions,
                                onCheckedChange = { showCompletedResolutions = it }
                            )
                            Text("انجام‌شده‌ها", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (openResolutions.isEmpty()) {
                        Text(
                            text = "مصوبهٔ بازی نیست.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        openResolutions.forEach { (sess, res, isLate) ->
                            val isDone = res.st == "done"
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
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${sess.t}${if (res.due.isNotBlank()) " · مهلت: " + JalaliCalendar.formatJalaliReadable(res.due) else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLate) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (res.tid.isBlank()) {
                                        TextButton(onClick = { viewModel.convertResolutionToTask(sess.id, res.id) }) {
                                            Text("تبدیل به تسک", fontSize = 12.sp)
                                        }
                                    } else {
                                        StatusBadge(text = "در تسک‌ها هست", color = MaterialTheme.colorScheme.primary)
                                    }
                                    Checkbox(
                                        checked = isDone,
                                        onCheckedChange = {
                                            val nextSt = if (isDone) "todo" else "done"
                                            viewModel.updateResolutionStatus(sess.id, res.id, nextSt)
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }

        // Sessions List
        if (state.sess.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنوز جلسه‌ای نداشته‌اید. جلسهٔ تفکر زمانی است که با خود خلوت می‌کنید، اهداف را مرور کرده و مصوبه تعیین می‌کنید.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.sess.sortedByDescending { it.d }, key = { it.id }) { session ->
                SessionCardItem(
                    session = session,
                    kindLabels = kindLabels,
                    verdictLabels = verdictLabels,
                    viewModel = viewModel,
                    onDelete = { deletingSessionId = session.id }
                )
            }
        }
    }

    deletingSessionId?.let { sId ->
        ConfirmDeleteDialog(
            title = "حذف جلسه تفکر",
            message = "آیا از حذف این جلسه اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteSession(sId)
                deletingSessionId = null
            },
            onDismiss = { deletingSessionId = null }
        )
    }
}

@Composable
fun SessionCardItem(
    session: ThinkingSessionItem,
    kindLabels: Map<String, String>,
    verdictLabels: Map<String, String>,
    viewModel: NotebookViewModel,
    onDelete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    var newResolutionText by remember { mutableStateOf("") }
    var newResolutionDue by remember { mutableStateOf("") }

    val kindName = kindLabels[session.kind] ?: session.kind

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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(text = kindName, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = session.t,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${JalaliCalendar.formatJalaliReadable(session.d)}${if (session.place.isNotBlank()) " · " + session.place else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(
                        text = "${JalaliCalendar.toFa(session.items.size)} موضوع · ${JalaliCalendar.toFa(session.res.size)} مصوبه",
                        color = MaterialTheme.colorScheme.secondary
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
                    // Agenda & Prompts
                    var agendaText by remember(session.agenda) { mutableStateOf(session.agenda) }
                    OutlinedTextField(
                        value = agendaText,
                        onValueChange = {
                            agendaText = it
                            viewModel.updateSession(session.copy(agenda = it))
                        },
                        label = { Text("محورها و پرسش‌های جلسه") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // Topics examined
                    Text("موضوعات بررسی‌شده (اهداف، ایده‌ها، پروژه‌ها):", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    // Topic adder dropdown
                    var expandedCandidateMenu by remember { mutableStateOf(false) }
                    val candidateTopics = remember(state, session) {
                        val goals = state.goal.filter { g -> !session.items.any { it.kind == "goal" && it.ref == g.id } }.map { g -> Triple("goal", g.id, "هدف: " + g.t) }
                        val ideas = state.idea.filter { i -> !session.items.any { it.kind == "idea" && it.ref == i.id } }.map { i -> Triple("idea", i.id, "ایده: " + i.t) }
                        val projs = state.proj.filter { p -> !session.items.any { it.kind == "proj" && it.ref == p.id } }.map { p -> Triple("proj", p.id, "پروژه: " + p.name) }
                        goals + ideas + projs
                    }

                    if (candidateTopics.isNotEmpty()) {
                        Box {
                            OutlinedButton(onClick = { expandedCandidateMenu = true }) {
                                Text("＋ افزودن هدف، ایده یا پروژه به دستور جلسه…", fontSize = 12.sp)
                            }
                            DropdownMenu(
                                expanded = expandedCandidateMenu,
                                onDismissRequest = { expandedCandidateMenu = false }
                            ) {
                                candidateTopics.forEach { (kind, refId, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.addSessionTopic(session.id, kind, refId)
                                            expandedCandidateMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Existing topics list
                    session.items.forEach { topic ->
                        val entityName = when (topic.kind) {
                            "goal" -> state.goal.find { it.id == topic.ref }?.t ?: "(هدف حذف‌شده)"
                            "idea" -> state.idea.find { it.id == topic.ref }?.t ?: "(ایده حذف‌شده)"
                            else -> state.proj.find { it.id == topic.ref }?.name ?: "(پروژه حذف‌شده)"
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(
                                            text = when (topic.kind) {
                                                "goal" -> "هدف"
                                                "idea" -> "ایده"
                                                else -> "پروژه"
                                            },
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(entityName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    IconButton(onClick = { viewModel.removeSessionTopic(session.id, topic.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "برداشتن", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var expandedVerdict by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(
                                            onClick = { expandedVerdict = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(verdictLabels[topic.verdict] ?: "تصمیم", fontSize = 11.sp)
                                        }
                                        DropdownMenu(expanded = expandedVerdict, onDismissRequest = { expandedVerdict = false }) {
                                            verdictLabels.forEach { (k, v) ->
                                                DropdownMenuItem(
                                                    text = { Text(v) },
                                                    onClick = {
                                                        viewModel.updateTopicVerdict(session.id, topic.id, k, topic.vnote)
                                                        expandedVerdict = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    var vnoteText by remember(topic.vnote) { mutableStateOf(topic.vnote) }
                                    OutlinedTextField(
                                        value = vnoteText,
                                        onValueChange = {
                                            vnoteText = it
                                            viewModel.updateTopicVerdict(session.id, topic.id, topic.verdict, it)
                                        },
                                        placeholder = { Text("توضیح یا مصوبه تصمیم…", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // Reflections & Conclusions
                    var notesText by remember(session.notes) { mutableStateOf(session.notes) }
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = {
                            notesText = it
                            viewModel.updateSession(session.copy(notes = it))
                        },
                        label = { Text("تأملات، جمع‌بندی و نتیجه‌گیری جلسه") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // Resolutions Section
                    Text("مصوبات این جلسه:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newResolutionText,
                            onValueChange = { newResolutionText = it },
                            placeholder = { Text("مصوبه تازه (چه کاری، تا کی؟)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newResolutionDue,
                            onValueChange = { newResolutionDue = it },
                            placeholder = { Text("مهلت 1405/01/01", fontSize = 11.sp) },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newResolutionText.isNotBlank()) {
                                    val normDue = JalaliCalendar.normalizeDate(newResolutionDue) ?: ""
                                    viewModel.addSessionResolution(session.id, newResolutionText.trim(), normDue)
                                    newResolutionText = ""
                                    newResolutionDue = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزودن")
                        }
                    }

                    session.res.forEach { res ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(res.t, style = MaterialTheme.typography.bodySmall)
                                if (res.due.isNotBlank()) {
                                    Text("مهلت: ${JalaliCalendar.formatJalaliReadable(res.due)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (res.tid.isBlank()) {
                                    TextButton(onClick = { viewModel.convertResolutionToTask(session.id, res.id) }) {
                                        Text("تبدیل به تسک", fontSize = 11.sp)
                                    }
                                } else {
                                    StatusBadge(text = "در تسک‌ها هست", color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteSessionResolution(session.id, res.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف مصوبه", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Delete Session
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف جلسه", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف این جلسه")
                    }
                }
            }
        }
    }
}
