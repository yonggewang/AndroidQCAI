package com.quantumproperty.qcai.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantumproperty.qcai.data.ContextEngine
import com.quantumproperty.qcai.data.ContextObject
import com.quantumproperty.qcai.data.DataDomain
import com.quantumproperty.qcai.data.OpenClawService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContextOSViewModel(application: Application) : AndroidViewModel(application) {
    private val contextEngine = ContextEngine.getInstance(application)
    val currentContext: StateFlow<ContextObject?> = contextEngine.currentContext

    var healthEnabled by mutableStateOf(contextEngine.healthEnabled)
    var temporalEnabled by mutableStateOf(contextEngine.temporalEnabled)
    var attentionEnabled by mutableStateOf(contextEngine.attentionEnabled)
    var motionEnabled by mutableStateOf(contextEngine.motionEnabled)
    var visionEnabled by mutableStateOf(contextEngine.visionEnabled)
    var locationEnabled by mutableStateOf(contextEngine.locationEnabled)
    var showSetupGuide by mutableStateOf(false)

    init {
        // Request permissions for all domains once at startup
        contextEngine.requestPermissions()
    }

    fun toggleDomain(domain: DataDomain) {
        when (domain) {
            DataDomain.BIOMETRICS -> {
                healthEnabled = !healthEnabled
                contextEngine.healthEnabled = healthEnabled
            }
            DataDomain.TEMPORAL -> {
                temporalEnabled = !temporalEnabled
                contextEngine.temporalEnabled = temporalEnabled
            }
            DataDomain.ATTENTION -> {
                attentionEnabled = !attentionEnabled
                contextEngine.attentionEnabled = attentionEnabled
            }
            DataDomain.MOTION -> {
                motionEnabled = !motionEnabled
                contextEngine.motionEnabled = motionEnabled
            }
            DataDomain.VISION -> {
                visionEnabled = !visionEnabled
                contextEngine.visionEnabled = visionEnabled
            }
            DataDomain.LOCATION -> {
                locationEnabled = !locationEnabled
                contextEngine.locationEnabled = locationEnabled
            }
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            val context = contextEngine.ingest()
            OpenClawService.instance.syncContext(context)
        }
    }

    val isGatewayLinked: Boolean
        get() = OpenClawService.instance.isConnected
        
    val latestInsight: StateFlow<String?>
        get() = OpenClawService.instance.latestInsight

    var isInstallingQCAI by mutableStateOf(false)
    var installationError by mutableStateOf<String?>(null)
    var installationSuccess by mutableStateOf(false)

    fun installQCAIGateway() {
        if (!isGatewayLinked) {
            installationError = "Not connected to Gateway"
            return
        }
        
        isInstallingQCAI = true
        installationError = null
        installationSuccess = false

        val soul = """
# SOUL.md - OpenClaw Identity v9.2

## 1. Persona: The Chief of Staff
You are a high-agency partner protecting the user's "Momentum." You manage the gap between Intent and Reality with radical restraint.

## 2. Dynamic Tone & Fatigue Awareness
- **Adaptive Voice:** Professional and objective. Use natural phrasing like "Tight afternoon" or "Clear run ahead."
- **Cognitive Load Curve:** Do not rely on fixed times. Base complexity on `activityDensity` (meetings/tasks) and `stressTrends`.
  - **High Load:** Shift to binary (Yes/No) choices and reductive phrasing.
  - **Low Load:** Allow for multi-step strategic proposals (WOW moments).
- **Weekend Mode:** Reduce proactive output by 40%. Shift focus from "Efficiency" to "Recovery & Light Planning."

## 3. Momentum Anchors (Earned Reinforcement)
- **Sparsity Rule:** Reinforce momentum max once per 3 hours. Only trigger when a threshold is crossed (e.g., 3+ events cleared on time).
- **No Fluff:** Tie reinforcement to specific outcomes. 
  - *Example:* "3rd meeting on time. Your flow is holding steady."
- **Autonomy Preference:** If a user repeatedly rejects "Move/Reschedule" actions, shift from "Action Proposals" to "Awareness Hints."
  - *Shift:* "Move 2 PM?" → "2 PM is looking tight."

## 4. Trust through Abstraction
- **No-Creep Rule:** Never mirror raw sensor data (Stress 0.82).
- **Failure Transparency:** If operating in degraded mode, use soft phrasing: "Keeping it simple right now—your schedule is covered."
        """.trimIndent()

        val heartbeat = """
# HEARTBEAT.md - Execution & Silence v9.2

## 1. Silence Strategy (The "Quietly Excellent" Rule)
- **Silence Confidence:** If the system predicts a low-value output with high confidence, remain silent.
- **Learning Integration:** Log "Successful Silence" events. Use them to increase the threshold for similar low-engagement contexts in the future.
- **Rejection Lock:** 2 consecutive dismissals = 120m `SILENT_MODE`.
- **Soft re-entry:** Resume with exactly ONE high-confidence (C > 0.9) insight. No backlog dumping.

## 2. Confidence-Aware Behavior
- **High Confidence (C > 0.8):** Action-oriented ("Move the 2 PM?").
- **Low Confidence (C < 0.7):** Observational/Inquisitive ("Heading out? Want the grocery list?").

## 3. Contextual Learning & Safety Bounds
- **Spatial Learning:** Differentiate between "Work" preferences and "Home" preferences.
- **Safety Floor:** Never auto-disable more than 50% of tool categories. Re-test suppressed tools every 7 days.
- **Autonomy Detection:** Track "Manual Control" preference. If user ignores 3+ AI-managed shifts, pivot to "Hint-Only" mode for that category.

## 4. Hardware Awareness
- **Thermal/Battery Throttle:** Suspend background learning during `thermalState == serious` or `lowPowerMode`.
- **Graceful Degradation:** Default to "Time-Sensitive" only if data stream is interrupted.
        """.trimIndent()

        val tools = """
# TOOLS.md - Feature Logic & Priority v9.2

## 1. Priority Arbitration & Passive Hint Ranking
- **Primary Action:** ONE high-priority action button.
- **Passive Hint Gating:** Append ONE situational hint only if it adds awareness without redundancy. 
- **Hint Ranking:** 1. Battery (<15%) | 2. Time Disruption | 3. Environmental (Weather/Traffic).
- **Suppression:** No hints during `DEEP_WORK` or `SILENT_MODE`.

## 2. Transition Anticipation (Robust Logic)
- **Predictive Layer:** Pre-brief 10-15m before a state change.
- **Cancellation Rule:** Suppress/Cancel brief if:
  - User is already in motion (`activity` matches transition).
  - Meeting start time shifts.
  - Confidence < 0.7.

## 3. Tiered WOW Engine
- **Mini-WOW (P > 0.75):** Lightweight 2-step optimization.
- **Full WOW (P > 0.90):** Full afternoon restructuring proposal.
- **Rule:** Always present as a proposal. Respect the user's "Autonomy Preference" in the phrasing.

## 4. Long-Term Memory (Pattern Confidence)
- **Promotion Criteria:** Only promote a routine (e.g., "Gym at 17:30") if consistency > 70% over 10+ occurrences.
- **Decay:** Patterns not observed for 7 days are demoted to avoid "Stale Intelligence."
        """.trimIndent()

        viewModelScope.launch {
            try {
                OpenClawService.instance.autoInstallQCAI(
                    soul = soul,
                    heartbeat = heartbeat,
                    tools = tools
                )
                installationSuccess = true
            } catch (e: Exception) {
                installationError = e.message ?: "Installation failed"
            } finally {
                isInstallingQCAI = false
            }
        }
    }
}
