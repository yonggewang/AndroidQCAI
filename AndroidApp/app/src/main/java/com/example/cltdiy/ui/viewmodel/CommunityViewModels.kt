package com.example.cltdiy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cltdiy.data.CommunityAPIService
import com.example.cltdiy.data.EventModel
import com.example.cltdiy.data.MarketplaceItemModel
import com.example.cltdiy.data.RentalModel
import com.example.cltdiy.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// Events ViewModel
class EventsViewModel : ViewModel() {
    private val apiService = CommunityAPIService.instance
    
    private val _events = MutableStateFlow<List<EventModel>>(emptyList())
    val events: StateFlow<List<EventModel>> = _events.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _events.value = apiService.fetchEvents()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createEvent(
        title: String,
        description: String,
        eventDate: String,
        location: String,
        imageFile: File?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val imageUrl = imageFile?.let { apiService.uploadImage(it) }
                apiService.createEvent(title, description, eventDate, location, imageUrl)
                loadEvents() // Reload list
                onComplete()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteEvent(id: Int) {
        viewModelScope.launch {
            try {
                apiService.deleteEvent(id)
                _events.value = _events.value.filter { it.id != id }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

// Marketplace ViewModel
class MarketplaceViewModel : ViewModel() {
    private val apiService = CommunityAPIService.instance
    
    private val _items = MutableStateFlow<List<MarketplaceItemModel>>(emptyList())
    val items: StateFlow<List<MarketplaceItemModel>> = _items.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _items.value = apiService.fetchMarketplaceItems()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createItem(
        title: String,
        description: String,
        price: Double,
        condition: String,
        imageFile: File?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val imageUrl = imageFile?.let { apiService.uploadImage(it) }
                apiService.createMarketplaceItem(title, description, price, condition, imageUrl)
                loadItems() // Reload list
                onComplete()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteItem(id: Int) {
        viewModelScope.launch {
            try {
                apiService.deleteMarketplaceItem(id)
                _items.value = _items.value.filter { it.id != id }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

// Rentals ViewModel
class RentalsViewModel : ViewModel() {
    private val apiService = CommunityAPIService.instance
    
    private val _rentals = MutableStateFlow<List<RentalModel>>(emptyList())
    val rentals: StateFlow<List<RentalModel>> = _rentals.asStateFlow()
    
    private val _filterType = MutableStateFlow<String?>("offer") // "offer" or "request"
    val filterType: StateFlow<String?> = _filterType.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadRentals() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _rentals.value = apiService.fetchRentals(type = _filterType.value)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setFilterType(type: String) {
        _filterType.value = type
        loadRentals()
    }
    
    fun createRental(
        title: String,
        description: String,
        price: Double,
        location: String,
        rentalType: String,
        contactInfo: String?,
        imageFile: File?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val imageUrl = imageFile?.let { apiService.uploadImage(it) }
                apiService.createRental(title, description, price, location, rentalType, contactInfo, imageUrl)
                loadRentals() // Reload list
                onComplete()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteRental(id: Int) {
        viewModelScope.launch {
            try {
                apiService.deleteRental(id)
                _rentals.value = _rentals.value.filter { it.id != id }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
