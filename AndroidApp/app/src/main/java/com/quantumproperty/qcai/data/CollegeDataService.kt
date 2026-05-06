package com.quantumproperty.qcai.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStreamReader

class CollegeDataService private constructor() {
    companion object {
        val shared = CollegeDataService()
    }
    
    private val _universities = MutableStateFlow<List<University>>(emptyList())
    val universities = _universities.asStateFlow()
    
    private var isLoaded = false
    
    fun loadData(context: Context) {
        if (isLoaded) return
        
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("universities.json")
            val reader = InputStreamReader(inputStream)
            
            val type = object : TypeToken<List<University>>() {}.type
            val loadedList: List<University> = Gson().fromJson(reader, type)
            
            _universities.value = loadedList
            isLoaded = true
            android.util.Log.d("CollegeDataService", "Loaded ${loadedList.size} universities from assets.")
        } catch (e: Exception) {
            android.util.Log.e("CollegeDataService", "Failed to load universities: ${e.message}")
            e.printStackTrace()
        }
    }
}
