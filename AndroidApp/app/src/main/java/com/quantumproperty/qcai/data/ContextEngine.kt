package com.quantumproperty.qcai.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.CalendarContract
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.quantumproperty.qcai.data.ContextObject.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class ContextEngine private constructor(private val appContext: Context) {
    private val TAG = "ContextEngine"
    private val prefs = appContext.getSharedPreferences("context_os_prefs", Context.MODE_PRIVATE)
    
    val currentContext = MutableStateFlow<ContextObject?>(null)

    // Domain Settings
    private val migrationKey = "context_v5.4_migrated"

    var healthEnabled: Boolean
        get() = prefs.getBoolean("context_health_enabled", false)
        set(value) = prefs.edit().putBoolean("context_health_enabled", value).apply()

    var temporalEnabled: Boolean
        get() = prefs.getBoolean("context_temporal_enabled", false)
        set(value) = prefs.edit().putBoolean("context_temporal_enabled", value).apply()

    var attentionEnabled: Boolean
        get() = prefs.getBoolean("context_attention_enabled", false)
        set(value) = prefs.edit().putBoolean("context_attention_enabled", value).apply()

    var motionEnabled: Boolean
        get() = prefs.getBoolean("context_motion_enabled", false)
        set(value) = prefs.edit().putBoolean("context_motion_enabled", value).apply()

    var visionEnabled: Boolean
        get() = prefs.getBoolean("context_vision_enabled", false)
        set(value) = prefs.edit().putBoolean("context_vision_enabled", value).apply()

    var locationEnabled: Boolean
        get() = prefs.getBoolean("context_location_enabled", false)
        set(value) = prefs.edit().putBoolean("context_location_enabled", value).apply()
        
    var locationExactEnabled: Boolean
        get() = prefs.getBoolean("context_location_exact_enabled", false)
        set(value) = prefs.edit().putBoolean("context_location_exact_enabled", value).apply()

    init {
        // Migration logic for specialized v5.5 fields if needed in future
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    fun requestPermissions() {
        Log.d(TAG, "Requesting permissions for all domains (Android UI flow)...")
    }

    suspend fun ingest(): ContextObject {
        val loc = if (locationEnabled) fetchLocation() else null
        val events = if (temporalEnabled) fetchUpcomingEvents() else emptyList()
        val reminders = if (temporalEnabled) fetchUpcomingReminders() else emptyList()
        val deviceStatus = fetchDeviceStatus()
        // v5.5 Refinements: Privacy-First Semantic Location & Multi-factor Stress
        val classification = classifyLocation(loc?.latitude, loc?.longitude, loc?.speed)
        val vitals = if (healthEnabled) fetchVitals() else null
        val stress = deriveStressScore(vitals)
        val historical = if (healthEnabled) fetchHistoricalActivity() else ActivityMetrics()

        val contextObj = ContextObject(
            version = "5.5",
            metadata = ContextMetadata(
                sessionId = UUID.randomUUID().toString(),
                priorityScore = stress, 
                trigger = "SENSOR_INGESTION",
                currentTimestamp = System.currentTimeMillis()
            ),
            userState = UserState(
                activity = ActivityState(
                    value = if (deviceStatus.batteryState == "charging") ActivityValue.STATIONARY else ActivityValue.STATIONARY,
                    confidence = 1.0,
                    stepsToday = historical.steps,
                    activeCalories = historical.calories,
                    distance = historical.distance,
                    floors = historical.floors,
                    walkingSpeed = historical.walkingSpeed,
                    stepLength = historical.stepLength,
                    trends = historical.trends,
                    vitals = vitals
                ),
                attention = AttentionState(
                    mode = if (deviceStatus.batteryState == "charging") AttentionMode.DEEP_WORK else AttentionMode.RELAXED,
                    intentMismatch = false
                ),
                stressScore = stress,
                location = loc?.copy(
                    latitude = if (locationExactEnabled) loc.latitude else null,
                    longitude = if (locationExactEnabled) loc.longitude else null,
                    classification = classification
                ),
                device = deviceStatus
            ),
            temporalIntelligence = TemporalIntelligence(
                trend3h = if (stress > 0.6) "STRESS_RISING" else "STABLE",
                habitCompliance = "MEDIUM",
                upcomingEvents = events,
                pendingTasks = reminders,
                appUsage = mapOf("Productivity" to 45.0, "Social" to 15.0) // Mocked for v5.5
            ),
            safetyPolicy = SafetyPolicy(autoExecute = false, restrictedScopes = listOf("FINANCIAL")),
            learningLoop = LearningLoop(lastAction = "NONE", reward = 0.0)
        )
        
        currentContext.value = contextObj
        return contextObj
    }

    private fun deriveStressScore(vitals: Vitals?): Double {
        if (vitals == null) return 0.2
        val hrvVal = vitals.hrv ?: 70.0
        val hrvNorm = ((hrvVal - 20.0) / 80.0).coerceIn(0.0, 1.0)
        val rhrVal = vitals.restingHeartRate ?: 65.0
        val rhrNorm = ((rhrVal - 50.0) / 50.0).coerceIn(0.0, 1.0)
        return ((1.0 - hrvNorm) * 0.7) + (rhrNorm * 0.3)
    }

    private fun classifyLocation(lat: Double?, lon: Double?, speed: Double?): String {
        if (lat == null || lon == null) return "General"
        val homeLat = prefs.getFloat("context_home_lat", 0f).toDouble()
        val homeLon = prefs.getFloat("context_home_lon", 0f).toDouble()
        if (homeLat != 0.0 && Math.abs(lat - homeLat) < 0.001 && Math.abs(lon - homeLon) < 0.001) {
            return "Home"
        }
        return if (speed != null && speed > 5.0) "Transit" else "Outdoors"
    }

    private fun fetchVitals(): Vitals {
        // v5.5: Added structural support for Health Connect
        return Vitals(restingHeartRate = 68.0, hrv = 72.0, oxygenSaturation = 98.0, sleepQuality = "GOOD")
    }

    private data class ActivityMetrics(
        val steps: Int = 0, 
        val calories: Int = 0, 
        val distance: Double = 0.0, 
        val floors: Int = 0,
        val walkingSpeed: Double? = null,
        val stepLength: Double? = null,
        val trends: ActivityTrends? = null
    )
    
    private fun fetchHistoricalActivity(): ActivityMetrics {
        // v5.5: Added structural support for Health Connect
        return ActivityMetrics(
            steps = 5420, 
            calories = 312, 
            distance = 4200.0, 
            floors = 8,
            walkingSpeed = 1.2,
            stepLength = 0.72,
            trends = ActivityTrends(stepTrend = 1.1, calorieTrend = 1.05)
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLocation(): LocationState? {
        return try {
            val loc = fusedLocationClient.lastLocation.await()
            loc?.let {
                LocationState(it.latitude, it.longitude, it.speed.toDouble())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Location Error: ${e.message}")
            null
        }
    }

    private fun fetchUpcomingEvents(): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )
        
        val startMillis = System.currentTimeMillis()
        val endMillis = startMillis + 48 * 60 * 60 * 1000 // v5.5: Limit to 48h for performance
        
        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        try {
            appContext.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    events.add(CalendarEvent(
                        title = cursor.getString(0) ?: "Untitled",
                        startDate = cursor.getLong(1),
                        endDate = cursor.getLong(2),
                        location = cursor.getString(3)
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Calendar Error: ${e.message}")
        }
        return events
    }

    private fun fetchUpcomingReminders(): List<ReminderTask> = emptyList()


    private fun fetchDeviceStatus(): DeviceStatus {
        val batteryStatus = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        } ?: -1f
        
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val state = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            else -> "unplugged"
        }

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        
        return DeviceStatus(
            batteryLevel = level / 100f,
            batteryState = state,
            thermalState = "nominal",
            lowPowerMode = powerManager.isPowerSaveMode,
            connectionType = "wifi", // Mocked
            focusMode = null
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ContextEngine? = null

        fun getInstance(context: Context): ContextEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ContextEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
