package com.quantumproperty.qcai.data

import kotlin.math.ceil

class AIRecommendationEngine {
    
    private val catalogService = HardwareCatalogService.shared
    
    // MARK: - Model Selection Pipeline
    
    fun recommendModel(response: SurveyResponse): AIModel {
        val candidates = catalogService.models
        
        // Priority 1: Context Size (Large Docs force massive context)
        if (response.inputSize == "largeDocs") {
            candidates.firstOrNull { it.contextWindow >= 128000 && it.parameters >= 70 }?.let { return it }
        }
        
        // Priority 2: Industry/Task Specialization (Benchmarks)
        val industry = response.businessType.lowercase()
        val goals = response.aiGoals
        
        if (industry.contains("legal") || industry.contains("firm") || industry.contains("enterprise")) {
            // Force reasoning depth (70B+ class)
            candidates.firstOrNull { it.parameters >= 70 }?.let { return it }
        }
        
        if (goals.contains("coding")) {
            candidates.firstOrNull { it.id.contains("coder") || it.name.contains("Coder") }?.let { return it }
        }
        
        if (response.visionNeeds) {
            candidates.firstOrNull { it.id.contains("vision") || it.name.contains("Vision") }?.let { return it }
        }
        
        // Fallback: Use Accuracy preference
        return if (response.accuracyLevel == "High-Precision") {
            candidates.sortedByDescending { it.parameters }.firstOrNull() ?: candidates.first()
        } else {
            candidates.sortedBy { it.parameters }.firstOrNull() ?: candidates.first()
        }
    }
    
    // MARK: - Scientific Math
    
    fun calculateScientificVRAM(model: AIModel, accuracy: String): Int {
        // Quantization Ratios: High(8-bit)=1.2, Balanced(6-bit)=0.9, Fast(4-bit)=0.75
        val ratio = when (accuracy) {
            "High-Precision" -> 1.2
            "Balanced" -> 0.9
            else -> 0.75
        }
        
        // Overhead for KV Cache/Activations
        val overhead = when (accuracy) {
            "High-Precision" -> 1.2
            "Balanced" -> 1.15
            else -> 1.1
        }
        
        val minNeeded = (model.parameters * ratio) * overhead
        return ceil(minNeeded).toInt()
    }
    
    fun getMinimumBandwidth(teamSize: String): Int {
        return when (teamSize) {
            "enterprise" -> 800 // HBM Tier
            "smallTeam" -> 500  // High-end Consumer
            else -> 250        // Standard Consumer
        }
    }
    
    // MARK: - Hardware Matching
    
    fun matchHardware(vramNeeded: Int, bandwidthNeeded: Int, catalog: List<HardwareBundle>, platformPreference: String): HardwareBundle {
        var candidates = catalog.toMutableList()
        
        // Requirement 1: VRAM Capacity (Absolute Floor)
        candidates = candidates.filter { it.vram >= vramNeeded }.toMutableList()
        
        // Requirement 2: Memory Bandwidth (Performance Floor)
        candidates = candidates.filter { it.memoryBandwidth >= bandwidthNeeded }.toMutableList()
        
        // Fallback: If bandwidth too high, lower it but keep VRAM
        if (candidates.isEmpty()) {
            candidates = catalog.filter { it.vram >= vramNeeded }.toMutableList()
        }
        
        // Filter by Platform Preference (Soft Filter)
        val pref = platformPreference.lowercase()
        val platformOptions = if (pref.contains("mac") || pref.contains("apple")) {
            candidates.filter { it.name.contains("Mac") || it.gpu.contains("Apple") }
        } else if (pref.contains("nvidia") || pref.contains("rtx")) {
            candidates.filter { it.gpu.contains("NVIDIA") || it.gpu.contains("RTX") }
        } else if (pref.contains("amd") || pref.contains("radeon") || pref.contains("rocm")) {
            candidates.filter { it.gpu.contains("AMD") || it.gpu.contains("Radeon") || it.gpu.contains("Ryzen") }
        } else {
            candidates.filter { !it.name.contains("Mac") && !it.gpu.contains("Apple") }
        }
        
        if (platformOptions.isNotEmpty()) {
            candidates = platformOptions.toMutableList()
        }
        
        // Sort by price (cheapest capable)
        candidates.sortBy { it.price }
        
        return candidates.firstOrNull() ?: catalog.last()
    }
    
    // MARK: - Full Recommendation
    
