package com.quantumproperty.qcai.data

import com.google.gson.annotations.SerializedName

enum class ActivityValue {
    @SerializedName("STATIONARY") STATIONARY,
    @SerializedName("WALKING") WALKING,
    @SerializedName("RUNNING") RUNNING,
    @SerializedName("CYCLING") CYCLING,
    @SerializedName("AUTOMOTIVE") AUTOMOTIVE,
    @SerializedName("UNKNOWN") UNKNOWN
}

enum class AttentionMode {
    @SerializedName("DEEP_WORK") DEEP_WORK,
    @SerializedName("DISTRACTED") DISTRACTED,
    @SerializedName("COMMUTING") COMMUTING,
    @SerializedName("RELAXED") RELAXED
}

data class ContextObject(
    val version: String = "5.5",
    val metadata: ContextMetadata,
    val userState: UserState,
    @SerializedName("temporal_intelligence") val temporalIntelligence: TemporalIntelligence,
    @SerializedName("safety_policy") val safetyPolicy: SafetyPolicy = SafetyPolicy(),
    @SerializedName("learning_loop") val learningLoop: LearningLoop = LearningLoop()
) {
    data class ContextMetadata(
        @SerializedName("session_id") val sessionId: String,
        @SerializedName("priority_score") val priorityScore: Double,
        val trigger: String,
        @SerializedName("current_timestamp") val currentTimestamp: Long = System.currentTimeMillis()
    )

    data class UserState(
        val activity: ActivityState,
        val attention: AttentionState,
        @SerializedName("stress_score") val stressScore: Double,
        val location: LocationState? = null,
        val device: DeviceStatus? = null
    )

    data class DeviceStatus(
        @SerializedName("battery_level") val batteryLevel: Float,
        @SerializedName("battery_state") val batteryState: String,
        @SerializedName("thermal_state") val thermalState: String,
        @SerializedName("low_power_mode") val lowPowerMode: Boolean,
        @SerializedName("connection_type") val connectionType: String,
        @SerializedName("focus_mode") val focusMode: String? = null
    )

    data class ActivityState(
        val value: ActivityValue,
        val confidence: Double,
        @SerializedName("steps_today") val stepsToday: Int? = null,
        @SerializedName("active_calories") val activeCalories: Int? = null,
        val distance: Double? = null,
        val floors: Int? = null,
        @SerializedName("walking_speed") val walkingSpeed: Double? = null,
        @SerializedName("step_length") val stepLength: Double? = null,
        val trends: ActivityTrends? = null,
        val vitals: Vitals? = null
    )

    data class ActivityTrends(
        @SerializedName("step_trend") val stepTrend: Double,
        @SerializedName("calorie_trend") val calorieTrend: Double
    )

    data class Vitals(
        @SerializedName("resting_heart_rate") val restingHeartRate: Double? = null,
        val hrv: Double? = null,
        @SerializedName("oxygen_saturation") val oxygenSaturation: Double? = null,
        @SerializedName("sleep_quality") val sleepQuality: String? = null
    )

    data class AttentionState(
        val mode: AttentionMode,
        @SerializedName("intent_mismatch") val intentMismatch: Boolean
    )

    data class LocationState(
        val latitude: Double?,
        val longitude: Double?,
        val speed: Double,
        val classification: String? = null
    )

    data class TemporalIntelligence(
        @SerializedName("trend_3h") val trend3h: String,
        @SerializedName("habit_compliance") val habitCompliance: String,
        @SerializedName("upcoming_events") val upcomingEvents: List<CalendarEvent> = emptyList(),
        @SerializedName("pending_tasks") val pendingTasks: List<ReminderTask> = emptyList(),
        @SerializedName("app_usage") val appUsage: Map<String, Double>? = null
    )

    data class CalendarEvent(
        val title: String,
        @SerializedName("start_date") val startDate: Long,
        @SerializedName("end_date") val endDate: Long,
        val location: String? = null
    )

    data class ReminderTask(
        val title: String,
        @SerializedName("due_date") val dueDate: Long? = null,
        @SerializedName("is_completed") val isCompleted: Boolean = false,
        val priority: Int = 0
    )

    data class SafetyPolicy(
        @SerializedName("auto_execute") val autoExecute: Boolean = false,
        @SerializedName("restricted_scopes") val restrictedScopes: List<String> = listOf("FINANCIAL")
    )

    data class LearningLoop(
        @SerializedName("last_action") val lastAction: String? = null,
        val reward: Double = 0.0,
        @SerializedName("user_feedback") val userFeedback: String? = null,
        @SerializedName("decay_half_life_days") val decayHalfLifeDays: Int = 7
    )
}

enum class DataDomain {
    BIOMETRICS, TEMPORAL, ATTENTION, MOTION, VISION, LOCATION
}
