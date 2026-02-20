package com.quantumproperty.qcai.data

import com.google.gson.annotations.SerializedName

// Enum for Privacy
enum class DataPrivacy {
    @SerializedName("local_only") LOCAL_ONLY,
    @SerializedName("cloud_ok") CLOUD_OK,
    @SerializedName("unsure") UNSURE
}

// Data Models for Catalog
data class AIModel(
    val id: String,
    val name: String,
    val family: String,
    @SerializedName("parameter_size") val parameterSize: String,
    @SerializedName("context_window") val contextWindow: Int,
    @SerializedName("is_open_source") val isOpenSource: Boolean,
    @SerializedName("best_for") val bestFor: List<String>,
    @SerializedName("min_vram") val minVRAM: Int? = null,
    val parameters: Double,
    @SerializedName("model_url") val modelUrl: String = "" // Added default for backward compatibility
)

data class AICompatibility(
    @SerializedName("model_id") val modelId: String,
    val recommended: Boolean
)

data class HardwareBundle(
    val id: String,
    val name: String,
    val gpu: String,
    val price: Double,
    val vram: Int,
    @SerializedName("memory_bandwidth") val memoryBandwidth: Int,
    val tier: String,
    @SerializedName("best_for") val bestFor: String,
    @SerializedName("partner_name") val partnerName: String,
    @SerializedName("purchase_url") val purchaseUrl: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("diy_template") val diyTemplate: String = "diy_midrange",
    @SerializedName("ai_compatibility") val aiCompatibility: List<AICompatibility>? = emptyList()
)

data class Professional(
    val id: String,
    val name: String,
    val title: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("linkedin_url") val linkedinUrl: String,
    val email: String,
    val experience: String,
    val bio: String,
    val specialties: List<String>,
    @SerializedName("why_verified") val whyVerified: String
)

data class HardwareCatalog(
    @SerializedName("hardware_bundles") val hardwareBundles: List<HardwareBundle>,
    @SerializedName("ai_models") val aiModels: List<AIModel>,
    @SerializedName("verified_professionals") val verifiedProfessionals: List<Professional> = emptyList()
)

// Survey Response
data class SurveyResponse(
    var businessType: String = "", // Maps to Industry
    var teamSize: String = "",     // Maps to UsageIntensity
    var aiGoals: Set<String> = emptySet(), // Maps to TaskType
    var dataPrivacy: DataPrivacy = DataPrivacy.UNSURE,
    var technicalSkill: String = "", 
    var inputSize: String = "",
    var accuracyLevel: String = "Balanced", // New: Creative/Fast, Balanced, High-Precision
    var existingHardware: String = "",
    var platformPreference: String = "", 
    var multiUserNeeds: Boolean = false,
    var visionNeeds: Boolean = false,
    var timeline: String = ""
)
