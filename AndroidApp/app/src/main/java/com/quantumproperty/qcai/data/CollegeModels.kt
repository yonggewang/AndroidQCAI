package com.quantumproperty.qcai.data

import com.google.gson.annotations.SerializedName

enum class SchoolType(val value: String) {
    @SerializedName("public") PUBLIC("public"),
    @SerializedName("private") PRIVATE("private")
}

enum class SelectivityTier(val value: String) {
    @SerializedName("elite") ELITE("elite"),
    @SerializedName("high") HIGH("high"),
    @SerializedName("medium") MEDIUM("medium"),
    @SerializedName("low") LOW("low")
}

data class AdmissionsData(
    @SerializedName("acceptance_rate_overall") val acceptanceRateOverall: Double,
    @SerializedName("acceptance_rate_in_state") val acceptanceRateInState: Double?,
    @SerializedName("acceptance_rate_out_state") val acceptanceRateOutState: Double?,
    @SerializedName("acceptance_rate_source") val acceptanceRateSource: String?,
    @SerializedName("applicants") val applicants: Int?,
    @SerializedName("admitted") val admitted: Int?,
    @SerializedName("data_quality") val dataQuality: DataQuality,
    @SerializedName("source_url") val sourceUrl: String?,
    @SerializedName("score_source") val scoreSource: ScoreSource
) {
    enum class DataQuality {
        @SerializedName("high") HIGH,
        @SerializedName("medium") MEDIUM,
        @SerializedName("low") LOW,
        @SerializedName("estimated") ESTIMATED
    }

    enum class ScoreSource {
        @SerializedName("cds") CDS,
        @SerializedName("ipeds") IPEDS,
        @SerializedName("estimated") ESTIMATED,
        @SerializedName("nces") NCES,
        @SerializedName("null") NULL_SOURCE
    }
}

data class ScoreData(
    @SerializedName("sat_25") val sat25: Int?,
    @SerializedName("sat_75") val sat75: Int?,
    @SerializedName("act_25") val act25: Int?,
    @SerializedName("act_75") val act75: Int?
)

data class AcademicData(
    @SerializedName("gpa_25") val gpa25: Double?,
    @SerializedName("gpa_75") val gpa75: Double?,
    @SerializedName("gpa_avg") val gpaAvg: Double?,
    @SerializedName("rank_top_10_percent") val rankTop10Percent: Double?,
    @SerializedName("rank_top_25_percent") val rankTop25Percent: Double?,
    @SerializedName("national_ranking") val nationalRanking: Int?,
    @SerializedName("major_difficulty") val majorDifficulty: Map<String, Int>?
)

data class TuitionData(
    @SerializedName("tuition_in_state") val tuitionInState: Int?,
    @SerializedName("tuition_out_state") val tuitionOutState: Int?
)

data class AdmissionPolicy(
    @SerializedName("test_policy") val testPolicy: TestPolicy
) {
    enum class TestPolicy {
        @SerializedName("required") REQUIRED,
        @SerializedName("optional") OPTIONAL,
        @SerializedName("blind") BLIND
    }
}

data class UniversityMeta(
    @SerializedName("city") val city: String,
    @SerializedName("enrollment") val enrollment: Int?,
    @SerializedName("reporting_year") val reportingYear: Int?,
    @SerializedName("admit_by_major") val admitByMajor: Boolean?
)

data class University(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("state") val state: String,
    @SerializedName("type") val type: SchoolType,
    @SerializedName("admissions") val admissions: AdmissionsData,
    @SerializedName("scores") val scores: ScoreData,
    @SerializedName("academics") val academics: AcademicData,
    @SerializedName("cost") val cost: TuitionData,
    @SerializedName("policy") val policy: AdmissionPolicy,
    @SerializedName("meta") val meta: UniversityMeta,
    @SerializedName("selectivity_tier") val selectivityTier: SelectivityTier
)

// Application level models

enum class ClassRankPercentile(val label: String, val threshold: Double) {
    TOP_5("Top 5%", 0.05),
    TOP_10("Top 10%", 0.10),
    TOP_25("Top 25%", 0.25),
    TOP_50("Top 50%", 0.50),
    NOT_SURE("Not Sure", 1.0)
}

object StandardMajors {
    val all = listOf(
        "Undecided",
        "Accounting",
        "Biology",
        "Business Administration",
        "Chemistry",
        "Communications",
        "Computer Science",
        "Criminal Justice",
        "Data Science",
        "Economics",
        "Education",
        "Electrical Engineering",
        "English / Literature",
        "Environmental Science",
        "Finance",
        "Graphic Design",
        "Health Sciences",
        "History",
        "Information Technology",
        "International Relations",
        "Marketing",
        "Mathematics",
        "Mechanical Engineering",
        "Nursing",
        "Philosophy",
        "Physics",
        "Political Science",
        "Pre-Med",
        "Psychology",
        "Sociology",
        "Other"
    )
}

enum class CollegeSortOption(val label: String) {
    CATEGORY("Category"),
    NATIONAL_RANK("National Rank"),
    TUITION_LOW("Tuition: Low → High"),
    TUITION_HIGH("Tuition: High → Low"),
    ACCEPTANCE_HIGH("Acceptance: High → Low"),
    ACCEPTANCE_LOW("Most Selective"),
    SELECTIVITY("Selectivity Tier"),
    ALPHABETICAL("A → Z")
}

data class StudentProfile(
    var gpa: Double = 0.0,
    var sat: Int? = null,
    var act: Int? = null,
    var state: String = "",
    var intendedMajor: String? = null,
    var classRank: ClassRankPercentile? = null
)

enum class AdmissionCategory(val label: String) {
    REACH("Reach"),
    MATCH("Match"),
    SAFETY("Safety")
}

data class AdmissionResult(
    val category: AdmissionCategory,
    val confidence: AdmissionsData.DataQuality,
    val reason: String,
    val tuitionEstimate: Int?,
    val residencyApplied: Boolean,
    val matchStrategy: String
)
