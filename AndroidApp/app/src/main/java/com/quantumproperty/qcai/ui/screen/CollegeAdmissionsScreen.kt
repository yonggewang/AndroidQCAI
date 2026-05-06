package com.quantumproperty.qcai.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumproperty.qcai.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeAdmissionsScreen(
    appLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val states = listOf("NC", "SC", "GA", "VA", "FL", "NY", "CA", "TX", "MA", "IL", "PA", "OH", "NJ", "MI", "WA", "CO", "MD", "CT", "OR", "Other")

    // Observe data
    val universities by CollegeDataService.shared.universities.collectAsState()
    
    // Auto load
    LaunchedEffect(Unit) {
        if (universities.isEmpty()) {
            CollegeDataService.shared.loadData(context)
        }
    }

    var selectedUniversity by remember { mutableStateOf<University?>(null) }

    // Student Profile State
    var gpaText by remember { mutableStateOf("") }
    var satText by remember { mutableStateOf("") }
    var actText by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf(states[0]) }
    var selectedRank by remember { mutableStateOf(ClassRankPercentile.NOT_SURE) }
    var selectedMajor by remember { mutableStateOf("Undecided") }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(CollegeSortOption.CATEGORY) }

    if (selectedUniversity != null) {
        val student = StudentProfile(
            gpa = gpaText.toDoubleOrNull() ?: 0.0,
            sat = satText.toIntOrNull(),
            act = actText.toIntOrNull(),
            state = selectedState,
            classRank = selectedRank,
            intendedMajor = if (selectedMajor == "Undecided") null else selectedMajor
        )
        CollegeDetailScreen(
            university = selectedUniversity!!,
            student = student,
            isEnglish = isEnglish,
            onBack = { selectedUniversity = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        // App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = if (isEnglish) "College Match" else "AI 升学助手",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Input Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isEnglish) "Your Profile" else "您的档案",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6200EE)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = gpaText,
                                onValueChange = { gpaText = it },
                                label = { Text("GPA (4.0)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            CollegeDropdown(
                                label = if (isEnglish) "State" else "所在州",
                                options = states,
                                selected = selectedState,
                                onSelected = { selectedState = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = satText,
                                onValueChange = { satText = it },
                                label = { Text("SAT") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = actText,
                                onValueChange = { actText = it },
                                label = { Text("ACT") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        CollegeDropdown(
                            label = if (isEnglish) "Class Rank" else "班级排名",
                            options = ClassRankPercentile.values().map { it.label },
                            selected = selectedRank.label,
                            onSelected = { label ->
                                selectedRank = ClassRankPercentile.values().find { it.label == label } ?: ClassRankPercentile.NOT_SURE
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CollegeDropdown(
                            label = if (isEnglish) "Intended Major" else "意向专业",
                            options = StandardMajors.all,
                            selected = selectedMajor,
                            onSelected = { selectedMajor = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isEnglish) "Search universities..." else "搜索大学...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // Results calculation
            val currentGpa = gpaText.toDoubleOrNull() ?: 0.0
            
            val baseResults = if (currentGpa > 0.0 || searchQuery.isNotBlank()) {
                var list = universities
                if (searchQuery.isNotBlank()) {
                    list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                }

                val studentProfile = StudentProfile(
                    gpa = currentGpa,
                    sat = satText.toIntOrNull(),
                    act = actText.toIntOrNull(),
                    state = selectedState,
                    classRank = selectedRank,
                    intendedMajor = if (selectedMajor == "Undecided") null else selectedMajor
                )

                list.map { univ ->
                    val res = CollegeAdmissionEngine.shared.analyze(studentProfile, univ)
                    Pair(univ, res)
                }
            } else {
                emptyList()
            }

            // Sorting logic
            val finalResults = when (sortOption) {
                CollegeSortOption.CATEGORY -> baseResults.sortedBy { 
                    when(it.second.category) {
                        AdmissionCategory.SAFETY -> 0
                        AdmissionCategory.MATCH -> 1
                        AdmissionCategory.REACH -> 2
                    }
                }
                CollegeSortOption.TUITION_LOW -> baseResults.sortedBy { it.second.tuitionEstimate ?: Int.MAX_VALUE }
                CollegeSortOption.TUITION_HIGH -> baseResults.sortedByDescending { it.second.tuitionEstimate ?: 0 }
                CollegeSortOption.ACCEPTANCE_HIGH -> baseResults.sortedByDescending { it.first.admissions.acceptanceRateOverall }
                CollegeSortOption.ACCEPTANCE_LOW -> baseResults.sortedBy { it.first.admissions.acceptanceRateOverall }
                CollegeSortOption.SELECTIVITY -> baseResults.sortedBy { 
                    when(it.first.selectivityTier) {
                        SelectivityTier.ELITE -> 0
                        SelectivityTier.HIGH -> 1
                        SelectivityTier.MEDIUM -> 2
                        SelectivityTier.LOW -> 3
                    }
                }
                CollegeSortOption.NATIONAL_RANK -> baseResults.sortedBy { it.first.academics.nationalRanking ?: Int.MAX_VALUE }
                CollegeSortOption.ALPHABETICAL -> baseResults.sortedBy { it.first.name }
            }

            if (finalResults.isNotEmpty()) {
                // Sort Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CollegeSortOption.values()) { option ->
                            SortChip(
                                option = option,
                                isSelected = sortOption == option,
                                onClick = { sortOption = option }
                            )
                        }
                    }
                }

                // Grouped or List display
                if (sortOption == CollegeSortOption.CATEGORY) {
                    val grouped = finalResults.groupBy { it.second.category }
                    
                    // Display Safety
                    val safeties = grouped[AdmissionCategory.SAFETY] ?: emptyList()
                    if (safeties.isNotEmpty()) {
                        item { CategoryHeader(if (isEnglish) "Safety Schools" else "保底学校", Color(0xFF4CAF50)) }
                        items(safeties) { (univ, res) ->
                             UniversityRow(univ, result = res, showCategoryLabel = false, onClick = { selectedUniversity = univ })
                        }
                    }

                    // Display Match
                    val matches = grouped[AdmissionCategory.MATCH] ?: emptyList()
                    if (matches.isNotEmpty()) {
                        item { CategoryHeader(if (isEnglish) "Match Schools" else "匹配学校", Color(0xFF2196F3)) }
                        items(matches) { (univ, res) ->
                             UniversityRow(univ, result = res, showCategoryLabel = false, onClick = { selectedUniversity = univ })
                        }
                    }

                    // Display Reach
                    val reaches = grouped[AdmissionCategory.REACH] ?: emptyList()
                    if (reaches.isNotEmpty()) {
                        item { CategoryHeader(if (isEnglish) "Reach Schools" else "冲刺学校", Color(0xFFF44336)) }
                        items(reaches) { (univ, res) ->
                             UniversityRow(univ, result = res, showCategoryLabel = false, onClick = { selectedUniversity = univ })
                        }
                    }
                } else {
                    // Just flat list if not sorting by category
                    items(finalResults) { (univ, res) ->
                        UniversityRow(univ, result = res, showCategoryLabel = true, onClick = { selectedUniversity = univ })
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (currentGpa == 0.0 && searchQuery.isBlank()) 
                                (if (isEnglish) "Enter your GPA or search to see schools." else "输入GPA或搜索以查看学校")
                                else (if (isEnglish) "No schools found." else "未找到学校"),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SortChip(
    option: CollegeSortOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(if (isSelected) Color(0xFF6200EE) else Color.White)
    val contentColor by animateColorAsState(if (isSelected) Color.White else Color.Black)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Text(
            text = option.label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CategoryHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    }
}

@Composable
fun UniversityRow(university: University, result: AdmissionResult? = null, showCategoryLabel: Boolean = false, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = university.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val rankText = university.academics.nationalRanking?.let { "#$it" } ?: "200+"
                    Surface(
                        color = Color(0xFF6200EE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "National Rank: $rankText",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    if (showCategoryLabel && result != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val categoryColor = when (result.category) {
                            AdmissionCategory.SAFETY -> Color(0xFF4CAF50)
                            AdmissionCategory.MATCH -> Color(0xFF2196F3)
                            AdmissionCategory.REACH -> Color(0xFFF44336)
                        }
                        Surface(
                            color = categoryColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = result.category.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${university.meta.city}, ${university.state}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = Color.LightGray
            )
        }
    }
}
