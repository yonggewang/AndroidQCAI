package com.quantumproperty.qcai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.CookieJar
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder

// Data classes for parsing responses
data class Candidate(
    val address: String,
    val location: Location,
    val attributes: Map<String, String>?
)

data class Location(
    val x: Double,
    val y: Double
)

data class OwnerSearchResult(
    val owner: String,
    val address: String,
    val pid: String
)

data class PropertyAttributes(
    var pid: String = "",
    var ownerName: String = "None Found",
    var lastSalePrice: Double = 0.0,
    var lastSaleDate: String = "N/A",
    var deedBook: String = "N/A",
    var deedPage: String = "N/A",
    var assessedValue: Double = 0.0,
    var landValue: Double = 0.0,
    var buildingValue: Double = 0.0,
    var taxAmount: Double = 0.0,
    var yearBuilt: Int = 0,
    var squareFeet: Int = 0,
    var bedrooms: Int? = null,
    var bathrooms: Double? = null,
    var zoning: String = "N/A"
)

// External owner footprint lookup services

object OpenAlexAPI {
    private val client = OkHttpClient()
    
    suspend fun searchScholarProfiles(name: String): String = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.lowercase() == "none" || cleanName.lowercase() == "n/a" || cleanName.lowercase() == "none found") {
            return@withContext "No public footprint profiles located"
        }
        
        try {
            val encoded = URLEncoder.encode(cleanName, "UTF-8")
            val url = "https://api.openalex.org/authors?search=$encoded"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext "No public footprint profiles located"
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results") ?: return@withContext "No public footprint profiles located"
                    if (results.length() > 0) {
                        val sb = java.lang.StringBuilder()
                        val limit = minOf(results.length(), 3)
                        for (i in 0 until limit) {
                            val r = results.getJSONObject(i)
                            val displayName = r.optString("display_name", cleanName)
                            val worksCount = r.optInt("works_count", 0)
                            val citedByCount = r.optInt("cited_by_count", 0)
                            var instName = "Unknown Institution"
                            val insts = r.optJSONArray("last_known_institutions")
                            if (insts != null && insts.length() > 0) {
                                instName = insts.getJSONObject(0).optString("display_name", "Unknown Institution")
                            }
                            sb.append("- $displayName affiliated with $instName (Works: $worksCount, Citations: $citedByCount)\n")
                        }
                        return@withContext sb.toString().trim()
                    }
                }
            }
        } catch (e: Exception) {
            println("OpenAlex API Error: ${e.message}")
        }
        return@withContext "No public footprint profiles located"
    }
}

object JudyRecordsAPI {
    private val client = OkHttpClient()
    
    suspend fun searchCivilRecords(name: String): String = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.lowercase() == "none" || cleanName.lowercase() == "n/a" || cleanName.lowercase() == "none found") {
            return@withContext "No public court records located"
        }
        
        try {
            val encoded = URLEncoder.encode(cleanName, "UTF-8")
            val url = "https://api.judyrecords.com/v1/records?q=$encoded"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext "No public court records located"
                    val json = JSONObject(body)
                    val records = json.optJSONArray("records") ?: return@withContext "No public court records located"
                    if (records.length() > 0) {
                        val sb = java.lang.StringBuilder()
                        val limit = minOf(records.length(), 3)
                        for (i in 0 until limit) {
                            val r = records.getJSONObject(i)
                            val caseTitle = r.optString("title", "Civil Case")
                            val court = r.optString("court", "State Court")
                            val date = r.optString("date", "")
                            sb.append("- $caseTitle in $court ($date)\n")
                        }
                        return@withContext sb.toString().trim()
                    }
                }
            }
        } catch (e: Exception) {
            println("JudyRecords API Error: ${e.message}")
        }
        return@withContext "No public court records located"
    }
}

// Result struct for property data fetching
data class PropertyDataResult(
    val formattedData: String,
    val hasOfficialGISData: Boolean,   // True if Mecklenburg County parcel data was found
    val coordinates: Pair<Double, Double>?, // (x/lon, y/lat)
    val promptPayload: String?         // The custom structured prompt for Gemini
)

class InMemoryCookieJar : okhttp3.CookieJar {
    private val cookieStore = mutableMapOf<String, List<okhttp3.Cookie>>()

    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
        cookieStore[url.host] = cookies
    }

    override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }
}

class PropertyDataService {
    private val client = okhttp3.OkHttpClient.Builder()
        .cookieJar(InMemoryCookieJar())
        .build()
    private val spatialReference = 4326

