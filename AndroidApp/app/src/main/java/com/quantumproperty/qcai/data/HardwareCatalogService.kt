package com.quantumproperty.qcai.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HardwareCatalogService private constructor() {

    private val catalogURL = "https://qcai-net.github.io/aihardware/catalog.json"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // Fallback Data
    private val fallbackBundles: List<HardwareBundle> = listOf(
        HardwareBundle(
            id = "entry_amazon_cyberpower",
            name = "Entry Tier - CyberPowerPC Gamer Xtreme",
            gpu = "RTX 4060 Ti (16GB VRAM)",
            price = 1099.99,
            vram = 16,
            memoryBandwidth = 288,
            tier = "entry",
            bestFor = "Small Business & Customer Service Bots",
            partnerName = "Amazon",
            purchaseUrl = "https://www.amazon.com/dp/B0C7M8XN6S/?tag=queencityai-20",
            imageUrl = "https://m.media-amazon.com/images/I/71fV9p-8O9L._AC_SL1500_.jpg",
            diyTemplate = "diy_budget"
        ),
        HardwareBundle(
            id = "mid_tier_skytech",
            name = "Mid-Tier - Skytech King 95",
            gpu = "RTX 5070 Ti (16GB GDDR7)",
            price = 2699.99,
            vram = 16,
            memoryBandwidth = 672,
            tier = "mid",
            bestFor = "Fast Inference & Llama 8B Specialist",
            partnerName = "Amazon",
            purchaseUrl = "https://www.amazon.com/dp/B0DLN909X1/?tag=queencityai-20",
            imageUrl = "https://m.media-amazon.com/images/I/81+Xl9Z-MGL._AC_SL1500_.jpg",
            diyTemplate = "diy_midrange"
        ),
        HardwareBundle(
            id = "pro_tier_asus_rog",
            name = "Professional Tier - ASUS ROG G700",
            gpu = "RTX 5080 (16GB GDDR7)",
            price = 2749.99,
            vram = 16,
            memoryBandwidth = 960,
            tier = "pro",
            bestFor = "Multi-Agent AI & Coding Assistants",
            partnerName = "Amazon",
            purchaseUrl = "https://www.amazon.com/dp/B0D1V3N5K4/?tag=queencityai-20",
            imageUrl = "https://m.media-amazon.com/images/I/71p7eKk8IuL._AC_SL1500_.jpg",
            diyTemplate = "diy_midrange"
        ),
        HardwareBundle(
            id = "ultimate_amazon_msi",
            name = "Ultimate Tier - MSI Aegis RS2",
            gpu = "RTX 5090 (32GB GDDR7 VRAM)",
            price = 4899.99,
            vram = 32,
            memoryBandwidth = 1792,
            tier = "ultimate",
            bestFor = "Legal/Medical Firms & 70B+ Model Reasoning",
            partnerName = "Amazon",
            purchaseUrl = "https://www.amazon.com/dp/B0DZ909XLM/?tag=queencityai-20",
            imageUrl = "https://m.media-amazon.com/images/I/81WjX8rUvCL._AC_SL1500_.jpg",
            diyTemplate = "diy_enterprise"
        ),
        HardwareBundle(
            id = "creative_tier_mac_studio_m4",
            name = "Creative Tier - Mac Studio M4 Max",
            gpu = "M4 Max (128GB Unified Memory)",
            price = 3999.00,
            vram = 128,
            memoryBandwidth = 546,
            tier = "creative",
            bestFor = "Massive Context & Apple Ecosystem",
            partnerName = "Amazon",
            purchaseUrl = "https://www.amazon.com/dp/B0DJM5X9LV/?tag=queencityai-20",
            imageUrl = "https://m.media-amazon.com/images/I/51f9SDRyRML._AC_SL1500_.jpg",
            diyTemplate = "diy_midrange"
        )
    )

    private val fallbackModels: List<AIModel> = listOf(
        AIModel(
            id = "llama-3.1-8b",
            name = "Llama 3.1 8B",
            family = "Meta Llama",
            parameterSize = "8B",
            contextWindow = 128000,
            isOpenSource = true,
            bestFor = listOf("chatbot", "document_review", "data_analysis"),
            minVRAM = 8,
            parameters = 8.0,
            modelUrl = "https://huggingface.co/meta-llama/Llama-3.1-8B"
        ),
        AIModel(
            id = "mistral-7b",
            name = "Mistral 7B",
            family = "Mistral AI",
            parameterSize = "7B",
            contextWindow = 32000,
            isOpenSource = true,
            bestFor = listOf("chatbot", "data_analysis"),
            minVRAM = 7,
            parameters = 7.0,
            modelUrl = "https://huggingface.co/mistralai/Mistral-7B-v0.1"
        ),
        AIModel(
            id = "llama-3.1-70b",
            name = "Llama 3.1 70B",
            family = "Meta Llama",
            parameterSize = "70B",
            contextWindow = 128000,
            isOpenSource = true,
            bestFor = listOf("coding", "legal", "medical"),
            minVRAM = 40,
            parameters = 70.0,
            modelUrl = "https://huggingface.co/meta-llama/Llama-3.1-70B"
        ),
        AIModel(
            id = "deepseek-coder-v3",
            name = "DeepSeek Coder V3",
            family = "DeepSeek",
            parameterSize = "33B",
            contextWindow = 64000,
            isOpenSource = true,
            bestFor = listOf("coding"),
            minVRAM = 24,
            parameters = 33.0,
            modelUrl = "https://huggingface.co/deepseek-ai/deepseek-coder-33b-instruct"
        ),
        AIModel(
            id = "llama-3.2-vision",
            name = "Llama 3.2 Vision 11B",
            family = "Meta Llama",
            parameterSize = "11B",
            contextWindow = 128000,
            isOpenSource = true,
            bestFor = listOf("image_gen", "document_review"),
            minVRAM = 12,
            parameters = 11.0,
            modelUrl = "https://huggingface.co/meta-llama/Llama-3.2-11B-Vision"
        )
    )

    var bundles: List<HardwareBundle> = fallbackBundles
        private set
    var models: List<AIModel> = fallbackModels
        private set
    var professionals: List<Professional> = emptyList()
        private set
        
    var isLoading = false
        private set
    var lastError: String? = null
        private set

    suspend fun fetchCatalog() = withContext(Dispatchers.IO) {
        isLoading = true
        lastError = null
        println("Fetching Hardware Catalog...")
        
        try {
            val request = Request.Builder()
                .url(catalogURL)
                .get()
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val json = response.body?.string() ?: "{}"
                val catalog = gson.fromJson(json, HardwareCatalog::class.java)
                
                if (catalog != null) {
                    bundles = catalog.hardwareBundles
                    models = catalog.aiModels
                    professionals = catalog.verifiedProfessionals
                    android.util.Log.d("CatalogService", "Hardware Catalog loaded successfully: ${models.size} models, ${bundles.size} bundles, ${professionals.size} professionals")
                } else {
                    android.util.Log.e("CatalogService", "Failed to parse catalog: Catalog object is null")
                }
            } else {
                android.util.Log.e("CatalogService", "Failed to fetch catalog: ${response.code}")
                lastError = "Failed to fetch catalog: ${response.code}"
            }
        } catch (e: Exception) {
            android.util.Log.e("CatalogService", "Exception fetching catalog", e)
            lastError = e.message
        } finally {
            isLoading = false
        }
    }

    companion object {
        val shared = HardwareCatalogService()
    }
}
