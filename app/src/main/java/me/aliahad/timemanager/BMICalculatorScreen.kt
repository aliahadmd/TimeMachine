package me.aliahad.timemanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.BMICalculation
import me.aliahad.timemanager.data.BMIClassification
import me.aliahad.timemanager.data.Gender
import me.aliahad.timemanager.data.WHOBMICategory
import me.aliahad.timemanager.ui.components.ConfirmDeleteDialog
import me.aliahad.timemanager.ui.components.EmptyStateView
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TabularNumberStyle
import me.aliahad.timemanager.ui.viewmodel.BMICalculatorViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BMICalculatorScreen(onNavigateBack: () -> Unit) {
    val viewModel = appViewModel { container -> BMICalculatorViewModel(container) }
    val records by viewModel.savedRecords.collectAsStateWithLifecycle()

    var useImperial by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var classification by remember { mutableStateOf(BMIClassification.WHO) }
    var ageText by remember { mutableStateOf("28") }
    var heightCm by remember { mutableFloatStateOf(172f) }
    var weightKg by remember { mutableFloatStateOf(70f) }

    var editingRecord by remember { mutableStateOf<BMICalculation?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BMICalculation?>(null) }

    val age = ageText.toIntOrNull()
    val validation = validateBMIInputs(age, heightCm, weightKg)
    val bmi = if (validation.isValid) calculateBMI(heightCm, weightKg) else 0f
    val safeAge = age ?: 28
    val category = getBMICategory(bmi, safeAge, gender, classification)
    val catColor = Color(getBMICategoryColor(bmi, safeAge, gender, classification))
    val idealRange = getIdealWeightRange(heightCm, gender, safeAge, classification)
    val guidance = getTargetWeightGuidance(weightKg, (idealRange.first + idealRange.second) / 2f)

    Scaffold(
        topBar = {
            TimeMachineTopBar(
                title = "BMI Calculator",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "bmi_inputs") {
                SectionCard {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !useImperial,
                            onClick = { useImperial = false },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Metric (cm / kg)") }
                        SegmentedButton(
                            selected = useImperial,
                            onClick = { useImperial = true },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Imperial (ft·in / lbs)") }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Gender.entries.forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g.displayName) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BMIClassification.entries.forEach { cls ->
                            FilterChip(
                                selected = classification == cls,
                                onClick = { classification = cls },
                                label = { Text(cls.name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Age (2–120)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    if (!useImperial) {
                        Text(
                            "Height: ${heightCm.toInt()} cm",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = heightCm,
                            onValueChange = { heightCm = it },
                            valueRange = 50f..250f
                        )
                        Text(
                            "Weight: ${Formatters.formatOneDecimal(weightKg)} kg",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = weightKg,
                            onValueChange = { weightKg = it },
                            valueRange = 20f..250f
                        )
                    } else {
                        val (feet, inches) = cmToFeetAndInches(heightCm)
                        val totalInches = (feet * 12 + inches).toFloat().coerceIn(24f, 96f)
                        Text(
                            "Height: $feet ft $inches in (${heightCm.toInt()} cm)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = totalInches,
                            onValueChange = { nextIn ->
                                val rounded = nextIn.toInt()
                                heightCm = feetAndInchesToCm(rounded / 12, rounded % 12)
                            },
                            valueRange = 24f..96f
                        )
                        val lbs = kgToLbs(weightKg)
                        Text(
                            "Weight: ${Formatters.formatOneDecimal(lbs)} lbs",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = lbs.coerceIn(44f, 550f),
                            onValueChange = { weightKg = lbsToKg(it) },
                            valueRange = 44f..550f
                        )
                    }

                    if (!validation.isValid && validation.errorMessage != null) {
                        Text(
                            text = validation.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item(key = "bmi_result") {
                SectionCard {
                    ResponsiveBMIGauge(bmi = bmi, accent = catColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleLarge,
                        color = catColor,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = getBMIInterpretation(bmi, category, classification),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val rangeLabel = if (!useImperial) {
                        "${Formatters.formatOneDecimal(idealRange.first)} – ${Formatters.formatOneDecimal(idealRange.second)} kg"
                    } else {
                        "${Formatters.formatOneDecimal(kgToLbs(idealRange.first))} – ${Formatters.formatOneDecimal(kgToLbs(idealRange.second))} lbs"
                    }
                    Text(
                        text = "Ideal weight range: $rangeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = guidance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        enabled = validation.isValid,
                        onClick = {
                            editingRecord = null
                            showSaveDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Save BMI Record")
                    }
                }
            }

            if (classification == BMIClassification.WHO) {
                item(key = "who_legend") {
                    SectionCard {
                        Text("WHO 8-Band Reference", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            WHOBMICategory.entries.forEach { band ->
                                val maxStr = if (band.maxBMI > 100f) "+" else "–${band.maxBMI}"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(band.displayName, style = MaterialTheme.typography.bodySmall, color = Color(band.color))
                                    Text(
                                        "${band.minBMI}$maxStr",
                                        style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bmi_history_hdr") {
                Text("Saved History (${records.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (records.size >= 2) {
                item(key = "bmi_trend_summary") {
                    val latest = records.first()
                    val previous = records[1]
                    val bmiDelta = latest.bmiValue - previous.bmiValue
                    val weightDelta = latest.weightKg - previous.weightKg
                    val bmiSign = if (bmiDelta >= 0f) "+" else ""
                    val wSign = if (weightDelta >= 0f) "+" else ""
                    SectionCard {
                        Text("Recent Trend", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "BMI ${bmiSign}${Formatters.formatOneDecimal(bmiDelta)} • Weight ${wSign}${Formatters.formatOneDecimal(weightDelta)} kg vs prior entry",
                            style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (records.isEmpty()) {
                item(key = "empty_bmi") {
                    EmptyStateView(
                        emoji = "⚖️",
                        title = "No Saved BMI Entries",
                        subtitle = "Save measurements above to track BMI changes over time."
                    )
                }
            } else {
                items(records, key = { it.id }) { item ->
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "BMI ${formatBMI(item.bmiValue)} • ${item.category} (${item.classification.name})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${item.heightCm.toInt()} cm • ${Formatters.formatOneDecimal(item.weightKg)} kg • Age ${item.age}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                editingRecord = item
                                ageText = item.age.toString()
                                heightCm = item.heightCm
                                weightKg = item.weightKg
                                gender = item.gender
                                classification = item.classification
                                showSaveDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { pendingDelete = item }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveBMIDialog(
            existing = editingRecord,
            onDismiss = { showSaveDialog = false },
            onConfirm = { label ->
                val now = System.currentTimeMillis()
                viewModel.saveRecord(
                    BMICalculation(
                        id = editingRecord?.id ?: 0L,
                        name = label,
                        age = safeAge,
                        heightCm = heightCm,
                        weightKg = weightKg,
                        gender = gender,
                        classification = classification,
                        bmiValue = bmi,
                        category = category,
                        createdAt = editingRecord?.createdAt ?: now,
                        updatedAt = now
                    ),
                    isEdit = editingRecord != null
                )
                showSaveDialog = false
            }
        )
    }

    pendingDelete?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete BMI Record?",
            message = "Delete \"${item.name}\" (BMI ${formatBMI(item.bmiValue)})?",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                viewModel.deleteRecord(item)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun ResponsiveBMIGauge(bmi: Float, accent: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val diameter = (maxWidth * 0.65f).coerceIn(160.dp, 220.dp)
        Box(
            modifier = Modifier
                .size(width = diameter, height = diameter * 0.62f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 14.dp.toPx()
                val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, (size.width - strokeW))
                val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                drawArc(
                    color = track,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                val normalized = ((bmi - 12f) / 30f).coerceIn(0f, 1f)
                drawArc(
                    color = accent,
                    startAngle = 180f,
                    sweepAngle = normalized * 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                val angleRad = Math.toRadians((180f + normalized * 180f).toDouble())
                val radius = arcSize.width / 2f
                val cx = topLeft.x + radius
                val cy = topLeft.y + radius
                val tipX = cx + (radius * cos(angleRad)).toFloat()
                val tipY = cy + (radius * sin(angleRad)).toFloat()
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(tipX, tipY))
                drawCircle(color = accent, radius = 4.dp.toPx(), center = Offset(tipX, tipY))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatBMI(bmi),
                    style = MaterialTheme.typography.headlineLarge.merge(TabularNumberStyle),
                    fontWeight = FontWeight.Bold
                )
                Text("kg/m²", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SaveBMIDialog(
    existing: BMICalculation?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "My BMI") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Save BMI Record" else "Edit BMI Record") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile / Note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