    private fun splitOwnerNames(ownerName: String): List<String> {
        val clean = ownerName.trim()
        if (clean.isEmpty() || clean.lowercase() == "none" || clean.lowercase() == "n/a" || clean.lowercase() == "none found") {
            return emptyList()
        }
        val normalized = clean.replace("(?i)\\s+&\\s+".toRegex(), " and ")
        val parts = normalized.split("(?i)\\s+and\\s+".toRegex())
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun fetchPropertyData(address: String): PropertyDataResult = withContext(Dispatchers.IO) {
        // Ensure address has city context for geocoding
        val addrLower = address.lowercase()
        val hasCityOrNC = addrLower.contains(", nc") || addrLower.contains("nc ") || addrLower.contains("north carolina") ||
           addrLower.contains("charlotte") || addrLower.contains("matthews") || addrLower.contains("mint hill") ||
           addrLower.contains("huntersville") || addrLower.contains("cornelius") || addrLower.contains("davidson") ||
           addrLower.contains("pineville") || addrLower.contains("mooresville") ||
           addrLower.contains("concord") || addrLower.contains("kannapolis") || addrLower.contains("harrisburg") ||
           addrLower.contains("monroe") || addrLower.contains("waxhaw") || addrLower.contains("indian trail")
           
        val normalizedAddress = if (hasCityOrNC) address else "$address, Charlotte, NC"
        
        // Strategy 1: Try US Census Geocoder first for Address Standardization
        var candidate = geocodeAddressCensus(normalizedAddress)
        var geocodeSource = "US Census Geocoder"

        // Strategy 2: Try Mecklenburg County GIS geocoder as fallback
        if (candidate == null) {
            println("PropertyDataService: Census geocode failed, trying Mecklenburg GIS...")
            candidate = geocodeAddressGIS(normalizedAddress)
            geocodeSource = "Mecklenburg County GIS"
        }

        // Strategy 3: Try Nominatim (OpenStreetMap) as final fallback
        if (candidate == null) {
            println("PropertyDataService: Mecklenburg GIS geocode failed, trying Nominatim fallback...")
            candidate = geocodeAddressNominatim(normalizedAddress)
            geocodeSource = "OpenStreetMap/Nominatim"
        }

        // If all geocoding strategies fail, return descriptive error
        if (candidate == null) {
            return@withContext PropertyDataResult(
                formattedData = "Could not geocode the address \"$address\" using any available geocoding service (Census, Mecklenburg GIS, OpenStreetMap).",
                hasOfficialGISData = false,
                coordinates = null,
                promptPayload = null
            )
        }

        val x = candidate.location.x
        val y = candidate.location.y

        val resolvedCounty = identifyCounty(candidate.address)
        
        var output = ""
        var hasOfficialGISData = false
        var ownerNameForGrounding = ""
        var parcelIdForGrounding = ""
        var assessedValueForGrounding = ""
        var taxAmountForGrounding = ""

        if (resolvedCounty == "Union") {
            val unionParcel = fetchUnionParcelInfo(x, y)
            if (unionParcel != null) {
                hasOfficialGISData = true
                val parcelNo = unionParcel.optString("parcel_number", "")
                val rawAddr = unionParcel.optString("Address", candidate.address)
                
                var assessedVal = 0.0
                var landVal = 0.0
                var bldgVal = 0.0
                
                if (parcelNo.isNotEmpty()) {
                    val unionAssessed = fetchUnionAssessedValue(parcelNo)
                    if (unionAssessed != null) {
                        assessedVal = unionAssessed.optDouble("market_total", 0.0)
                        landVal = unionAssessed.optDouble("market_land", 0.0)
                        bldgVal = unionAssessed.optDouble("market_building", 0.0)
                    }
                }
                
                val taxEst = assessedVal * 0.01
                val taxAmountString = String.format(java.util.Locale.US, "$%.2f", taxEst)
                
                val sb = StringBuilder()
                sb.append("### 📋 Union County Property Registry\n")
                sb.append("*   **Owner(s)**: Restricted in Union County public GIS (Search Grounding recommended)\n")
                sb.append("*   **Street Address**: $rawAddr\n")
                sb.append("*   **Parcel ID (PIN)**: $parcelNo\n")
                if (assessedVal > 0) {
                    val landStr = if (landVal > 0) String.format(java.util.Locale.US, "$%.2f", landVal) else "N/A"
                    val bldgStr = if (bldgVal > 0) String.format(java.util.Locale.US, "$%.2f", bldgVal) else "N/A"
                    sb.append("*   **Assessed Value**: ${String.format(java.util.Locale.US, "$%.2f", assessedVal)} (Land: $landStr, Building: $bldgStr)\n")
                    sb.append("*   **Estimated Annual Tax**: $taxAmountString\n")
                }
                sb.append("\n*Note: Owner names are restricted in Union County's public GIS. Ask Gemini to search for the owner using Google search grounding.*\n")
                
                output = sb.toString()
                ownerNameForGrounding = "Restricted in Union County public GIS"
                parcelIdForGrounding = parcelNo
                assessedValueForGrounding = if (assessedVal > 0) String.format(java.util.Locale.US, "$%.2f", assessedVal) else "N/A"
                taxAmountForGrounding = taxAmountString
            } else {
                output = """
                    ### 📍 Geocoding Details (No Parcel Registry Found)
                    *   **Resolved Address**: ${candidate.address}
                    *   **Coordinates**: $x, $y
                    *   **Geocode Source**: $geocodeSource
                    
                    *Note: This property is outside the official Union County GIS parcel boundary map.*
                """.trimIndent()
            }
        } else if (resolvedCounty == "Cabarrus") {
            var objectIdVal: Int? = null
            var pinVal = ""
            val cabParcel = fetchCabarrusParcelInfo(x, y)
            if (cabParcel != null) {
                if (cabParcel.has("OBJECTID")) {
                    objectIdVal = cabParcel.optInt("OBJECTID")
                }
                if (cabParcel.has("PIN14")) {
                    pinVal = cabParcel.optString("PIN14")
                }
            }
            
            if (objectIdVal != null) {
                hasOfficialGISData = true
                var ownerName = "None Found"
                var marketVal = 0.0
                var landVal = 0.0
                var bldgVal = 0.0
                var salePrice = 0.0
                var saleYear = 0
                var nbhName = ""
                var acreage = 0.0
                
                val cabTax = fetchCabarrusTaxInfo(objectIdVal)
                if (cabTax != null) {
                    val acct1 = cabTax.optString("AcctName1", "")
                    val acct2 = cabTax.optString("AcctName2", "")
                    if (acct1.isNotEmpty()) {
                        ownerName = if (acct2.isEmpty()) acct1 else "$acct1 & $acct2"
                    }
                    marketVal = cabTax.optDouble("MarketValue", cabTax.optDouble("AssessedValue", 0.0))
                    landVal = cabTax.optDouble("LandValue", 0.0)
                    bldgVal = cabTax.optDouble("BuildingValue", 0.0)
                    salePrice = cabTax.optDouble("SalePrice", 0.0)
                    saleYear = cabTax.optInt("SaleYear", 0)
                    nbhName = cabTax.optString("NBH_NAME", "")
                    acreage = cabTax.optDouble("CALCULATED_ACREAGE", 0.0)
                }
                
                val taxEst = marketVal * 0.01
                val taxAmountString = String.format(java.util.Locale.US, "$%.2f", taxEst)
                
                val sb = StringBuilder()
                sb.append("### 📋 Cabarrus County Property Registry\n")
                sb.append("*   **Owner(s)**: $ownerName\n")
                sb.append("*   **Street Address**: ${candidate.address}\n")
                sb.append("*   **Parcel ID (PIN)**: $pinVal\n")
                if (salePrice > 0) {
                    val yearStr = if (saleYear > 0) " in $saleYear" else ""
                    sb.append("*   **Last Transaction**: ${String.format(java.util.Locale.US, "$%.2f", salePrice)}$yearStr\n")
                }
                if (marketVal > 0) {
                    val landStr = if (landVal > 0) String.format(java.util.Locale.US, "$%.2f", landVal) else "N/A"
                    val bldgStr = if (bldgVal > 0) String.format(java.util.Locale.US, "$%.2f", bldgVal) else "N/A"
                    sb.append("*   **Assessed Value**: ${String.format(java.util.Locale.US, "$%.2f", marketVal)} (Land: $landStr, Building: $bldgStr)\n")
                    sb.append("*   **Estimated Annual Tax**: $taxAmountString\n")
                }
                if (nbhName.isNotEmpty()) {
                    sb.append("*   **Neighborhood**: $nbhName\n")
                }
                if (acreage > 0) {
                    sb.append("*   **Acreage**: ${String.format(java.util.Locale.US, "%.3f", acreage)} acres\n")
                }
                
                output = sb.toString()
                ownerNameForGrounding = ownerName
                parcelIdForGrounding = pinVal
                assessedValueForGrounding = if (marketVal > 0) String.format(java.util.Locale.US, "$%.2f", marketVal) else "N/A"
                taxAmountForGrounding = taxAmountString
            } else {
                output = """
                    ### 📍 Geocoding Details (No Parcel Registry Found)
                    *   **Resolved Address**: ${candidate.address}
                    *   **Coordinates**: $x, $y
                    *   **Geocode Source**: $geocodeSource
                    
                    *Note: This property is outside the official Cabarrus County GIS parcel boundary map.*
                """.trimIndent()
            }
        } else {
            // Mecklenburg County (default)
            coroutineScope {
                val spatialestDeferred = async { fetchSpatialestData(candidate.address, x, y) }
                val schoolsDeferred = async { fetchSchoolZone(x, y) }

                val spatialestAttrs = spatialestDeferred.await()
                val schools = schoolsDeferred.await()

                var property = PropertyAttributes()
                var geocodeSourceString = geocodeSource

                if (spatialestAttrs != null) {
                    property = spatialestAttrs
                    hasOfficialGISData = true
                    geocodeSourceString = "Mecklenburg Property System (Spatialest)"
                } else {
                    // Fallback to ArcGIS parcels boundaries layer
                    val rawParcelAttrs = fetchParcelInfo(x, y)
                    if (rawParcelAttrs != null) {
                        property = parsePropertyAttributes(rawParcelAttrs)
                        hasOfficialGISData = property.pid.isNotEmpty() || property.ownerName != "None Found"
                    }
                }

                // Format values for prompt/output
                val salePriceString = if (property.lastSalePrice > 0) {
                    String.format(java.util.Locale.US, "$%.2f", property.lastSalePrice)
                } else {
                    "No historical transaction price recorded in county registry"
                }
                val taxAmountString = String.format(java.util.Locale.US, "$%.2f", property.taxAmount)

                val sb = StringBuilder()

                if (hasOfficialGISData) {
                    sb.append("### 📋 Mecklenburg County Property Registry\n")
                    sb.append("*   **Owner(s)**: ${property.ownerName}\n")
                    sb.append("*   **Street Address**: ${candidate.address}\n")
                    sb.append("*   **Parcel ID (PIN)**: ${property.pid}\n")
                    if (property.deedBook.isNotEmpty() && property.deedBook != "N/A") {
                        sb.append("*   **Deed Reference**: Book ${property.deedBook} / Page ${property.deedPage}\n")
                    }
                    sb.append("*   **Last Transaction**: $salePriceString on ${property.lastSaleDate}\n")
                    sb.append("*   **Calculated Annual Tax**: $taxAmountString\n")
                    if (property.assessedValue > 0) {
                        val landStr = if (property.landValue > 0) String.format(java.util.Locale.US, "$%.2f", property.landValue) else "N/A"
                        val bldgStr = if (property.buildingValue > 0) String.format(java.util.Locale.US, "$%.2f", property.buildingValue) else "N/A"
                        sb.append("*   **Assessed Value**: ${String.format(java.util.Locale.US, "$%.2f", property.assessedValue)} (Land: $landStr, Building: $bldgStr)\n")
                    }
                    if (property.yearBuilt > 0) sb.append("*   **Year Built**: ${property.yearBuilt}\n")
                    if (property.squareFeet > 0) sb.append("*   **Finished Area**: ${property.squareFeet} sq ft\n")

                    val layoutStr = if (property.bedrooms != null && property.bathrooms != null) {
                        "${property.bedrooms} Beds / ${property.bathrooms} Baths"
                    } else if (property.bedrooms != null) {
                        "${property.bedrooms} Beds"
                    } else if (property.bathrooms != null) {
                        "${property.bathrooms} Baths"
                    } else {
                        "N/A"
                    }
                    sb.append("*   **Layout**: $layoutStr\n")
                    if (property.zoning.isNotEmpty() && property.zoning != "N/A") sb.append("*   **Zoning/Land Use**: ${property.zoning}\n")

                    if (schools != null) {
                        sb.append("\n### 🏫 School Attendance Zones\n")
                        val sortedKeys = schools.keys.sorted()
                        for (key in sortedKeys) {
                            sb.append("*   **$key**: ${schools[key]}\n")
                        }
                    }
                } else {
                    sb.append("### 📍 Geocoding Details (No Parcel Registry Found)\n")
                    sb.append("*   **Resolved Address**: ${candidate.address}\n")
                    sb.append("*   **Coordinates**: $x, $y\n")
                    sb.append("*   **Geocode Source**: $geocodeSourceString\n")
                    sb.append("\n*Note: This property is outside the official Mecklenburg County GIS parcel boundary map.*\n")
                }
                
                output = sb.toString()
                ownerNameForGrounding = property.ownerName
                parcelIdForGrounding = property.pid
                assessedValueForGrounding = if (property.assessedValue > 0) String.format(java.util.Locale.US, "$%.2f", property.assessedValue) else "N/A"
                taxAmountForGrounding = taxAmountString
            }
        }

        // Build instructions for Gemini owner search grounding
        val ownerGroundingInstructions = if (resolvedCounty == "Union") {
            "- **Identify Owner & Background**: Since public GIS owner names are restricted in Union County, first perform a live web search to identify the owner(s) of the property at \"${candidate.address}\" (look up GIS records, property sales, or tax portals). Once identified, perform professional and social networking searches (e.g. LinkedIn, Facebook) to retrieve their background, professional roles, or public profiles."
        } else {
            val ownerNames = splitOwnerNames(ownerNameForGrounding)
            if (ownerNames.isEmpty()) {
                "- **Potential Professional & Social Footprints**: Search for the property owner(s) \"$ownerNameForGrounding\" in Charlotte or North Carolina to find potential background or footprints."
            } else {
                val sbOwners = StringBuilder("- **Potential Professional & Social Footprints**: For each of the following owners individually, perform a live web search using queries like \"[Owner Name], NC\" or \"[Owner Name]\" on professional and social networking sites (especially LinkedIn and Facebook) to retrieve better, highly relevant profiles and public footprints. Describe any found professional roles, academic background, or associations for each owner:\n")
                for (name in ownerNames) {
                    sbOwners.append("     * Owner: \"$name\"\n")
                }
                sbOwners.toString()
            }
        }

        // Build the prompt payload directing Gemini to focus on supplementary grounding search
        val promptPayload = """
            [SYSTEM INSTRUCTION]
            You are an advanced real estate compiler equipped with Google Search Grounding.
            
            CRITICAL OUTPUT REQUIREMENTS:
            1. The client application is ALREADY displaying the official property registry details (such as Parcel ID, owner names, values, deed books, tax, school attendance zones, etc.) directly to the user. DO NOT repeat or echo these raw registry details in your response, and DO NOT print headers like "Property Details" or list these duplicate metrics.
            2. Focus your response exclusively on supplementary web-grounded analysis:
               - **Neighborhood & Block-level Safety**: Search for block-level safety reviews, crime indexes, safety ratings, and hazard factors for "${candidate.address}" in $resolvedCounty County.
               - **Local Property Market & Trends**: Search for recent neighborhood market updates, value appreciation trends, or regional housing insights.
               - **Detailed School Performance**: Search for performance ratings, parent reviews, test score ranks, and academic insights for the schools near "${candidate.address}".
               $ownerGroundingInstructions
               Label the owner section clearly as "Potential Professional & Social Footprints (from live web lookup)".
            3. Never include citations (e.g. `[cite: ...]`) pointing to the local registry context block. If you mention the address, owner names, or schools, do so as normal text without any citation brackets or sources.
            
            === CONTEXT: INTEGRATED LOCAL APP REGISTRIES ===
            Target Physical Address: ${candidate.address}
            Owner Name(s): $ownerNameForGrounding
            Parcel Legal ID (PIN): $parcelIdForGrounding
            Assessed Value: $assessedValueForGrounding
            Calculated Tax: $taxAmountForGrounding
        """.trimIndent()

        PropertyDataResult(
            formattedData = output,
            hasOfficialGISData = hasOfficialGISData,
            coordinates = Pair(x, y),
            promptPayload = promptPayload
        )
    }

    suspend fun fetchPropertyDataString(address: String): String {
        return fetchPropertyData(address).formattedData
    }

    data class SpatialestResolution(val id: Int, val csrfToken: String)

    suspend fun getSpatialestId(address: String, x: Double, y: Double): SpatialestResolution? = withContext(Dispatchers.IO) {
        val urlMain = "https://property.spatialest.com/nc/mecklenburg/"
        val requestMain = Request.Builder()
            .url(urlMain)
            .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        
        try {
            client.newCall(requestMain).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: return@withContext null
                
                // Extract CSRF Token
                var csrfToken = ""
                val regexToken = Regex("\"csrfToken\":\"([^\"]+)\"")
                val match = regexToken.find(html)
                if (match != null) {
                    csrfToken = match.groupValues[1]
                } else {
                    val regexMeta = Regex("name=\"csrf-token\"\\s+content=\"([^\"]+)\"")
                    val matchMeta = regexMeta.find(html)
                    if (matchMeta != null) {
                        csrfToken = matchMeta.groupValues[1]
                    }
                }
                
                if (csrfToken.isEmpty()) {
                    println("Spatialest: CSRF token not found in main page HTML")
                    return@withContext null
                }
                
                // Clean address
                var cleanStreet = address
                val parts = address.split(",")
                if (parts.isNotEmpty()) {
                    cleanStreet = parts[0].trim()
                }
                // Strip zip (anchored to the end of the string to preserve 5-digit house numbers)
                val zipRegex = Regex("\\b\\d{5}(-\\d{4})?\\s*$")
                cleanStreet = zipRegex.replace(cleanStreet, "").trim()
                
                var internalId: Int? = null
                
                // 1. Text search first via POST
                val urlSearchPost = "https://property.spatialest.com/nc/mecklenburg/api/v2/search"
                val bodyObj = JSONObject().apply {
                    put("filters", JSONObject().apply {
                        put("term", cleanStreet)
                    })
                    put("page", 1)
                    put("limit", 21)
                }
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = bodyObj.toString().toRequestBody(mediaType)
                
                val requestPost = Request.Builder()
                    .url(urlSearchPost)
                    .post(body)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://property.spatialest.com/nc/mecklenburg/")
                    .addHeader("X-CSRF-TOKEN", csrfToken)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build()
                
                try {
                    client.newCall(requestPost).execute().use { respPost ->
                        if (respPost.isSuccessful) {
                            val resStr = respPost.body?.string()
                            if (resStr != null) {
                                val jsonPost = JSONObject(resStr)
                                if (jsonPost.has("id")) {
                                    internalId = jsonPost.optInt("id")
                                } else if (jsonPost.has("results")) {
                                    val results = jsonPost.optJSONArray("results")
                                    if (results != null) {
                                        for (i in 0 until results.length()) {
                                            val res = results.getJSONObject(i)
                                            val display = res.optJSONArray("display")
                                            if (display != null) {
                                                for (j in 0 until display.length()) {
                                                    val item = display.getJSONObject(j)
                                                    if (item.optString("id") == "location_address") {
                                                        val cleanedVal = item.optString("value").uppercase().trim()
                                                        val cleanedTarget = cleanStreet.uppercase()
                                                        if (cleanedVal == cleanedTarget || cleanedVal.contains(cleanedTarget) || cleanedTarget.contains(cleanedVal)) {
                                                            internalId = res.optInt("ParcelIdentifier")
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                            if (internalId != null) break
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Spatialest POST search failed: ${e.message}")
                }
                
                // 2. Fallback to coordinate query
                if (internalId == null) {
                    println("Spatialest POST search did not find ID for '$cleanStreet', falling back to coordinates...")
                    val urlSearchString = "https://property.spatialest.com/nc/mecklenburg/api/v2/search?filters[lat]=$y&filters[lng]=$x"
                    val requestSearch = Request.Builder()
                        .url(urlSearchString)
                        .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .addHeader("Referer", "https://property.spatialest.com/nc/mecklenburg/")
                        .addHeader("X-CSRF-TOKEN", csrfToken)
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build()
                    
                    try {
                        client.newCall(requestSearch).execute().use { respSearch ->
                            if (respSearch.isSuccessful) {
                                val resStr = respSearch.body?.string()
                                if (resStr != null) {
                                    val jsonSearch = JSONObject(resStr)
                                    if (jsonSearch.has("id")) {
                                        internalId = jsonSearch.optInt("id")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Spatialest coordinate search failed: ${e.message}")
                    }
                }
                
                val resolvedId = internalId ?: return@withContext null
                return@withContext SpatialestResolution(resolvedId, csrfToken)
            }
        } catch (e: Exception) {
            println("Spatialest error in getSpatialestId: ${e.message}")
        }
        return@withContext null
    }

    suspend fun resolveSpatialestId(address: String): Int? = withContext(Dispatchers.IO) {
        val addrLower = address.lowercase()
        val normalizedAddress = if (
            addrLower.contains("charlotte") || addrLower.contains(", nc") ||
            addrLower.contains("matthews") || addrLower.contains("mint hill") ||
            addrLower.contains("huntersville") || addrLower.contains("cornelius") ||
            addrLower.contains("davidson") || addrLower.contains("pineville") ||
            addrLower.contains("mooresville") || addrLower.contains("concord")
        ) address else "$address, Charlotte, NC"
        
        var candidate = geocodeAddressCensus(normalizedAddress)
        if (candidate == null) {
            candidate = geocodeAddressGIS(normalizedAddress)
        }
        if (candidate == null) {
            candidate = geocodeAddressNominatim(normalizedAddress)
        }
        
        if (candidate == null) return@withContext null
        return@withContext getSpatialestId(candidate.address, candidate.location.x, candidate.location.y)?.id
    }

    private suspend fun fetchSpatialestData(address: String, x: Double, y: Double): PropertyAttributes? = withContext(Dispatchers.IO) {
        val resolved = getSpatialestId(address, x, y) ?: return@withContext null
        val resolvedId = resolved.id
        val csrfToken = resolved.csrfToken
        
        try {
            println("Spatialest using resolved ID: $resolvedId")
            
            // 3. Query record card
            val urlCardString = "https://property.spatialest.com/nc/mecklenburg/api/v1/recordcard/$resolvedId"
            val requestCard = Request.Builder()
                .url(urlCardString)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://property.spatialest.com/nc/mecklenburg/")
                    .addHeader("X-CSRF-TOKEN", csrfToken)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build()
                
                client.newCall(requestCard).execute().use { respCard ->
                    if (!respCard.isSuccessful) return@withContext null
                    val cardStr = respCard.body?.string() ?: return@withContext null
                    val jsonCard = JSONObject(cardStr)
                    val parcel = jsonCard.optJSONObject("parcel") ?: return@withContext null
                    
                    val header = parcel.optJSONObject("header") ?: JSONObject()
                    val sections = parcel.optJSONArray("sections") ?: JSONArray()
                    
                    val prop = PropertyAttributes()
                    prop.pid = header.optString("ParcelID", "")
                    prop.ownerName = header.optString("owners", "None Found")
                    
                    val assessedValueStr = header.optString("PublicTotalMarketValue", "$0")
                    val numericVal = assessedValueStr.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    prop.assessedValue = numericVal
                    prop.taxAmount = numericVal * 0.01 // 1% estimation
                    
                    if (sections.length() > 0) {
                        val secDict = sections.optJSONObject(0)
                        if (secDict != null) {
                            val sec1 = secDict.optJSONArray("1")
                            if (sec1 != null && sec1.length() > 0) {
                                val first1 = sec1.getJSONObject(0)
                                val salePriceStr = first1.optString("SalePrice", "$0")
                                prop.lastSalePrice = salePriceStr.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                                prop.lastSaleDate = first1.optString("SaleDate", "N/A")
                                prop.zoning = first1.optString("landuse_description", "N/A")
                                
                                val legal = first1.optString("LegalDescription", "N/A")
                                if (legal.startsWith("L")) {
                                    val partsLegal = legal.split(" ")
                                    if (partsLegal.size >= 2) {
                                        prop.deedBook = partsLegal[0].drop(1)
                                        if (partsLegal[1].startsWith("M")) {
                                            prop.deedPage = partsLegal[1].drop(1)
                                        }
                                    }
                                }
                            }
                            val sec2 = secDict.optJSONArray("2")
                            if (sec2 != null && sec2.length() > 0) {
                                val first2 = sec2.getJSONObject(0)
                                val landValStr = first2.optString("PublicTotalLandValue", "$0")
                                val bldgValStr = first2.optString("PublicTotalBuildingValue", "$0")
                                prop.landValue = landValStr.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                                prop.buildingValue = bldgValStr.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                            }
                        }
                    }
                    
                    if (sections.length() > 2) {
                        val bldgGroup = sections.optJSONArray(2)
                        if (bldgGroup != null && bldgGroup.length() > 0) {
                            val firstGroup = bldgGroup.optJSONArray(0)
                            if (firstGroup != null && firstGroup.length() > 0) {
                                val firstBldg = firstGroup.optJSONObject(0)
                                if (firstBldg != null) {
                                    prop.yearBuilt = firstBldg.optInt("yearbuilt", 0)
                                    if (prop.yearBuilt == 0) {
                                        val ybStr = firstBldg.optString("yearbuilt", "")
                                        prop.yearBuilt = ybStr.toIntOrNull() ?: 0
                                    }
                                    
                                    prop.squareFeet = firstBldg.optInt("finishedarea", 0)
                                    if (prop.squareFeet == 0) {
                                        val sfStr = firstBldg.optString("finishedarea", "").replace(",", "")
                                        prop.squareFeet = sfStr.toIntOrNull() ?: 0
                                    }
                                    
                                    prop.bedrooms = firstBldg.optInt("BedRooms", 0)
                                    if (prop.bedrooms == 0) {
                                        val brStr = firstBldg.optString("BedRooms", "")
                                        prop.bedrooms = brStr.toIntOrNull()
                                    }
                                    
                                    var baths = 0.0
                                    val fb = firstBldg.optDouble("FullBath", 0.0)
                                    baths += fb
                                    
                                    val hb = firstBldg.optDouble("HalfBath", 0.0)
                                    baths += hb * 0.5
                                    
                                    if (baths > 0.0) {
                                        prop.bathrooms = baths
                                    }
                                }
                            }
                        }
                    }
                    
                    return@withContext prop
                }
            } catch (e: Exception) {
                println("Spatialest error: ${e.message}")
            }
            return@withContext null
        }       


    // Private Fetchers & Parsers

    private fun geocodeAddressCensus(street: String): Candidate? {
        try {
            val encodedStreet = URLEncoder.encode(street, "UTF-8")
            val url = "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress?address=$encodedStreet&benchmark=Public_AR_Current&format=json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val resultObj = json.optJSONObject("result") ?: return null
                val matches = resultObj.optJSONArray("addressMatches") ?: return null
                if (matches.length() > 0) {
                    val first = matches.getJSONObject(0)
                    val matchedAddress = first.optString("matchedAddress", street)
                    val coords = first.optJSONObject("coordinates") ?: return null
                    val location = Location(coords.optDouble("x"), coords.optDouble("y"))
                    return Candidate(matchedAddress, location, null)
                }
            }
        } catch (e: Exception) {
            println("Census Geocode Error: ${e.message}")
        }
        return null
    }

    private fun geocodeAddressGIS(street: String): Candidate? {
        val encodedStreet = URLEncoder.encode(street, "UTF-8")
        val url = "https://gis.charlottenc.gov/arcgis/rest/services/LOC/MasterAddress/GeocodeServer/findAddressCandidates?SingleLine=$encodedStreet&outSR=$spatialReference&f=json"
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val candidates = json.optJSONArray("candidates") ?: return null
                if (candidates.length() > 0) {
                    val first = candidates.getJSONObject(0)
                    val locObj = first.getJSONObject("location")
                    val location = Location(locObj.getDouble("x"), locObj.getDouble("y"))
                    val addressStr = first.optString("address")
                    return Candidate(addressStr, location, null)
                }
            }
        } catch (e: Exception) {
            println("GIS Geocode Error: ${e.message}")
        }
        return null
    }

    private fun geocodeAddressNominatim(street: String): Candidate? {
        val encodedStreet = URLEncoder.encode(street, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedStreet&format=json&limit=1&countrycodes=us"

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "QCAI-App/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val results = JSONArray(jsonStr)
                if (results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val lat = first.optString("lat").toDoubleOrNull() ?: return null
                    val lon = first.optString("lon").toDoubleOrNull() ?: return null
                    val displayName = first.optString("display_name", street)
                    val location = Location(lon, lat)
                    return Candidate(displayName, location, null)
                }
            }
        } catch (e: Exception) {
            println("Nominatim Geocode Error: ${e.message}")
        }
        return null
    }

    private fun fetchParcelInfo(x: Double, y: Double): JSONObject? {
        val url = "https://gis.charlottenc.gov/arcgis/rest/services/CountyData/Parcels/MapServer/0/query?geometry=$x,$y&geometryType=esriGeometryPoint&inSR=$spatialReference&spatialRel=esriSpatialRelIntersects&outFields=*&f=json"
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    return features.getJSONObject(0).optJSONObject("attributes")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchSchoolZone(x: Double, y: Double): Map<String, Any>? {
        val url = "https://gis.charlottenc.gov/arcgis/rest/services/CountyData/SchoolAttendance/FeatureServer/0/query?geometry=$x,$y&geometryType=esriGeometryPoint&inSR=$spatialReference&spatialRel=esriSpatialRelIntersects&outFields=*&f=json"
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    val attributes = features.getJSONObject(0).optJSONObject("attributes") ?: return null
                    val map = mutableMapOf<String, Any>()
                    val keys = attributes.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = attributes.get(key)
                    }
                    return map
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parsePropertyAttributes(attrs: JSONObject): PropertyAttributes {
        val prop = PropertyAttributes()
        val keys = attrs.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val kl = key.lowercase()
            val valObj = attrs.opt(key)
            if (valObj == null || valObj == JSONObject.NULL) continue
            
            if (kl == "pid" || kl == "pin" || kl == "parcel_id" || kl == "parcelid" || kl == "nc_pin") {
                prop.pid = valObj.toString()
            }
            else if (kl == "owner_name" || kl == "own_name" || kl == "legal_owner" || kl == "owner") {
                prop.ownerName = valObj.toString()
            }
            else if (kl.contains("sale_price") || kl.contains("saleprice") || kl.contains("sale_val") || (kl.contains("price") && !kl.contains("unit"))) {
                prop.lastSalePrice = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("sale_date") || kl.contains("saledate") || kl == "date" || kl.contains("record_date")) {
                if (valObj is Number) {
                    val date = java.util.Date(valObj.toLong())
                    val formatter = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                    prop.lastSaleDate = formatter.format(date)
                } else {
                    prop.lastSaleDate = valObj.toString()
                }
            }
            else if (kl.contains("deed_book") || kl.contains("deedbook") || kl == "book" || kl.contains("book_num")) {
                prop.deedBook = valObj.toString()
            }
            else if (kl.contains("deed_page") || kl.contains("deedpage") || kl == "page" || kl.contains("page_num")) {
                prop.deedPage = valObj.toString()
            }
            else if (kl.contains("assessed_value") || kl.contains("assessed_val") || kl == "total_value" || kl == "total_val" || (kl.contains("value") && !kl.contains("land") && !kl.contains("bldg") && !kl.contains("improvement") && !kl.contains("building"))) {
                prop.assessedValue = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("land_value") || kl.contains("land_val") || kl.contains("landvalue")) {
                prop.landValue = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("building_value") || kl.contains("building_val") || kl.contains("buildingvalue") || kl.contains("bldg_val") || kl.contains("improvement")) {
                prop.buildingValue = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("tax_amount") || kl.contains("tax_amt") || kl.contains("tax_val") || kl == "tax" || kl.contains("tax_levy")) {
                prop.taxAmount = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("year_built") || kl.contains("yearbuilt") || kl.contains("built_year") || kl == "yr_built" || kl == "year") {
                prop.yearBuilt = when (valObj) {
                    is Number -> valObj.toInt()
                    else -> valObj.toString().toIntOrNull() ?: 0
                }
            }
            else if (kl.contains("square_feet") || kl.contains("sq_feet") || kl == "sqft" || kl.contains("heated_area") || kl == "area" || kl.contains("sq_ft")) {
                prop.squareFeet = when (valObj) {
                    is Number -> valObj.toInt()
                    else -> valObj.toString().toIntOrNull() ?: 0
                }
            }
            else if (kl.contains("bedrooms") || kl.contains("bedroom") || kl == "beds" || kl == "bed") {
                prop.bedrooms = when (valObj) {
                    is Number -> valObj.toInt()
                    else -> valObj.toString().toIntOrNull()
                }
            }
            else if (kl.contains("bathrooms") || kl.contains("bathroom") || kl == "baths" || kl == "bath") {
                prop.bathrooms = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull()
                }
            }
            else if (kl == "zoning" || kl == "zone" || kl.contains("zoning_class")) {
                prop.zoning = valObj.toString()
            }
        }
        return prop
    }

    private fun identifyCounty(address: String): String {
        val addr = address.lowercase()
        if (addr.contains("mecklenburg")) {
            return "Mecklenburg"
        } else if (addr.contains("union")) {
            return "Union"
        } else if (addr.contains("cabarrus")) {
            return "Cabarrus"
        }
        
        val unionTowns = listOf("monroe", "waxhaw", "indian trail", "stallings", "marvin", "weddington", "wesley chapel", "wingate", "marshville", "unionville", "fairview", "lake park")
        for (town in unionTowns) {
            if (addr.contains(town)) {
                return "Union"
            }
        }
        
        val cabarrusTowns = listOf("concord", "kannapolis", "harrisburg", "mount pleasant", "midland")
        for (town in cabarrusTowns) {
            if (addr.contains(town)) {
                return "Cabarrus"
            }
        }
        
        return "Mecklenburg"
    }

    private fun fetchUnionParcelInfo(x: Double, y: Double): JSONObject? {
        try {
            val geomObj = JSONObject().apply {
                put("xmin", x - 0.0001)
                put("ymin", y - 0.0001)
                put("xmax", x + 0.0001)
                put("ymax", y + 0.0001)
                put("spatialReference", JSONObject().apply { put("wkid", 4326) })
            }
            val geomString = URLEncoder.encode(geomObj.toString(), "UTF-8")
            val url = "https://atlas.unioncountync.gov/server/rest/services/Property_Tax_Live/Parcels/MapServer/0/query?geometry=$geomString&geometryType=esriGeometryEnvelope&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=*&returnGeometry=false&f=json"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    return features.getJSONObject(0).optJSONObject("attributes")
                }
            }
        } catch (e: Exception) {
            println("Union Parcel Query Error: ${e.message}")
        }
        return null
    }

    private fun fetchUnionAssessedValue(parcelNumber: String): JSONObject? {
        try {
            val whereClause = URLEncoder.encode("parcel_number = '$parcelNumber'", "UTF-8")
            val url = "https://atlas.unioncountync.gov/server/rest/services/Property_Tax_Live/Assessed_Value/MapServer/0/query?where=$whereClause&outFields=*&returnGeometry=false&f=json"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    return features.getJSONObject(0).optJSONObject("attributes")
                }
            }
        } catch (e: Exception) {
            println("Union Assessed Value Query Error: ${e.message}")
        }
        return null
    }

    private fun fetchCabarrusParcelInfo(x: Double, y: Double): JSONObject? {
        try {
            val geomObj = JSONObject().apply {
                put("xmin", x - 0.0001)
                put("ymin", y - 0.0001)
                put("xmax", x + 0.0001)
                put("ymax", y + 0.0001)
                put("spatialReference", JSONObject().apply { put("wkid", 4326) })
            }
            val geomString = URLEncoder.encode(geomObj.toString(), "UTF-8")
            val url = "https://location.cabarruscounty.us/arcgishost/rest/services/ParcelsDash/MapServer/4/query?geometry=$geomString&geometryType=esriGeometryEnvelope&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=*&returnGeometry=false&f=json"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    return features.getJSONObject(0).optJSONObject("attributes")
                }
            }
        } catch (e: Exception) {
            println("Cabarrus Parcel Query Error: ${e.message}")
        }
        return null
    }

    private fun fetchCabarrusTaxInfo(objectId: Int): JSONObject? {
        try {
            val url = "https://location.cabarruscounty.us/arcgishost/rest/services/TaxParcelsDash/TaxParcelsDash/MapServer/0/query?where=OBJECTID%20%3D%20$objectId&outFields=*&returnGeometry=false&f=json"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    return features.getJSONObject(0).optJSONObject("attributes")
                }
            }
        } catch (e: Exception) {
            println("Cabarrus Tax Query Error: ${e.message}")
        }
        return null
    }

    private suspend fun searchSpatialestByOwner(county: String, first: String, last: String): List<OwnerSearchResult> {
        val cleanFirst = first.uppercase()
        val cleanLast = last.uppercase()
        
        // 1. Try search with full term "last, first"
        val term = if (cleanFirst.isEmpty()) cleanLast else "$cleanLast, $cleanFirst"
        var results = executeSpatialestQuery(county, term, 25)
        
        // 2. Fallback: if no results and both names are present, query by last name and filter locally
        if (results.isEmpty() && cleanFirst.isNotEmpty() && cleanLast.isNotEmpty()) {
            val fallbackResults = executeSpatialestQuery(county, cleanLast, 100)
            results = fallbackResults.filter { item ->
                item.owner.uppercase().contains(cleanFirst)
            }
        }
        
        return results
    }

    private suspend fun executeSpatialestQuery(county: String, term: String, limit: Int): List<OwnerSearchResult> = withContext(Dispatchers.IO) {
        val urlMain = "https://property.spatialest.com/nc/$county/"
        val requestMain = Request.Builder()
            .url(urlMain)
            .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        
        try {
            client.newCall(requestMain).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList<OwnerSearchResult>()
                val html = response.body?.string() ?: return@withContext emptyList<OwnerSearchResult>()
                
                // Extract CSRF Token
                var csrfToken = ""
                val regexToken = Regex("\"csrfToken\":\"([^\"]+)\"")
                val match = regexToken.find(html)
                if (match != null) {
                    csrfToken = match.groupValues[1]
                } else {
                    val regexMeta = Regex("name=\"csrf-token\"\\s+content=\"([^\"]+)\"")
                    val matchMeta = regexMeta.find(html)
                    if (matchMeta != null) {
                        csrfToken = matchMeta.groupValues[1]
                    }
                }
                
                if (csrfToken.isEmpty()) return@withContext emptyList<OwnerSearchResult>()
                
                val urlSearchPost = "https://property.spatialest.com/nc/$county/api/v2/search"
                val bodyObj = JSONObject().apply {
                    put("filters", JSONObject().apply {
                        put("term", term)
                    })
                    put("page", 1)
                    put("limit", limit)
                }
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = bodyObj.toString().toRequestBody(mediaType)
                
                val requestPost = Request.Builder()
                    .url(urlSearchPost)
                    .post(body)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://property.spatialest.com/nc/$county/")
                    .addHeader("X-CSRF-TOKEN", csrfToken)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build()
                
                client.newCall(requestPost).execute().use { respPost ->
                    if (respPost.isSuccessful) {
                        val resStr = respPost.body?.string() ?: return@withContext emptyList<OwnerSearchResult>()
                        val jsonPost = JSONObject(resStr)
                        val results = jsonPost.optJSONArray("results") ?: return@withContext emptyList<OwnerSearchResult>()
                        
                        val list = mutableListOf<OwnerSearchResult>()
                        for (i in 0 until results.length()) {
                            val res = results.getJSONObject(i)
                            var address = ""
                            var owner = ""
                            val display = res.optJSONArray("display")
                            if (display != null) {
                                for (j in 0 until display.length()) {
                                    val item = display.getJSONObject(j)
                                    val id = item.optString("id")
                                    if (id == "location_address" || id == "PHYSSTRADD") {
                                        address = item.optString("value")
                                    } else if (id == "owner" || id == "owners" || id == "header_owners" || id == "ownernames") {
                                        owner = item.optString("value")
                                    }
                                }
                            }
                            
                            val pid = res.optString("ParcelIdentifier", "")
                            
                            if (owner.isEmpty()) {
                                val header = res.optJSONObject("header")
                                if (header != null) {
                                    owner = header.optString("owners", "")
                                }
                            }
                            
                            if (owner.isEmpty()) {
                                owner = "Found Registry Entry"
                            }
                            
                            list.add(OwnerSearchResult(owner, address, pid))
                        }
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            println("Spatialest query failed for $county: ${e.message}")
        }
        return@withContext emptyList<OwnerSearchResult>()
    }

    private suspend fun searchMecklenburgByOwner(first: String, last: String): List<OwnerSearchResult> {
        return searchSpatialestByOwner("mecklenburg", first, last)
    }

    private suspend fun searchUnionByOwner(first: String, last: String): List<OwnerSearchResult> {
        return searchSpatialestByOwner("union", first, last)
    }

    private suspend fun searchCabarrusByOwner(first: String, last: String): List<OwnerSearchResult> = withContext(Dispatchers.IO) {
        var whereClause = "AcctName1 LIKE '%$last%'"
        if (first.isNotEmpty()) {
            whereClause += " AND (AcctName1 LIKE '%$first%' OR AcctName2 LIKE '%$first%')"
        }
        
        try {
            val encodedWhere = URLEncoder.encode(whereClause, "UTF-8")
            val url = "https://location.cabarruscounty.us/arcgishost/rest/services/TaxParcelsDash/TaxParcelsDash/MapServer/0/query?where=$encodedWhere&outFields=*&returnGeometry=false&f=json"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList<OwnerSearchResult>()
                val jsonStr = response.body?.string() ?: return@withContext emptyList<OwnerSearchResult>()
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return@withContext emptyList<OwnerSearchResult>()
                
                val list = mutableListOf<OwnerSearchResult>()
                val limit = minOf(features.length(), 25)
                for (i in 0 until limit) {
                    val attrs = features.getJSONObject(i).optJSONObject("attributes") ?: continue
                    val acct1 = attrs.optString("AcctName1", "")
                    val acct2 = attrs.optString("AcctName2", "")
                    val owner = if (acct2.isEmpty()) acct1 else "$acct1 & $acct2"
                    val nbh = attrs.optString("NBH_NAME", "")
                    val pid = attrs.optString("OBJECTID", "")
                    
                    list.add(OwnerSearchResult(owner, if (nbh.isEmpty()) "" else "Neighborhood: $nbh", pid))
                }
                return@withContext list
            }
        } catch (e: Exception) {
            println("Cabarrus owner search failed: ${e.message}")
        }
        return@withContext emptyList<OwnerSearchResult>()
    }

    suspend fun searchPropertiesByOwner(firstName: String, lastName: String): String = withContext(Dispatchers.IO) {
        val cleanFirst = firstName.trim().uppercase()
        val cleanLast = lastName.trim().uppercase()
        
        if (cleanFirst.isEmpty() && cleanLast.isEmpty()) {
            return@withContext "Please enter a first name or last name to search."
        }
        
        coroutineScope {
            val meckDeferred = async { searchMecklenburgByOwner(cleanFirst, cleanLast) }
            val unionDeferred = async { searchUnionByOwner(cleanFirst, cleanLast) }
            val cabarrusDeferred = async { searchCabarrusByOwner(cleanFirst, cleanLast) }
            
            val meckList = meckDeferred.await()
            val unionList = unionDeferred.await()
            val cabList = cabarrusDeferred.await()
            
            val sb = StringBuilder()
            sb.append("### 🔍 Owner Search Results\n")
            sb.append("Searched for: **$cleanFirst $cleanLast**\n\n")
            
            var foundAny = false
            
            if (meckList.isNotEmpty()) {
                foundAny = true
                sb.append("#### 📋 Mecklenburg County (${meckList.size} found)\n")
                for (item in meckList) {
                    sb.append("*   **Owner(s)**: ${item.owner}\n")
                    if (item.address.isNotEmpty()) {
                        sb.append("    *   Address: ${item.address}\n")
                    }
                    sb.append("    *   Parcel ID: [${item.pid}](https://property.spatialest.com/nc/mecklenburg/#/property/${item.pid}) (Polaris: [Link](https://polaris3g.mecklenburgcountync.gov/address/${item.pid}))\n")
                }
                sb.append("\n")
            }
            
            if (unionList.isNotEmpty()) {
                foundAny = true
                sb.append("#### 📋 Union County (${unionList.size} found)\n")
                for (item in unionList) {
                    sb.append("*   **Owner(s)**: ${item.owner}\n")
                    if (item.address.isNotEmpty()) {
                        sb.append("    *   Address: ${item.address}\n")
                    }
                    sb.append("    *   Parcel ID: [${item.pid}](https://property.spatialest.com/nc/union/#/property/${item.pid})\n")
                }
                sb.append("\n")
            } else {
                sb.append("#### 📋 Union County\n")
                sb.append("*   No properties found in Union County Spatialest registry. (Or search manually on the [Union County DevNet Tax Portal](https://unionnc-tax.devnetwedge.com/))\n\n")
            }
            
            if (cabList.isNotEmpty()) {
                foundAny = true
                sb.append("#### 📋 Cabarrus County (${cabList.size} found)\n")
                for (item in cabList) {
                    sb.append("*   **Owner(s)**: ${item.owner}\n")
                    if (item.address.isNotEmpty()) {
                        sb.append("    *   Address: ${item.address}\n")
                    }
                    sb.append("    *   Parcel ID: [${item.pid}](https://tax.cabarruscounty.us/)\n")
                }
                sb.append("\n")
            }
            
            if (!foundAny) {
                return@coroutineScope "### 🔍 Owner Search Results\nNo properties found for **$cleanFirst $cleanLast** in Mecklenburg, Union, or Cabarrus counties.\n\n*Note: You can try searching manually on the [Union County DevNet Tax Portal](https://unionnc-tax.devnetwedge.com/).*"
            }
            
            sb.toString()
        }
    }
}
