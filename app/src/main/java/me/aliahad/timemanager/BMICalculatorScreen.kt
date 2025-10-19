package me.aliahad.timemanager

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.*
import kotlin.math.roundToInt

enum class HeightUnit { CM, FT_IN }
enum class WeightUnit { KG, LBS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BMICalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val bmiDao = database.bmiCalculationDao()
    val scope = rememberCoroutineScope()
    
    val savedCalculations by bmiDao.getAllCalculations().collectAsState(initial = emptyList())
    
    var showCalculator by remember { mutableStateOf(true) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // Input states
    var age by remember { mutableStateOf("") }
    var heightCm by remember { mutableFloatStateOf(170f) }
    var weightKg by remember { mutableFloatStateOf(70f) }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var classification by remember { mutableStateOf(BMIClassification.WHO) }
    
    // Unit preferences
    var heightUnit by remember { mutableStateOf(HeightUnit.CM) }
    var weightUnit by remember { mutableStateOf(WeightUnit.KG) }
    
    // Calculated values
    val bmiValue = remember(heightCm, weightKg) { calculateBMI(heightCm, weightKg) }
    val bmiCategory = remember(bmiValue, age, gender, classification) {
        age.toIntOrNull()?.let { ageInt ->
            getBMICategory(bmiValue, ageInt, gender, classification)
        } ?: "Enter age"
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💪 BMI Calculator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCalculator = !showCalculator }) {
                        Icon(
                            if (showCalculator) Icons.Default.History else Icons.Default.Calculate,
                            if (showCalculator) "View history" else "Calculate"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (showCalculator && age.toIntOrNull() != null) {
                FloatingActionButton(
                    onClick = { showSaveDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Save, "Save")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = showCalculator,
                transitionSpec = {
                    slideInHorizontally { if (targetState) it else -it } + fadeIn() togetherWith
                            slideOutHorizontally { if (targetState) -it else it } + fadeOut()
                }
            ) { isCalculatorView ->
                if (isCalculatorView) {
                    BMICalculatorView(
                        age = age,
                        heightCm = heightCm,
                        weightKg = weightKg,
                        gender = gender,
                        classification = classification,
                        heightUnit = heightUnit,
                        weightUnit = weightUnit,
                        bmiValue = bmiValue,
                        bmiCategory = bmiCategory,
                        onAgeChange = { age = it },
                        onHeightChange = { heightCm = it },
                        onWeightChange = { weightKg = it },
                        onGenderChange = { gender = it },
                        onClassificationChange = { classification = it },
                        onHeightUnitChange = { heightUnit = it },
                        onWeightUnitChange = { weightUnit = it }
                    )
                } else {
                    BMIHistoryView(
                        calculations = savedCalculations,
                        onCalculationClick = { calc ->
                            age = calc.age.toString()
                            heightCm = calc.heightCm
                            weightKg = calc.weightKg
                            gender = calc.gender
                            classification = calc.classification
                            showCalculator = true
                        },
                        onCalculationDelete = { calc ->
                            scope.launch {
                                bmiDao.deleteCalculation(calc)
                            }
                        }
                    )
                }
            }
        }
        
        if (showSaveDialog) {
            SaveBMIDialog(
                bmiValue = bmiValue,
                category = bmiCategory,
                onDismiss = { showSaveDialog = false },
                onSave = { name ->
                    scope.launch {
                        age.toIntOrNull()?.let { ageInt ->
                            val calculation = BMICalculation(
                                name = name,
                                age = ageInt,
                                heightCm = heightCm,
                                weightKg = weightKg,
                                gender = gender,
                                classification = classification,
                                bmiValue = bmiValue,
                                category = bmiCategory
                            )
                            bmiDao.insertCalculation(calculation)
                            showSaveDialog = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun BMICalculatorView(
    age: String,
    heightCm: Float,
    weightKg: Float,
    gender: Gender,
    classification: BMIClassification,
    heightUnit: HeightUnit,
    weightUnit: WeightUnit,
    bmiValue: Float,
    bmiCategory: String,
    onAgeChange: (String) -> Unit,
    onHeightChange: (Float) -> Unit,
    onWeightChange: (Float) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onClassificationChange: (BMIClassification) -> Unit,
    onHeightUnitChange: (HeightUnit) -> Unit,
    onWeightUnitChange: (WeightUnit) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BMIInputCard(
                age = age,
                heightCm = heightCm,
                weightKg = weightKg,
                gender = gender,
                classification = classification,
                heightUnit = heightUnit,
                weightUnit = weightUnit,
                onAgeChange = onAgeChange,
                onHeightChange = onHeightChange,
                onWeightChange = onWeightChange,
                onGenderChange = onGenderChange,
                onClassificationChange = onClassificationChange,
                onHeightUnitChange = onHeightUnitChange,
                onWeightUnitChange = onWeightUnitChange
            )
        }
        
        if (age.toIntOrNull() != null && bmiValue > 0) {
            item {
                BMIResultCard(bmiValue, bmiCategory)
            }
            
            item {
                BMIGaugeCard(bmiValue)
            }
            
            item {
                IdealWeightCard(heightCm, gender)
            }
            
            item {
                HealthTipsCard(bmiValue)
            }
        }
    }
}

@Composable
fun BMIInputCard(
    age: String,
    heightCm: Float,
    weightKg: Float,
    gender: Gender,
    classification: BMIClassification,
    heightUnit: HeightUnit,
    weightUnit: WeightUnit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (Float) -> Unit,
    onWeightChange: (Float) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onClassificationChange: (BMIClassification) -> Unit,
    onHeightUnitChange: (HeightUnit) -> Unit,
    onWeightUnitChange: (WeightUnit) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "📝 Enter Your Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Age
            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("Age (years)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Cake, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Gender Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Gender.values().forEach { genderOption ->
                    FilterChip(
                        selected = gender == genderOption,
                        onClick = { onGenderChange(genderOption) },
                        label = { Text(genderOption.displayName) },
                        leadingIcon = if (gender == genderOption) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Height Input
            HeightInput(
                heightCm = heightCm,
                unit = heightUnit,
                onHeightChange = onHeightChange,
                onUnitChange = onHeightUnitChange
            )
            
            // Weight Input
            WeightInput(
                weightKg = weightKg,
                unit = weightUnit,
                onWeightChange = onWeightChange,
                onUnitChange = onWeightUnitChange
            )
            
            // Classification
            Text(
                "Classification System",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BMIClassification.values().forEach { classOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClassificationChange(classOption) }
                            .background(
                                if (classification == classOption)
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = classification == classOption,
                            onClick = { onClassificationChange(classOption) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            classOption.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeightInput(
    heightCm: Float,
    unit: HeightUnit,
    onHeightChange: (Float) -> Unit,
    onUnitChange: (HeightUnit) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Height",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HeightUnit.values().forEach { unitOption ->
                    FilterChip(
                        selected = unit == unitOption,
                        onClick = { onUnitChange(unitOption) },
                        label = { Text(unitOption.name) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        when (unit) {
            HeightUnit.CM -> {
                Slider(
                    value = heightCm,
                    onValueChange = onHeightChange,
                    valueRange = 50f..250f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${heightCm.roundToInt()} cm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            HeightUnit.FT_IN -> {
                val feet = (cmToFeet(heightCm)).toInt()
                val inches = ((cmToFeet(heightCm) - feet) * 12).roundToInt()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Feet", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = feet.toFloat(),
                            onValueChange = { newFeet ->
                                onHeightChange(feetAndInchesToCm(newFeet.toInt(), inches))
                            },
                            valueRange = 2f..8f,
                            steps = 5
                        )
                        Text(
                            "$feet ft",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inches", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = inches.toFloat(),
                            onValueChange = { newInches ->
                                onHeightChange(feetAndInchesToCm(feet, newInches.toInt()))
                            },
                            valueRange = 0f..11f,
                            steps = 10
                        )
                        Text(
                            "$inches in",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeightInput(
    weightKg: Float,
    unit: WeightUnit,
    onWeightChange: (Float) -> Unit,
    onUnitChange: (WeightUnit) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Weight",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WeightUnit.values().forEach { unitOption ->
                    FilterChip(
                        selected = unit == unitOption,
                        onClick = { onUnitChange(unitOption) },
                        label = { Text(unitOption.name) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val displayValue = when (unit) {
            WeightUnit.KG -> weightKg
            WeightUnit.LBS -> kgToLbs(weightKg)
        }
        
        val range = when (unit) {
            WeightUnit.KG -> 20f..200f
            WeightUnit.LBS -> 44f..440f
        }
        
        Slider(
            value = displayValue,
            onValueChange = { newValue ->
                onWeightChange(
                    when (unit) {
                        WeightUnit.KG -> newValue
                        WeightUnit.LBS -> lbsToKg(newValue)
                    }
                )
            },
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            when (unit) {
                WeightUnit.KG -> "${weightKg.roundToInt()} kg"
                WeightUnit.LBS -> "${kgToLbs(weightKg).roundToInt()} lbs"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BMIResultCard(bmiValue: Float, category: String) {
    val categoryColor = Color(getBMICategoryColor(bmiValue))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = categoryColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Your BMI",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatBMI(bmiValue),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = categoryColor
            )
            Text(
                category,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = categoryColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                getBMIInterpretation(bmiValue, category),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BMIGaugeCard(bmiValue: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "📊 BMI Scale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            BMIGauge(bmiValue)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // BMI Categories Legend
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WHOBMICategory.values().take(4).forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.color))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                category.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            "${formatBMI(category.minBMI)} - ${if (category.maxBMI == Float.MAX_VALUE) "∞" else formatBMI(category.maxBMI)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BMIGauge(bmiValue: Float) {
    val categories = listOf(
        Triple(18.5f, Color(0xFF42A5F5), "Under"),
        Triple(25f, Color(0xFF4CAF50), "Normal"),
        Triple(30f, Color(0xFFFFA726), "Over"),
        Triple(40f, Color(0xFFE53935), "Obese")
    )
    
    // Clamp BMI for display
    val displayBMI = bmiValue.coerceIn(15f, 40f)
    val position = (displayBMI - 15f) / 25f // Normalize to 0-1 range
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // Color gradient bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xFF42A5F5),
                        0.35f to Color(0xFF4CAF50),
                        0.60f to Color(0xFFFFA726),
                        1f to Color(0xFFE53935)
                    )
                )
        )
        
        // BMI indicator
        Box(
            modifier = Modifier
                .offset(x = (position * 100).coerceIn(0f, 100f).dp * 3.2f)
                .align(Alignment.CenterStart)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        formatBMI(bmiValue),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun IdealWeightCard(heightCm: Float, gender: Gender) {
    val weightCategories = getWeightCategoryTable(heightCm)
    val (minIdeal, maxIdeal) = getIdealWeightRange(heightCm, gender)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "⚖️ Weight Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Based on your height (${heightCm.roundToInt()} cm)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            weightCategories.forEach { category ->
                WeightCategoryRow(category)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "✅ Your Ideal Weight Range",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${minIdeal.roundToInt()} - ${maxIdeal.roundToInt()} kg",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${kgToLbs(minIdeal).roundToInt()} - ${kgToLbs(maxIdeal).roundToInt()} lbs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WeightCategoryRow(category: WeightCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(category.color).copy(alpha = 0.1f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(category.color))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                category.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (category.maxWeight == Float.MAX_VALUE)
                    "> ${category.minWeight.roundToInt()} kg"
                else
                    "${category.minWeight.roundToInt()} - ${category.maxWeight.roundToInt()} kg",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(category.color)
            )
            Text(
                if (category.maxWeight == Float.MAX_VALUE)
                    "> ${kgToLbs(category.minWeight).roundToInt()} lbs"
                else
                    "${kgToLbs(category.minWeight).roundToInt()} - ${kgToLbs(category.maxWeight).roundToInt()} lbs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HealthTipsCard(bmiValue: Float) {
    val tips = getHealthTips(bmiValue)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "💡 Health Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        tip,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BMIHistoryView(
    calculations: List<BMICalculation>,
    onCalculationClick: (BMICalculation) -> Unit,
    onCalculationDelete: (BMICalculation) -> Unit
) {
    if (calculations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text("💪", fontSize = 80.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Saved Calculations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Calculate your BMI and save it to track progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(calculations, key = { it.id }) { calculation ->
                BMIHistoryCard(
                    calculation = calculation,
                    onClick = { onCalculationClick(calculation) },
                    onDelete = { onCalculationDelete(calculation) }
                )
            }
        }
    }
}

@Composable
fun BMIHistoryCard(
    calculation: BMICalculation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = Color(getBMICategoryColor(calculation.bmiValue))
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    formatBMI(calculation.bmiValue),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    calculation.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    calculation.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = categoryColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${calculation.gender.displayName} • ${calculation.age}y • ${calculation.heightCm.roundToInt()}cm • ${calculation.weightKg.roundToInt()}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SaveBMIDialog(
    bmiValue: Float,
    category: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("My BMI") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Save, contentDescription = null) },
        title = { Text("Save BMI Calculation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "BMI: ${formatBMI(bmiValue)} ($category)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g., January 2025") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