    data class RecommendationResult(
        val model: AIModel,
        val hardware: HardwareBundle,
        val vramNeeded: Int,
        val justification: String
    )
    
    fun generateRecommendation(response: SurveyResponse): RecommendationResult {
        val model = recommendModel(response)
        val vramNeeded = calculateScientificVRAM(model, response.accuracyLevel)
        val bandwidthNeeded = getMinimumBandwidth(response.teamSize)
        val hardware = matchHardware(vramNeeded, bandwidthNeeded, catalogService.bundles, response.platformPreference)
        
        val justification = createJustification(response, model, hardware, vramNeeded)
        
        return RecommendationResult(model, hardware, vramNeeded, justification)
    }
    
    private fun createJustification(response: SurveyResponse, model: AIModel, hardware: HardwareBundle, vram: Int): String {
        val reasons = mutableListOf<String>()
        
        // Model Justification
        if (response.businessType.contains("Legal", ignoreCase = true) || response.businessType.contains("Enterprise", ignoreCase = true)) {
            reasons.add("Selected ${model.name} for its high reasoning depth required in ${response.businessType}.")
        } else if (response.aiGoals.contains("coding")) {
            reasons.add("Choosing ${model.name} based on its top-tier HumanEval coding benchmarks.")
        } else {
            reasons.add("Optimized for ${model.name} to balance speed and intelligence.")
        }
        
        // Hardware Justification
        val precision = when (response.accuracyLevel) {
            "High-Precision" -> "8-bit high precision"
            "Balanced" -> "balanced 6-bit"
            else -> "fast 4-bit"
        }
        reasons.add("Running this model at $precision requires ~${vram}GB of VRAM.")
        
        if (response.teamSize == "enterprise" || response.teamSize == "smallTeam") {
            reasons.add("A bandwidth of ${hardware.memoryBandwidth}GB/s was selected to support high-intensity concurrent access.")
        }
        
        return reasons.joinToString(" ")
    }
    
    // MARK: - Hardware Alternatives
    
    fun getAllCompatibleHardware(vramNeeded: Int, catalog: List<HardwareBundle>, platformPreference: String): List<HardwareBundle> {
        var candidates = catalog.filter { it.vram >= vramNeeded }
        
        // Filter by Platform Preference (Soft Filter)
        val pref = platformPreference.lowercase()
        val platformOptions = if (pref.contains("mac") || pref.contains("apple")) {
            candidates.filter { it.name.contains("Mac") || it.gpu.contains("Apple") }
        } else if (pref.contains("nvidia") || pref.contains("rtx")) {
            candidates.filter { it.gpu.contains("NVIDIA") || it.gpu.contains("RTX") }
        } else if (pref.contains("amd") || pref.contains("radeon") || pref.contains("rocm")) {
            candidates.filter { it.gpu.contains("AMD") || it.gpu.contains("Radeon") || it.gpu.contains("Ryzen") }
        } else {
            candidates.filter { !it.name.contains("Mac") && !it.gpu.contains("Apple") }
        }
        
        return if (platformOptions.isNotEmpty()) {
            platformOptions.sortedBy { it.price }
        } else {
            candidates.sortedBy { it.price }
        }
    }
    
    fun getUpgradeJustification(hardware: HardwareBundle, vramNeeded: Int): String {
        val vramHeadroom = hardware.vram - vramNeeded
        return when {
            vramHeadroom >= 100 -> "Run multiple large AI models simultaneously + Future-proof for next-gen models"
            vramHeadroom >= 40 -> "Future-proof: Can run 70B+ parameter models for advanced use cases"
            vramHeadroom >= 20 -> "Extra capacity for multi-user workloads + Run larger document batches"
            vramHeadroom >= 10 -> "Better performance with headroom for model updates"
            else -> "Premium hardware for professional reliability"
        }
    }
    
    // MARK: - ROI Calculation
    
    data class ROIResult(val annualSavings: Double, val breakEvenMonths: Double)
    
    fun calculateROI(
        hourlyRate: Double,
        hoursPerWeek: Double,
        automationPercent: Double,
        hardwareCost: Double
    ): ROIResult {
        val annualSavings = hourlyRate * hoursPerWeek * 52.0 * (automationPercent / 100.0)
        val breakEvenMonths = if (annualSavings > 0) hardwareCost / (annualSavings / 12.0) else 0.0
        return ROIResult(annualSavings, breakEvenMonths)
    }
}
