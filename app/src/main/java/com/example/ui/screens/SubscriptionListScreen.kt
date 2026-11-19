package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Currencies
import com.example.data.SubscriptionItem
import com.example.ui.SubscriptionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionListScreen(
    viewModel: SubscriptionViewModel
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedSubscriptionForEdit by remember { mutableStateOf<SubscriptionItem?>(null) }
    var isAddSheetOpen by remember { mutableStateOf(false) }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled for renewal alarms", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    checkAndRequestNotificationPermission()
                    selectedSubscriptionForEdit = null
                    isAddSheetOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Summary spend card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "ESTIMATED MONTHLY SPEND",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (summary.monthlyByCurrency.isEmpty()) {
                            Text(
                                text = Currencies.format("PKR", 0.0),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            summary.monthlyByCurrency.entries.sortedBy { it.key }.forEach { (code, amount) ->
                                Text(
                                    text = Currencies.format(code, amount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${summary.activeCount}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Active Subs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${summary.upcomingCount}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Due this week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (subscriptions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EventRepeat,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No subscriptions yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Add subscriptions to track renewals and get phone alarms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "UPCOMING RENEWALS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${subscriptions.size} Total",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(subscriptions, key = { it.id }) { item ->
                    SubscriptionCard(
                        item = item,
                        onClick = {
                            checkAndRequestNotificationPermission()
                            selectedSubscriptionForEdit = item
                            isAddSheetOpen = true
                        },
                        onTogglePin = { viewModel.togglePin(item) },
                        onToggleAlarm = {
                            checkAndRequestNotificationPermission()
                            viewModel.toggleAlarm(item)
                        },
                        onTestAlarm = {
                            checkAndRequestNotificationPermission()
                            viewModel.triggerTestAlarm(item)
                            Toast.makeText(context, "Alarm notification triggered for ${item.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (isAddSheetOpen) {
        SubscriptionEditDialog(
            existing = selectedSubscriptionForEdit,
            onDismiss = { isAddSheetOpen = false },
            onSave = { item ->
                viewModel.addOrUpdateSubscription(item)
                isAddSheetOpen = false
            },
            onDelete = { item ->
                viewModel.deleteSubscription(item)
                isAddSheetOpen = false
            },
            onTestAlarm = { item ->
                checkAndRequestNotificationPermission()
                viewModel.triggerTestAlarm(item)
                Toast.makeText(context, "Test alarm triggered for ${item.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SubscriptionCard(
    item: SubscriptionItem,
    onClick: () -> Unit,
    onTogglePin: () -> Unit = {},
    onToggleAlarm: () -> Unit,
    onTestAlarm: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(item.nextRenewalTimestamp))

    val daysDiff = ((item.nextRenewalTimestamp - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val countdownText = when {
        daysDiff < 0 -> "Overdue"
        daysDiff == 0 -> "Renews Today!"
        daysDiff == 1 -> "Renews Tomorrow"
        else -> "In $daysDiff days"
    }
    val countdownColor = when {
        daysDiff <= 1 -> MaterialTheme.colorScheme.error
        daysDiff <= 5 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = item.name.firstOrNull()?.toString()?.uppercase() ?: "S"
                    Text(
                        text = initial,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedDate • $countdownText",
                        style = MaterialTheme.typography.bodySmall,
                        color = countdownColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Currencies.format(item.currency, item.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.billingCycle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options", modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (item.isPinned) "Unpin" else "Pin") },
                                onClick = { showMenu = false; onTogglePin() },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isAlarmActive) Icons.Default.AlarmOn else Icons.Default.AlarmOff,
                        contentDescription = "Alarm status",
                        tint = if (item.isAlarmActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.isAlarmActive) "Alarm set (${item.reminderDaysBefore}d prior at ${String.format(Locale.US, "%02d:%02d", item.reminderHour, item.reminderMinute)})" else "Alarms paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Row {
                    IconButton(
                        onClick = onTestAlarm,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Test alarm",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleAlarm,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isAlarmActive) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = "Toggle alarm",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionEditDialog(
    existing: SubscriptionItem?,
    onDismiss: () -> Unit,
    onSave: (SubscriptionItem) -> Unit,
    onDelete: (SubscriptionItem) -> Unit,
    onTestAlarm: (SubscriptionItem) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var priceStr by remember { mutableStateOf(if (existing != null) String.format(Locale.US, "%.2f", existing.price) else "") }
    var currency by remember { mutableStateOf(Currencies.normalizeCurrency(existing?.currency)) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(existing?.category ?: "Entertainment") }
    var billingCycle by remember { mutableStateOf(existing?.billingCycle ?: "Monthly") }
    var nextRenewalMs by remember {
        mutableLongStateOf(
            existing?.nextRenewalTimestamp ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000L)
        )
    }
    var reminderDaysBefore by remember { mutableIntStateOf(existing?.reminderDaysBefore ?: 1) }
    var reminderHour by remember { mutableIntStateOf(existing?.reminderHour ?: 9) }
    var reminderMinute by remember { mutableIntStateOf(existing?.reminderMinute ?: 0) }
    var isAlarmActive by remember { mutableStateOf(existing?.isAlarmActive ?: true) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    val categories = listOf("Entertainment", "Streaming", "Productivity", "Cloud Storage", "Music", "Utilities", "Other")
    val cycles = listOf("Monthly", "Yearly", "Weekly", "Quarterly")
    val reminderDayOptions = listOf(0, 1, 2, 3, 7)

    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    val dialogScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "New Subscription" else "Edit Subscription",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(dialogScrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name (e.g. Netflix, Spotify)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    // Currency picker (was a free-text 80dp field)
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = "${Currencies.forCode(currency).symbol} ${Currencies.forCode(currency).code}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = {
                                IconButton(onClick = { currencyMenuExpanded = !currencyMenuExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose currency")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currencyMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            Currencies.ALL.forEach { info ->
                                DropdownMenuItem(
                                    text = { Text("${info.code} (${info.symbol}) — ${info.country}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = { currency = info.code; currencyMenuExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Category selector (all 7, wrapping — Music/Utilities/Other were unreachable before)
                Column {
                    Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Billing cycle selector
                Column {
                    Text("Billing Cycle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cycles.forEach { cycle ->
                            FilterChip(
                                selected = billingCycle == cycle,
                                onClick = { billingCycle = cycle },
                                label = { Text(cycle, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Renewal date button
                Column {
                    Text("Next Renewal Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = nextRenewalMs }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        cal.set(year, month, dayOfMonth)
                                        nextRenewalMs = cal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateFormat.format(Date(nextRenewalMs)), fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Phone Alarm and Reminders
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Phone Alarm & Reminder", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Direct alarm notification", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isAlarmActive,
                                onCheckedChange = { isAlarmActive = it }
                            )
                        }

                        if (isAlarmActive) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Notify before renewal:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                reminderDayOptions.forEach { d ->
                                    FilterChip(
                                        selected = reminderDaysBefore == d,
                                        onClick = { reminderDaysBefore = d },
                                        label = { Text(if (d == 0) "Same day" else "${d}d prior", fontSize = 10.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            // Time picker button
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            reminderHour = hourOfDay
                                            reminderMinute = minute
                                        },
                                        reminderHour,
                                        reminderMinute,
                                        false
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Alarm Time: ${String.format(Locale.US, "%02d:%02d", reminderHour, reminderMinute)}")
                            }
                        }
                    }
                }

                // Notes (declared & saved before, but had no field in the dialog)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Immediate test alarm button
                OutlinedButton(
                    onClick = {
                        val tempItem = SubscriptionItem(
                            id = existing?.id ?: 9999,
                            name = name.ifBlank { "Sample Subscription" },
                            category = category,
                            price = priceStr.toDoubleOrNull() ?: 9.99,
                            currency = currency,
                            billingCycle = billingCycle,
                            nextRenewalTimestamp = nextRenewalMs,
                            reminderDaysBefore = reminderDaysBefore,
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute,
                            isAlarmActive = true,
                            isPinned = existing?.isPinned ?: false,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                        onTestAlarm(tempItem)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Test Alarm & Notification Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val item = SubscriptionItem(
                        id = existing?.id ?: 0,
                        name = name.ifBlank { "Untitled Subscription" },
                        category = category,
                        price = price,
                        currency = currency,
                        billingCycle = billingCycle,
                        nextRenewalTimestamp = nextRenewalMs,
                        reminderDaysBefore = reminderDaysBefore,
                        reminderHour = reminderHour,
                        reminderMinute = reminderMinute,
                        notes = notes,
                        isAlarmActive = isAlarmActive,
                        isPinned = existing?.isPinned ?: false,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(item)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing != null) {
                    TextButton(
                        onClick = { onDelete(existing) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
