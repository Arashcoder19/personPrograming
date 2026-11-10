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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BuyItem
import com.example.ui.NotebookViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyScreen(viewModel: NotebookViewModel) {
    val state by viewModel.state.collectAsState()
    val statusFilter by viewModel.buyStatusFilter.collectAsState()
    val context = LocalContext.current

    val openItems = state.buy.filter { it.st == "want" || it.st == "plan" }
    val totalWishlistAmount = openItems.sumOf { it.price }
    val needPendingAmount = openItems.filter { it.kind == "need" }.sumOf { it.price }

    val currentMonthStr = JalaliCalendar.ymString(JalaliCalendar.currentYearMonth())
    val boughtThisMonth = state.buy.filter { it.st == "bought" && it.d.startsWith(currentMonthStr) }
    val boughtThisMonthAmount = boughtThisMonth.sumOf { it.price }

    val filteredItems = state.buy.filter { b ->
        when (statusFilter) {
            "open" -> b.st == "want" || b.st == "plan"
            "bought" -> b.st == "bought"
            "drop" -> b.st == "drop"
            else -> true
        }
    }.sortedBy {
        when (it.prio) {
            "high" -> 0
            "mid" -> 1
            else -> 2
        }
    }

    var newTitle by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newKind by remember { mutableStateOf("want") }
    var newPrio by remember { mutableStateOf("mid") }
    var newLink by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    var isNewFormExpanded by remember { mutableStateOf(false) }

    var deletingBuyId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "خریدها",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Summary Metric Tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "کل فهرست خرید",
                        value = JalaliCalendar.formatToman(totalWishlistAmount),
                        subValue = "${JalaliCalendar.toFa(openItems.size)} مورد در انتظار",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "ضروری‌های باقی‌مانده",
                        value = JalaliCalendar.formatToman(needPendingAmount),
                        subValue = "اولویت فوری",
                        isPositive = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricTile(
                    label = "خریدهای این ماه",
                    value = JalaliCalendar.formatToman(boughtThisMonthAmount),
                    subValue = "${JalaliCalendar.toFa(boughtThisMonth.size)} مورد خریده شده",
                    isPositive = true
                )
            }
        }

        // Add to Wishlist Card
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
                            text = "افزودن مورد به فهرست خرید",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { isNewFormExpanded = !isNewFormExpanded }) {
                            Icon(
                                if (isNewFormExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "بیشتر"
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("چه چیزی می‌خواهید بخرید؟") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newPrice,
                            onValueChange = { newPrice = it },
                            placeholder = { Text("قیمت تقریبی (تومان)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        var expandedKind by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(0.7f)) {
                            OutlinedButton(
                                onClick = { expandedKind = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    when (newKind) {
                                        "need" -> "ضروری"
                                        "daily" -> "روزمره"
                                        else -> "دلخواه"
                                    },
                                    fontSize = 12.sp
                                )
                            }
                            DropdownMenu(
                                expanded = expandedKind,
                                onDismissRequest = { expandedKind = false }
                            ) {
                                listOf("need" to "ضروری", "want" to "دلخواه", "daily" to "روزمره").forEach { (k, v) ->
                                    DropdownMenuItem(
                                        text = { Text(v) },
                                        onClick = { newKind = k; expandedKind = false }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = isNewFormExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("اولویت:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                                val prios = listOf("high" to "زیاد", "mid" to "متوسط", "low" to "کم")
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    prios.forEach { (k, v) ->
                                        FilterChip(
                                            selected = newPrio == k,
                                            onClick = { newPrio = k },
                                            label = { Text(v, fontSize = 12.sp) }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = newLink,
                                onValueChange = { newLink = it },
                                placeholder = { Text("لینک محصول در فروشگاه (اختیاری)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newNote,
                                onValueChange = { newNote = it },
                                placeholder = { Text("یادداشت و جزئیات…") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val priceNum = JalaliCalendar.parseFaDigits(newPrice).toDoubleOrNull() ?: 0.0
                            if (newTitle.isNotBlank()) {
                                viewModel.addBuyItem(
                                    title = newTitle.trim(),
                                    price = priceNum,
                                    kind = newKind,
                                    prio = newPrio,
                                    link = newLink.trim(),
                                    note = newNote.trim()
                                )
                                newTitle = ""
                                newPrice = ""
                                newLink = ""
                                newNote = ""
                                isNewFormExpanded = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("افزودن به فهرست")
                    }
                }
            }
        }

        // Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "open" to "در انتظار خرید",
                    "bought" to "خریده‌شده",
                    "drop" to "منصرف شدم",
                    "all" to "همه"
                ).forEach { (k, v) ->
                    FilterChip(
                        selected = statusFilter == k,
                        onClick = { viewModel.buyStatusFilter.value = k },
                        label = { Text(v, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Shopping List Items
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.buy.isEmpty()) "فهرست خرید خالی است. مواردی که قصد خریدشان را دارید بالا اضافه کنید." else "موردی با این فیلتر پیدا نشد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                BuyCardItem(
                    item = item,
                    onBought = { viewModel.markBuyItemBought(item.id) },
                    onUpdate = { viewModel.updateBuyItem(it) },
                    onDelete = { deletingBuyId = item.id },
                    onOpenLink = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (url.startsWith("http")) url else "https://$url"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            viewModel.showToast("خطا در باز کردن لینک")
                        }
                    }
                )
            }
        }
    }

    deletingBuyId?.let { buyId ->
        ConfirmDeleteDialog(
            title = "حذف مورد",
            message = "آیا از حذف این مورد از فهرست خرید اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteBuyItem(buyId)
                deletingBuyId = null
            },
            onDismiss = { deletingBuyId = null }
        )
    }
}

@Composable
fun BuyCardItem(
    item: BuyItem,
    onBought: () -> Unit,
    onUpdate: (BuyItem) -> Unit,
    onDelete: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val isBought = item.st == "bought"
    val isDropped = item.st == "drop"
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.t,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (isBought || isDropped) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isBought || isDropped) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.price > 0) {
                            Text(
                                text = JalaliCalendar.formatToman(item.price),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val kindLabel = when (item.kind) {
                            "need" -> "ضروری"
                            "daily" -> "روزمره"
                            else -> "دلخواه"
                        }
                        StatusBadge(
                            text = kindLabel,
                            color = if (item.kind == "need") DangerRed else MaterialTheme.colorScheme.primary
                        )
                        PriorityBadge(prio = item.prio)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isBought && !isDropped) {
                        Button(
                            onClick = onBought,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "خریدم", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("خریدم", fontSize = 12.sp)
                        }
                    } else if (isBought) {
                        StatusBadge(text = "خریده شد", color = SuccessGreen)
                    } else {
                        StatusBadge(text = "منصرف شدم", color = MaterialTheme.colorScheme.outline)
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.link.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onOpenLink(item.link) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "لینک", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاهده در فروشگاه", fontSize = 12.sp)
                        }
                    }

                    if (item.note.isNotBlank()) {
                        Text(
                            text = "یادداشت: ${item.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (item.d.isNotBlank()) {
                        Text(
                            text = "تاریخ: ${JalaliCalendar.formatJalaliReadable(item.d)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
