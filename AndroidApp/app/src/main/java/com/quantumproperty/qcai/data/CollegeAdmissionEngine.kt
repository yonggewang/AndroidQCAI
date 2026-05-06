package com.quantumproperty.qcai.data

class CollegeAdmissionEngine private constructor() {
    companion object {
        val shared = CollegeAdmissionEngine()
    }

    fun analyze(student: StudentProfile, university: University): AdmissionResult {
        // 1. Residency Selection
        val isSameState = student.state.trim().uppercase() == university.state.trim().uppercase()
        
        // 2. Base Score from Selectivity Tier
        var internalScore = 0
        when (university.selectivityTier) {
            SelectivityTier.ELITE -> internalScore = -3
            SelectivityTier.HIGH -> internalScore = -1
            SelectivityTier.MEDIUM -> internalScore = 0
            SelectivityTier.LOW -> internalScore = 1
        }
        
        // 3. Hybrid Academic Matching (v1.7 Logic)
        var strategy = "Test-Centric"
        
        // Check if we HAVE test data to compare
        val hasUnivTestStats = university.scores.sat25 != null || university.scores.act25 != null
        val hasStudentTestStats = student.sat != null || student.act != null
        
        if (university.policy.testPolicy == AdmissionPolicy.TestPolicy.BLIND || !hasUnivTestStats || !hasStudentTestStats) {
            // PIVOT TO GPA-CENTRIC MODE
            strategy = "GPA-Centric"
            
            val uGpa25 = university.academics.gpa25 ?: (if (university.academics.gpaAvg != null) university.academics.gpaAvg - 0.2 else null)
            val uGpa75 = university.academics.gpa75 ?: (if (university.academics.gpaAvg != null) university.academics.gpaAvg + 0.1 else null)
            
            if (uGpa25 != null && uGpa75 != null) {
                if (student.gpa < uGpa25) { internalScore -= 3 }
                else if (student.gpa >= uGpa75) { internalScore += 2 }
            } else {
                // Deep Fallback: Just compare to a selectivity-based GPA baseline
                val baseline = estimateGPABaseline(university.selectivityTier)
                if (student.gpa < baseline) { internalScore -= 2 }
                else if (student.gpa > baseline + 0.2) { internalScore += 1 }
            }
        } else {
            // TRADITIONAL TEST-CENTRIC MODE
            if (student.sat != null && university.scores.sat25 != null && university.scores.sat75 != null) {
                if (student.sat!! < university.scores.sat25) { internalScore -= 2 }
                else if (student.sat!! > university.scores.sat75) { internalScore += 2 }
            } else if (student.act != null && university.scores.act25 != null && university.scores.act75 != null) {
                if (student.act!! < university.scores.act25) { internalScore -= 2 }
                else if (student.act!! > university.scores.act75) { internalScore += 2 }
            }
            
            // GPA still adds some weight
            if (university.academics.gpa25 != null && university.academics.gpa75 != null) {
                if (student.gpa < university.academics.gpa25) { internalScore -= 1 }
                else if (student.gpa >= university.academics.gpa75) { internalScore += 1 }
            }
        }
        
        // 4. Major-Specific Difficulty Check (v2.0)
        var isHighlyImpactedMajor = false
        if (university.meta.admitByMajor == true && student.intendedMajor != null) {
            val difficultyMap = university.academics.majorDifficulty
            val level = difficultyMap?.get(student.intendedMajor)
            if (level != null) {
                // Penalty = Difficulty Level (1-5) * 0.6
                // e.g., Level 5 (CS) = 3.0 penalty (Safety -> Reach)
                val majorPenalty = level * 0.6
                if (level >= 4) { isHighlyImpactedMajor = true }
                
                internalScore -= Math.round(majorPenalty).toInt()
            }
        }
        
        // 5. Final Score mapping to Category
        var category = AdmissionCategory.REACH
        if (internalScore >= 2) {
            category = AdmissionCategory.SAFETY
        } else if (internalScore >= 0) {
            category = AdmissionCategory.MATCH
        }
        
        // 5. Rank-Based Reality Check (v1.9 — uses student's self-reported rank)
        var rankWarning = false
        val top10Stat = university.academics.rankTop10Percent
        if (top10Stat != null && top10Stat > 0.85) {
            // School takes >85% from Top 10%. Check if student qualifies.
            val studentMeetsRank: Boolean = when {
                student.classRank != null && student.classRank != ClassRankPercentile.NOT_SURE -> {
                    student.classRank!!.threshold <= 0.10 // TOP_5 or TOP_10
                }
                else -> false // No rank provided — assume conservative
            }

            if (!studentMeetsRank && (category == AdmissionCategory.MATCH || category == AdmissionCategory.SAFETY)) {
                rankWarning = true
            }
        } else {
            val top25Stat = university.academics.rankTop25Percent
            if (top25Stat != null && top25Stat > 0.85) {
                // School takes >85% from Top 25%. Less strict check.
                val studentMeetsRank: Boolean = when {
                    student.classRank != null && student.classRank != ClassRankPercentile.NOT_SURE -> {
                        student.classRank!!.threshold <= 0.25
                    }
                    else -> false
                }

                if (!studentMeetsRank && (category == AdmissionCategory.MATCH || category == AdmissionCategory.SAFETY)) {
                    rankWarning = true
                }
            }
        }
        
        val finalCategory = if (rankWarning) AdmissionCategory.REACH else category
        
        // 6. Tone & Explanation (Enhanced v1.8)
        val reason = generateExplanation(
            student = student,
            university = university,
            isSameState = isSameState,
            rankWarning = rankWarning,
            isHighlyImpactedMajor = isHighlyImpactedMajor,
            strategy = strategy
        )
        
        val tuitionEstimate = if (isSameState) university.cost.tuitionInState else university.cost.tuitionOutState
        
        return AdmissionResult(
            category = finalCategory,
            confidence = university.admissions.dataQuality,
            reason = reason,
            tuitionEstimate = tuitionEstimate,
            residencyApplied = isSameState,
            matchStrategy = strategy
        )
    }

