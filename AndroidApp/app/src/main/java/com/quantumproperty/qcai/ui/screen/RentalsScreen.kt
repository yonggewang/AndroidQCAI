package com.quantumproperty.qcai.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.quantumproperty.qcai.data.RentalModel
import com.quantumproperty.qcai.ui.viewmodel.RentalsViewModel
import com.quantumproperty.qcai.data.UserProfile
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsScreen(
    userProfile: UserProfile?,
    onBack: () -> Unit
) {
    val viewModel: RentalsViewModel = viewModel()
    val rentals by viewModel.rentals.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadRentals()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Housing & Rentals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show FAB for verified members (trustLevel > 0)
            if (userProfile != null && userProfile.vipLevel > 0) {
                FloatingActionButton(
                    onClick = {
                        viewModel.clearError()
                        showCreateDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Create Listing")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "offer",
                    onClick = { viewModel.setFilterType("offer") },
                    label = { Text("For Rent") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                
                FilterChip(
                    selected = filterType == "request",
                    onClick = { viewModel.setFilterType("request") },
                    label = { Text("Seeking") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    rentals.isEmpty() -> {
                        Text(
                            "No rentals available.",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(rentals) { rental ->
                                RentalCard(
                                    rental = rental,
                                    userProfile = userProfile,
                                    onDelete = { viewModel.deleteRental(rental.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Error Snackbar
    errorMessage?.let { message ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(message)
        }
    }
    
    // Create Rental Dialog
    if (showCreateDialog) {
        CreateRentalDialog(
            isLoading = isLoading,
            errorMessage = errorMessage,
            onDismiss = {
                viewModel.clearError()
                showCreateDialog = false
            },
            onCreate = { title, description, price, location, rentalType, contactInfo, imageFile ->
                viewModel.createRental(title, description, price, location, rentalType, contactInfo, imageFile) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun RentalCard(
    rental: RentalModel,
    userProfile: UserProfile?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (rental.rentalType == "offer") 
                        Color(0xFF2196F3).copy(alpha = 0.1f) 
                    else 
                        Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (rental.rentalType == "offer") "🏠 FOR RENT" else "🙋 SEEKING RENTAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rental.rentalType == "offer") Color(0xFF2196F3) else Color(0xFFFF9800)
                        )
                    }
                }
                
                // Price
                Text(
                    text = "$${rental.price.toInt()}/mo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = rental.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            
            // Image
            rental.imageUrl?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = url,
                    contentDescription = "Rental Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = rental.description,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rental.location,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Author",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rental.authorUsername,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    rental.contactInfo?.let { contact ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Contact",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contact,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Delete button - only for owner or super user (Level 99+)
                if (userProfile != null && 
                    (rental.authorUsername == userProfile.username || userProfile.isAdmin)) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRentalDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, Double, String, String, String?, File?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var rentalType by remember { mutableStateOf("offer") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
    }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("New Listing") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = rentalType == "offer",
                        onClick = { if (!isLoading) rentalType = "offer" },
                        label = { Text("House for Rent") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                    FilterChip(
                        selected = rentalType == "request",
                        onClick = { if (!isLoading) rentalType = "request" },
                        label = { Text("Seeking Rental") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                }
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. 2BR Apartment in Uptown)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Monthly Rent") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("$") },
                    enabled = !isLoading
                )
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (Address or Area)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("Contact Info (Phone/Email)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4,
                    enabled = !isLoading
                )
                
                // Image Picker
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, "Add Photo")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (imageUri == null) "Select Photo" else "Change Photo")
                }
                
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Fair Housing Guidelines
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Anti-Discrimination Policy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "By posting, you agree to comply with Fair Housing laws. You must not discriminate based on race, color, national origin, religion, sex, familial status, or disability.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val priceValue = price.toDoubleOrNull() ?: 0.0
                    val imageFile = imageUri?.let { uri ->
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val file = File(context.cacheDir, "rental_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { outputStream ->
                                inputStream?.copyTo(outputStream)
                            }
                            file
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val contact = if (contactInfo.isNotEmpty()) contactInfo else null
                    onCreate(title, description, priceValue, location, rentalType, contact, imageFile)
                },
                enabled = title.isNotEmpty() && price.isNotEmpty() && !isLoading
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