    private fun estimateGPABaseline(tier: SelectivityTier): Double {
        return when (tier) {
            SelectivityTier.ELITE -> 3.9
            SelectivityTier.HIGH -> 3.7
            SelectivityTier.MEDIUM -> 3.5
            SelectivityTier.LOW -> 3.0
        }
    }

    private fun generateExplanation(
        student: StudentProfile,
        university: University,
        isSameState: Boolean,
        rankWarning: Boolean,
        isHighlyImpactedMajor: Boolean,
        strategy: String
    ): String {
        val parts = mutableListOf<String>()
        
        if (strategy == "GPA-Centric") {
            parts.add("Note: Classification based primarily on your GPA and institutional selectivity tiers.")
        }
        
        if (isHighlyImpactedMajor) {
            val major = student.intendedMajor ?: "the selected major"
            parts.add("Note: $major is a highly impacted program here with significantly higher standards than the general admission rate.")
        }

        if (university.selectivityTier == SelectivityTier.ELITE) {
            parts.add("This is an elite institution with an extremely competitive applicant pool.")
        }

        if (rankWarning) {
            parts.add("Warning: This school admits the vast majority of students from the top 10% of their class. Given your profile, this remains a High Reach.")
        }

        if (university.academics.gpa75 != null && student.gpa >= university.academics.gpa75) {
            parts.add("Your academic record is exceptional compared to recent admits.")
        }

        if (university.policy.testPolicy == AdmissionPolicy.TestPolicy.BLIND) {
            parts.add("This school follows a test-blind policy.")
        }

        if (isSameState && university.type == SchoolType.PUBLIC) {
            parts.add("In-state residency provides a distinct advantage here.")
        }

        return parts.joinToString(" ")
    }
}
